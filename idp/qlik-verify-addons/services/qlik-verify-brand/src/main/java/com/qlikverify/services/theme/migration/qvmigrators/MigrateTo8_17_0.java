package com.qlikverify.services.theme.migration.qvmigrators;

import org.keycloak.EnvironmentReader;
import org.keycloak.migration.ModelVersion;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class MigrateTo8_17_0 implements QVMigration{
    public static final ModelVersion VERSION = new ModelVersion("8.17.0");
    @Override
    public void migrate(KeycloakSession session) {
        System.out.println("----------MIRGRATION FOR:" + VERSION.toString() + " STARTED-------------");
        session.realms().getRealmsStream()
                .filter(realm -> realm.getMode().equals("bo"))
                .forEach(x -> {
                    System.out.println("-------------------------------");
                    System.out.println("MIGRATION STARTED FOR :" + x.getName());
                    addSecurityHeader(session, x);
                    System.out.println("MIGRATION COMPLETED FOR :" + x.getName());
                    System.out.println("-------------------------------");
                });
        System.out.println("----------MIRGRATION FOR:" + VERSION.toString() + " COMPLETED-------------");
    }

    @Override
    public ModelVersion getVersion() {
        return VERSION;
    }

    @Override
    public void lazyUserMigration(KeycloakSession session, RealmModel realmModel, UserModel userModel) {
        // NOP
    }

    public void addSecurityHeader(KeycloakSession session, RealmModel realm) {
        String videoKycSecurityHeader = EnvironmentReader.getValue("EZTO_VERIFY_VIDEO_SDK_URL", "https://video-kyc.eztovrfy.com");
        Map<String, String> browserHeaders = realm.getBrowserSecurityHeaders();
        Map<String, String> headers = new HashMap<>();
        headers.putAll(browserHeaders);
        String csp = headers.get("contentSecurityPolicy");

        List<String> cspSplit = Arrays.stream(csp.split(";")).collect(Collectors.toList());
        for (int x = 0; x < cspSplit.size(); x++) {
            if (cspSplit.get(x).trim().startsWith("frame-src")) {
                String frame = cspSplit.get(x);
                if (!frame.contains(videoKycSecurityHeader)) {
                    frame += " " + videoKycSecurityHeader;
                }
                cspSplit.set(x, frame);
            }
        }
        csp = String.join(";", cspSplit);
        headers.put("contentSecurityPolicy", csp);
        realm.setBrowserSecurityHeaders(headers);
    }
}
