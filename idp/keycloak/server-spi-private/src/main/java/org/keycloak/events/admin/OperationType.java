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

package org.keycloak.events.admin;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public enum OperationType {

    CREATE,
    UPDATE,
    DELETE,
    SOFT_DELETE,
    HARD_DELETE_TRIGGER,
    HARD_DELETE,
    TRIGGER,
    TRIGGER_ENROLLMENT_MAIL,
    RESEND_INVITE,
    ACTION,
    CLEAR,
    FAILED,
    CREATE_FAILED,
    UPDATE_FAILED,
    DELETE_FAILED,
    READ_FAILED,
    READ,
    DOWNLOAD,
    TRIGGER_DOWNLOAD,
    DOWNLOAD_FAILED,
    ADD_ACCOUNT_CREATOR_TRIGGER,
    ADD_ACCOUNT_CREATOR,
    ADMIN_ROLE_ADDED,
    ADMIN_ROLE_REMOVED,
    NOTIFICATION_ERROR,
    NOTIFICATION,
    REVOKE_SOFT_DELETE,
    HOOK_FAILURE_NOTIFICATION,
    SOFT_DELETE_SUCCESS,
    CONFIG_CHECK_SUCCESS,
    CONFIG_CHECK_FAILED,
    APP_TRANSACTION_TEST_SUCCESS,
    APP_TRANSACTION_TEST_FAILED,
    APP_RESULT_TEST_SUCCESS,
    APP_RESULT_TEST_FAILED,
    WORKFLOW_TRIGGERED,
    WORKFLOW_ROLE,
    ROTATE,
    USER,
    DNS;
}
