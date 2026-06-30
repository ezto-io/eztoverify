package com.qlikverify.services.eventlisteners.reports;

import com.opencsv.CSVWriter;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.jboss.logging.Logger;
import org.keycloak.EnvironmentReader;
import org.keycloak.avatar.AvatarProvider;
import org.keycloak.common.util.Resteasy;
import org.keycloak.events.*;
import org.keycloak.events.admin.*;
import org.keycloak.models.*;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.services.resources.admin.AdminEventBuilder;
import org.keycloak.util.JsonSerialization;
import org.keycloak.utils.GeneralHelper;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import static com.qlikverify.services.constants.Constants.REPORT_TYPE;

public class ReportProducer {
    private static Executor executor = Executors.newCachedThreadPool();

    public static void triggerReportCreation(KeycloakSession session, AdminEvent event, Map<String, String> config, boolean includeRepresentation) {

        CompletableFuture.runAsync(new Runnable() {
            @Override
            public void run() {
                KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), new KeycloakSessionTask() {
                    @Override
                    public void run(KeycloakSession innerSession) {
                        Path fileName = null;
                        RealmModel realm = null;
                        try {
                            Resteasy.pushContext(KeycloakSession.class, session);
                            realm = innerSession.realms().getRealm(event.getAuthDetails().getRealm().getId());
                            innerSession.getContext().setRealm(realm, session);
                            session.getContext().setRealm(realm, session);
                            Map<String, Object> reportRep = null;
                            try {
                                reportRep = JsonSerialization.readValue(event.getRepresentation(), Map.class);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            if (reportRep == null)
                                return;
                            EventStoreProvider eventStore = session.getProvider(EventStoreProvider.class);

                            if (event.getDetails().containsKey(REPORT_TYPE)) {
                                String reportType = event.getDetails().get(REPORT_TYPE);
                                if (reportType.equals("api_log") || reportType.equals("pricing_log")) {
                                    fileName = Files.createTempFile(reportType.equals("api_log") ? "api_logs_" : "pricing_logs_", ".csv");
                                    createAPILogFile(realm, reportRep, fileName);
                                } else if (reportType.equals("log")) {
                                    fileName = Files.createTempFile("audit_logs_", ".csv");
                                    createLogFile(realm, reportRep, eventStore, fileName, session);
                                } else {
                                    fileName = Files.createTempFile("audit_admin_logs_", ".csv");
                                    createAdminLogFile(realm, reportRep, eventStore, fileName);
                                }
                            }

                            String finalFileName = fileName.toFile().getName();
                            AvatarProvider avatarProvider = session.getProvider(AvatarProvider.class);
                            avatarProvider.saveFile(realm.getName(), finalFileName, new FileInputStream(fileName.toFile()));
                            String reportId = event.getDetails().get("report");
                            ReportModel reportModel = realm.getReportById(reportId);
                            if(reportModel == null)
                            {
                                RealmModel finalRealm = realm;
                                ReportModel repModel = new ReportModel();
                                repModel.setRealmId(finalRealm.getId());
                                repModel.setFileName(finalFileName);
                                repModel.setStatus("report-success");
                                repModel.setReportType(event.getDetails().get(REPORT_TYPE));
                                repModel.setRequestedBy(event.getAuthDetails().getUser().getId());
                                realm.addReport(repModel);
                            }
                            else {
                                reportModel.setStatus("report-success");
                                reportModel.setFileName(finalFileName);
                                realm.updateReport(reportModel);
                            }
                            realm.removeAttribute("isReportProgress");
                            AdminEventBuilder eventBuilder = new AdminEventBuilder(realm, event.getAuthDetails(), session, session.getContext().getConnection());
                            eventBuilder.resource(ResourceType.REPORT).operation(OperationType.CREATE).realm(realm.getId()).detail("query", JsonSerialization.writeValueAsString(reportRep)).detail("filename",finalFileName).detail("downloadUrl","/auth/admin/"+realm.getName()+"/console/#/realms/"+realm.getName()+"/reports").success();

                        } catch (Exception err) {
                            err.printStackTrace();

                        }
                        finally {
                            if(fileName != null)
                            {
                                try {
                                    Files.delete(fileName);
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }

                            if(realm != null)
                            {
                                realm.removeAttribute("isReportProgress");
                            }
                        }

                    }
                });
            }
        },executor);

    }

