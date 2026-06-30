/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.events.email;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.jboss.logging.Logger;
import org.keycloak.email.EmailTemplateProvider;
import org.keycloak.events.*;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RealmProvider;

import java.util.Map;
import java.util.Set;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class EmailEventListenerProvider implements EventListenerProvider {

    private static final Logger log = Logger.getLogger(EmailEventListenerProvider.class);

    private KeycloakSession session;
    private RealmProvider model;
    private EmailEventListenerProviderFactory factory;
    private EmailTemplateProvider emailTemplateProvider;
    private Set<EventType> includedEvents;
    private final EventListenerTransaction tx = new EventListenerTransaction(this::triggerAdminEvent, null);

    public EmailEventListenerProvider(KeycloakSession session, EmailTemplateProvider emailTemplateProvider, EmailEventListenerProviderFactory factory) {
        this.session = session;
        this.model = session.realms();
        this.emailTemplateProvider = emailTemplateProvider;
        this.factory = factory;
        this.session.getTransactionManager().enlistAfterCompletion(tx);
    }

    @Override
    public void onEvent(Event event, Map<String, String> config) {
        if (event.getType() == EventType.AZURE_SYNC_SUCCESS || 
                event.getType() == EventType.GOOGLE_WS_SYNC_SUCCESS ||
                event.getType() == EventType.DUO_WS_SYNC_SUCCESS || 
                event.getType() == EventType.OKTA_WS_SYNC_SUCCESS) {
            EmailProducer.sendSyncSuccessEMail(session, event, config);
        }

        if (event.getType() == EventType.CUSTOM_DOMAIN_SUCCESS || event.getType() == EventType.CUSTOM_DOMAIN_FAILURE) {
            EmailProducer.sendDNSProcessedEmail(session, event, config);
        }
    }

    private void triggerAdminEvent(AdminEvent event, Map<String, String> config, boolean includeRepresentation){
        switch (event.getOperationType()) {
            case CREATE:
            case RESEND_INVITE:
                switch (event.getResourceType()) {
                    case ADMIN_USER:
                        EmailProducer.sendAdminWelcomeEmail(session, event, config, includeRepresentation);
                        break;
                    case USER:
                        EmailProducer.sendUserWelcomeEmail(session, event, config, includeRepresentation);
                        break;
                    case GROUP_MEMBERSHIP:
                        EmailProducer.sendGroupAssigned(session, event, config, includeRepresentation);
                        break;
                    case WORKSPACE_ROLE_MAPPING:
                        EmailProducer.sendRoleAssigned(session, event, config, includeRepresentation);
                        break;
                    case REPORT:
                        EmailProducer.sendReportNotification(session,event,config,includeRepresentation);
                        break;
                }
                break;
            case ACTION:
                if (event.getResourceType() == ResourceType.RESET_PASSWORD ) {
                    EmailProducer.sendTempPassword(session, event, config, includeRepresentation, emailTemplateProvider);
                }
                break;
            case TRIGGER_ENROLLMENT_MAIL:
                if (event.getResourceType() == ResourceType.USER) {
                    EmailProducer.sendUserEnrollmentEmail(session, event, config, includeRepresentation);
                }
                break;
            case SOFT_DELETE:
                if (event.getResourceType() == ResourceType.WORKSPACE) {
                    EmailProducer.sendToManageAccountLambda(session, event, config, includeRepresentation, emailTemplateProvider);
                }
                break;
            case HARD_DELETE_TRIGGER:
                if (event.getResourceType() == ResourceType.WORKSPACE){
                    try{
                        Map<String, Object> rep = new ObjectMapper().readValue(event.getRepresentation(), Map.class);
                        if (rep == null) {
                            log.error("Representation is null");
                            break;
                        }
                        Object sendMail = rep.get("sendMail");
                        if (sendMail != null && sendMail.equals("true")) break;
                        EmailProducer.sendHardDeleteOtpEmail(session, event, config, includeRepresentation, emailTemplateProvider);
                    }catch (Exception e){
                        log.error(e);
                    }

                }
                break;
            case HARD_DELETE:
                if (event.getResourceType() == ResourceType.WORKSPACE) {
                    EmailProducer.sendHardDeleteSuccessEmail(session, event, config, includeRepresentation, emailTemplateProvider);
                }
                break;
            case ADD_ACCOUNT_CREATOR_TRIGGER:
                if(event.getResourceType() == ResourceType.USER){
                    EmailProducer.sendAccountCreatorOtpEmail(session,event,emailTemplateProvider);
                }
                break;
            case ADD_ACCOUNT_CREATOR:
                if(event.getResourceType() == ResourceType.USER){
                    EmailProducer.sendAccountCreatorSuccessEmail(session,event);
                }
                break;
            case TRIGGER:
                if (event.getResourceType() == ResourceType.SEND_GROUP_INVITE) {
                    EmailProducer.sendGroupInvites(session, event, config, includeRepresentation);
                }
                break;
            case DELETE:
                if (event.getResourceType() == ResourceType.GROUP_MEMBERSHIP) {
                    EmailProducer.sendGroupRemoved(session, event, config, includeRepresentation);
                } else if (event.getResourceType() == ResourceType.WORKSPACE_ROLE_MAPPING) {
                    EmailProducer.sendRoleRemoved(session, event, config, includeRepresentation);
                }
                break;
            case ADMIN_ROLE_REMOVED:
                if(event.getResourceType() == ResourceType.WORKSPACE_ROLE){
                    EmailProducer.sendAdminRoleRemoved(session,event,config,includeRepresentation);
                }
                break;
            case ADMIN_ROLE_ADDED:
                if(event.getResourceType() == ResourceType.WORKSPACE_ROLE){
                    EmailProducer.sendAdminRoleAdded(session,event,config,includeRepresentation);
                }
                break;
            case HOOK_FAILURE_NOTIFICATION:
                EmailProducer.sendHookFailedEmail(session,event);
                break;
        }

    }

    @Override
    public void onEvent(AdminEvent event, Map<String, String> config, boolean includeRepresentation) {
        switch (event.getOperationType()) {
            case CREATE:
            case RESEND_INVITE:
                if (event.getResourceType() == ResourceType.ADMIN_USER || event.getResourceType() == ResourceType.USER) {
                    tx.addAdminEvent(event, config, includeRepresentation);
                } else if (event.getResourceType() == ResourceType.GROUP_MEMBERSHIP) {
                    tx.addAdminEvent(event, config, includeRepresentation);
                } else if (event.getResourceType() == ResourceType.WORKSPACE_ROLE_MAPPING) {
                    tx.addAdminEvent(event, config, includeRepresentation);
                }else if(event.getResourceType() == ResourceType.REPORT){
                    //tx.addAdminEvent(event, config, includeRepresentation);
                    //TODO: Need to check why transaction is not working
                    EmailProducer.sendReportNotification(session,event,config,includeRepresentation);
                }
                break;
            case ACTION:
                if (event.getResourceType() == ResourceType.RESET_PASSWORD) {
                    tx.addAdminEvent(event, config, includeRepresentation);
                }
                break;
            case TRIGGER_ENROLLMENT_MAIL:
            case ADD_ACCOUNT_CREATOR_TRIGGER:
            case ADD_ACCOUNT_CREATOR:
                if (event.getResourceType() == ResourceType.USER) {
                    tx.addAdminEvent(event, config, includeRepresentation);
                }
                break;
            case SOFT_DELETE:
                RealmModel realm = session.realms().getRealm(event.getRealmId());
                if (event.getResourceType() == ResourceType.WORKSPACE && realm.getName().equals(org.keycloak.Config.getAdminRealm())) {
                    tx.addAdminEvent(event, config, includeRepresentation);
                }
                break;
            case HARD_DELETE_TRIGGER:
            case HARD_DELETE:
                if (event.getResourceType() == ResourceType.WORKSPACE) {
                    tx.addAdminEvent(event, config, includeRepresentation);
                }
                break;
            case TRIGGER:
                if (event.getResourceType() == ResourceType.SEND_GROUP_INVITE) {
                    tx.addAdminEvent(event, config, includeRepresentation);
                }
                break;
            case DELETE:
                if (event.getResourceType() == ResourceType.GROUP_MEMBERSHIP || event.getResourceType() == ResourceType.WORKSPACE_ROLE_MAPPING) {
                    tx.addAdminEvent(event, config, includeRepresentation);
                }
                break;
            case ADMIN_ROLE_REMOVED:
            case ADMIN_ROLE_ADDED:
                if(event.getResourceType() == ResourceType.WORKSPACE_ROLE){
                    tx.addAdminEvent(event, config, includeRepresentation);
                }
                break;
            case NOTIFICATION_ERROR:
                if(event.getResourceType() == ResourceType.NOTIFICATION){
                    tx.addAdminEvent(event, config, includeRepresentation);
                }
                break;
            case HOOK_FAILURE_NOTIFICATION:
                tx.addAdminEvent(event, config, includeRepresentation);
        }
    }

    @Override
    public EventListenerProviderFactory getFactory() {
        return factory;
    }

    @Override
    public void close() {
    }

}
