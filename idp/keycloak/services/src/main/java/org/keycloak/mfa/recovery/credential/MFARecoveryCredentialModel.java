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

package org.keycloak.mfa.recovery.credential;

import org.keycloak.common.util.Time;
import org.keycloak.credential.CredentialModel;

import java.util.Arrays;
import java.util.List;

/**
 * @author <a href="mailto:alistair.doswald@elca.ch">Alistair Doswald</a>
 * @version $Revision: 1 $
 */
public class MFARecoveryCredentialModel extends CredentialModel {
    public static final String TYPE = "MFA_RECOVERY";

    private final String secretData;


    private MFARecoveryCredentialModel(List<String> code) {
        secretData = String.join("##",code);
    }

    public static MFARecoveryCredentialModel createSecretQuestion(List<String> code) {
        MFARecoveryCredentialModel credentialModel = new MFARecoveryCredentialModel(code);
        credentialModel.fillCredentialModelFields();
        return credentialModel;
    }

    public static MFARecoveryCredentialModel createFromCredentialModel(CredentialModel credentialModel) {
        MFARecoveryCredentialModel secretQuestionCredentialModel = new MFARecoveryCredentialModel(Arrays.asList(credentialModel.getSecretData().split("##")));
        secretQuestionCredentialModel.setUserLabel(credentialModel.getUserLabel());
        secretQuestionCredentialModel.setCreatedDate(credentialModel.getCreatedDate());
        secretQuestionCredentialModel.setType(TYPE);
        secretQuestionCredentialModel.setId(credentialModel.getId());
        secretQuestionCredentialModel.setSecretData(credentialModel.getSecretData());
        secretQuestionCredentialModel.setCredentialData(credentialModel.getCredentialData());
        return secretQuestionCredentialModel;
    }

    public String getMFARecoveryData() {
        return secretData;
    }

    private void fillCredentialModelFields() {
        setSecretData(secretData);
        setType(TYPE);
        setCreatedDate(Time.currentTimeMillis());
    }

}