    private static void createLogFile(RealmModel realm, Map<String, Object> reportRep,EventStoreProvider eventStore, Path fileName, KeycloakSession session){
        try {
            EventQuery query = eventStore.createQuery().realm(realm.getId());
            if (reportRep.containsKey("client")) {
                query.client((String) reportRep.get("client"));
            }

            if (reportRep.containsKey("type")) {
                List<String> types = (List<String>) reportRep.get("type");
                EventType[] t = new EventType[types.size()];
                for (int i = 0; i < t.length; i++) {
                    t[i] = EventType.valueOf(types.get(i));
                }
                query.type(t);
            }

            if (reportRep.containsKey("user")) {
                query.user((String) reportRep.get("user"));
            }

            LocalDate today = LocalDate.now();
            Instant endOfDay = today.atStartOfDay(ZoneId.of("UTC")).plusDays(1).minusSeconds(1).toInstant();
            query.toDate(Date.from(endOfDay));

            String days = (String) reportRep.get("last");

            query.fromDate(GeneralHelper.getReverseDate(days != null ? days : "7d"));

            if (reportRep.containsKey("ipAddress")) {
                query.ipAddress((String) reportRep.get("ipAddress"));
            }
            if (reportRep.containsKey("first")) {
                query.firstResult((Integer) reportRep.get("first"));
            }
            if (reportRep.containsKey("max")) {
                query.maxResults((Integer) reportRep.get("max"));
            } else {
                query.maxResults(Constants.DEFAULT_MAX_RESULTS);
            }

            EventCollection eventCollection = query.getResult();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            //Here we convert to csv and save to minio


            CSVWriter csvWriter = new CSVWriter(new FileWriter(fileName.toFile()));
            String[] headers = {"Date", "EventType", "User", "Application", "IP", "Error", "Details"};
            csvWriter.writeNext(headers);
            for (Event eve :
                    eventCollection.getEvents()) {
                if (eve != null) {
                    String[] eveData = new String[]{
                            LocalDateTime.ofInstant(Instant.ofEpochMilli(eve.getTime()), ZoneId.of("UTC")).format(formatter),
                            eve.getType().name(),
                            eve.getUser() != null && eve.getUser().getEmail() != null ? eve.getUser().getEmail() : (eve.getUser() == null ? "No User" : (eve.getUser().getUsername()!= null ? eve.getUser().getUsername() : eve.getUser().getId())),
                            eve.getClient() != null && eve.getClient().getName() != null ? eve.getClient().getName() : (eve.getClient() == null ? "No Application Details" : eve.getClient().getClientId()),
                            eve.getIpAddress(),
                            eve.getError(),
                            JsonSerialization.writeValueAsString(eve.getDetails())
                    };
                    csvWriter.writeNext(eveData);
                }
            }
            csvWriter.close();
        }
        catch (Exception err)
        {
            err.printStackTrace();
        }
    }

    private static void createAdminLogFile(RealmModel realm, Map<String, Object> reportRep,EventStoreProvider eventStore, Path fileName){
        try {
            AdminEventQuery query = eventStore.createAdminQuery().realm(realm.getId());
            if (reportRep.containsKey("authRealm")) {
                query.authRealm((String)reportRep.get("authRealm"));
            }

            if (reportRep.containsKey("authClient")) {
                query.authClient((String)reportRep.get("authClient"));
            }

            if (reportRep.containsKey("authUser")) {
                query.authUser((String)reportRep.get("authUser"));
            }

            if (reportRep.containsKey("authIpAddress")) {
                query.authIpAddress((String)reportRep.get("authIpAddress"));
            }

            if (reportRep.containsKey("resourcePath")){
                query.resourcePath((String)reportRep.get("resourcePath"));

            }

            if (reportRep.containsKey("operationTypes")) {
                List<String> operationTypes = (List<String>) reportRep.get("operationTypes");
                OperationType[] t = new OperationType[operationTypes.size()];
                for (int i = 0; i < t.length; i++) {
                    t[i] = OperationType.valueOf(operationTypes.get(i));
                }
                query.operation(t);
            }

            if (reportRep.containsKey("resourceTypes")) {
                List<String> resourceTypes = (List<String>) reportRep.get("resourceTypes");
                ResourceType[] t = new ResourceType[resourceTypes.size()];
                for (int i = 0; i < t.length; i++) {
                    t[i] = ResourceType.valueOf(resourceTypes.get(i));
                }
                query.resourceType(t);
            }

            LocalDate today = LocalDate.now();
            Instant endOfDay = today.atStartOfDay(ZoneId.of("UTC")).plusDays(1).minusSeconds(1).toInstant();
            query.toTime(Date.from(endOfDay));

            String days = (String) reportRep.get("last");

            query.fromTime(GeneralHelper.getReverseDate(days != null ? days : "7d"));

            if (reportRep.containsKey("first")) {
                query.firstResult((Integer) reportRep.get("first"));
            }
            if (reportRep.containsKey("max")) {
                query.maxResults((Integer) reportRep.get("max"));
            } else {
                query.maxResults(Constants.DEFAULT_MAX_RESULTS);
            }

            AdminEventCollection eventCollection = query.getResult();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            //Here we convert to csv and save to minio

            CSVWriter csvWriter = new CSVWriter(new FileWriter(fileName.toFile()));
            String[] headers = {"Date", "AdminEventType","Action", "User",  "IP","Details"};
            csvWriter.writeNext(headers);
            for (AdminEvent eve :
                    eventCollection.getEvents()) {
                if (eve != null) {
                    String[] eveData = new String[]{
                            LocalDateTime.ofInstant(Instant.ofEpochMilli(eve.getTime()), ZoneId.of("UTC")).format(formatter),
                            eve.getResourceType().name(),
                            eve.getOperationType().name(),
                            eve.getAuthDetails() != null && eve.getAuthDetails().getUser() != null && eve.getAuthDetails().getUser().getEmail() != null ? eve.getAuthDetails().getUser().getEmail() : (eve.getAuthDetails() == null ? "No User" : (eve.getAuthDetails().getUser() == null ? "No User" : eve.getAuthDetails().getUser().getId())),
                            eve.getAuthDetails() != null && eve.getAuthDetails().getIpAddress() != null ?  eve.getAuthDetails().getIpAddress() : "Unknown Address",
                            JsonSerialization.writeValueAsString(eve.getRepresentation())
                    };
                    csvWriter.writeNext(eveData);
                }
            }
            csvWriter.close();
        }
        catch (Exception err)
        {
            err.printStackTrace();
        }
    }

    private static void createAPILogFile(RealmModel realm, Map<String, Object> reportRep, Path fileName) {

        HttpUrl.Builder urlBuilder = HttpUrl.parse(
                EnvironmentReader.getValue("API_CONNECTOR_HOST_URL", "http://localhost:8081") +
                        "/api/audit-logs/get-verification-logs"
        ).newBuilder();

        urlBuilder.addQueryParameter("workspaceId", realm.getId())
                .addQueryParameter("page", String.valueOf(reportRep.get("page")))
                .addQueryParameter("size", String.valueOf(reportRep.get("size")))
                .addQueryParameter("isPricingLogs", String.valueOf(reportRep.get("isPricingLogs")))
                .addQueryParameter("filterByVerifierName", String.valueOf(reportRep.get("filterByVerifierName")))
                .addQueryParameter("filterByTimestamp", String.valueOf(reportRep.get("filterByTimestamp")));

        if (reportRep.containsKey("eventType") && reportRep.get("eventType") != null) {
            ((List<String>) reportRep.get("eventType"))
                    .forEach(eventType -> urlBuilder.addQueryParameter("eventTypes", eventType));
        }

        if (reportRep.containsKey("requestId")) {
            urlBuilder.addQueryParameter("requestId", String.valueOf(reportRep.get("requestId")));
        }

        Request request = new Request.Builder().url(urlBuilder.build()).get().build();
        OkHttpClient okHttpClient = new OkHttpClient();

        try (okhttp3.Response response = okHttpClient.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) return;

            String responseBody = response.body().string();
            Map<String, Object> apiLogs = JsonSerialization.readValue(responseBody, Map.class);
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

            boolean isPricingLogs = Boolean.parseBoolean(reportRep.get("isPricingLogs").toString());
            String[] headers = isPricingLogs
                    ? new String[]{"Timestamp", "RequestId", "Deducted For", "Price"}
                    : new String[]{"Timestamp", "RequestId", "EventType", "Name", "Error"};

            try (CSVWriter csvWriter = new CSVWriter(new FileWriter(fileName.toFile()))) {
                csvWriter.writeNext(headers);

                List<Map<String, Object>> content = (List<Map<String, Object>>) apiLogs.get("content");
                for (Map<String, Object> x : content) {
                    String date = LocalDateTime.ofInstant(
                            Instant.ofEpochMilli(Long.parseLong(x.get("timestamp").toString())),
                            ZoneId.of("UTC")
                    ).format(formatter);

                    String[] row = isPricingLogs
                            ? new String[]{
                            date,
                            Objects.toString(x.get("requestId"), ""),
                            Objects.toString(x.get("verifierName").toString().toUpperCase(), ""),
                            Objects.toString(x.get("verifierPrice"), "")
                    }
                            : new String[]{
                            date,
                            Objects.toString(x.get("requestId"), ""),
                            Objects.toString(x.get("resourceType"), ""),
                            Objects.toString(x.get("verifierName").toString().toUpperCase(), ""),
                            Objects.toString(x.get("error"), "")
                    };
                    csvWriter.writeNext(row);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
