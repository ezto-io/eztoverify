/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.models;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.keycloak.jose.jws.Algorithm;
import org.keycloak.utils.StringUtil;

public class CibaConfig extends AbstractConfig {

    // Constants
    public static final String CIBA_POLL_MODE = "poll";
    public static final String CIBA_PING_MODE = "ping";
    public static final String CIBA_PUSH_MODE = "push";
    public static final List<String> CIBA_SUPPORTED_MODES = Arrays.asList(CIBA_POLL_MODE, CIBA_PING_MODE,CIBA_PUSH_MODE);

    // realm attribute names
    public static final String CIBA_BACKCHANNEL_TOKEN_DELIVERY_MODE = "cibaBackchannelTokenDeliveryMode";
    public static final String CIBA_EXPIRES_IN = "cibaExpiresIn";
    public static final String CIBA_INTERVAL = "cibaInterval";
    public static final String CIBA_AUTH_REQUESTED_USER_HINT = "cibaAuthRequestedUserHint";

    // default value
    public static final String DEFAULT_CIBA_POLICY_TOKEN_DELIVERY_MODE = "hook-notifier";
    public static final int DEFAULT_CIBA_POLICY_EXPIRES_IN = 120;
    public static final int DEFAULT_CIBA_POLICY_INTERVAL = 5;
    public static final String DEFAULT_CIBA_POLICY_AUTH_REQUESTED_USER_HINT = "user";

    private String backchannelTokenDeliveryMode = DEFAULT_CIBA_POLICY_TOKEN_DELIVERY_MODE;
    private int expiresIn = DEFAULT_CIBA_POLICY_EXPIRES_IN;
    private int poolingInterval = DEFAULT_CIBA_POLICY_INTERVAL;
    private String authRequestedUserHint = DEFAULT_CIBA_POLICY_AUTH_REQUESTED_USER_HINT;

    // client attribute names
    public static final String OIDC_CIBA_GRANT_ENABLED = "oidc.ciba.grant.enabled";
    public static final String CIBA_BACKCHANNEL_TOKEN_DELIVERY_MODE_PER_CLIENT = "ciba.backchannel.token.delivery.mode";
    public static final String CIBA_BACKCHANNEL_DEAD_LETTER_HOOK_DELIVERY_MODE_PER_CLIENT = "ciba.backchannel.dead.letter.hook.delivery.mode";

    public static final String CIBA_BACKCHANNEL_CLIENT_NOTIFICATION_ENDPOINT = "ciba.backchannel.client.notification.endpoint";
    public static final String CIBA_BACKCHANNEL_AUTH_REQUEST_SIGNING_ALG = "ciba.backchannel.auth.request.signing.alg";

    public CibaConfig(RealmModel realm) {
        this.realm = () -> realm;

        setBackchannelTokenDeliveryMode(realm.getAttribute(CIBA_BACKCHANNEL_TOKEN_DELIVERY_MODE));

        String expiresIn = realm.getAttribute(CIBA_EXPIRES_IN);

        if (StringUtil.isNotBlank(expiresIn)) {
            setExpiresIn(Integer.parseInt(expiresIn));
        }

        String interval = realm.getAttribute(CIBA_INTERVAL);

        if (StringUtil.isNotBlank(interval)) {
            setPoolingInterval(Integer.parseInt(interval));
        }

        setAuthRequestedUserHint(realm.getAttribute(CIBA_AUTH_REQUESTED_USER_HINT));

        this.realmForWrite = () -> realm;
    }

    public String getBackchannelTokenDeliveryMode(ClientModel client) {
        String mode = client.getAttribute(CIBA_BACKCHANNEL_TOKEN_DELIVERY_MODE_PER_CLIENT);
        if (StringUtil.isBlank(mode)) {
            mode = getBackchannelTokenDeliveryMode();
        }
        return mode;
    }

    public String getBackchannelTokenDeliveryMode() {
        return backchannelTokenDeliveryMode;
    }

    public void setBackchannelTokenDeliveryMode(String mode) {
        if (StringUtil.isBlank(mode)) {
            mode = DEFAULT_CIBA_POLICY_TOKEN_DELIVERY_MODE;
        }
        this.backchannelTokenDeliveryMode = mode;
        persistRealmAttribute(CIBA_BACKCHANNEL_TOKEN_DELIVERY_MODE, mode);
    }

    public long getExpiresIn(ClientModel clientModel,RealmModel realmModel) {
        return getExpiryDurationInRawSec(getExpiresInConfig(clientModel,realmModel));
    }

    public String getExpiresInConfig(ClientModel clientModel,RealmModel realmModel)
    {
        CibaAuthConfigModel configModel = realmModel.getCibaAuthConfigByClientId(clientModel.getClientId(),realmModel.getId());
        String cltExpiresIn;
        if("api-authorizer".equalsIgnoreCase(clientModel.getAuthenticationFlowBindingOverride("ciba_auth")) &&
                (configModel == null || configModel.getConfig() == null || configModel.getConfig().get("verify-expiry-time") == null)){
            cltExpiresIn = "300s";
        } else {
            cltExpiresIn = configModel.getConfig().get("verify-expiry-time");
        }
        if(StringUtil.isBlank(cltExpiresIn))
            return expiresIn+"s";
        return cltExpiresIn;
    }

    public int getExpiresIn() {
        return expiresIn;
    }

    public void setExpiresIn(String expiresIn) {
        if (expiresIn == null) {
            setExpiresIn((Integer) null);
        } else {
            setExpiresIn(Integer.parseInt(expiresIn));
        }
    }

    public void setExpiresIn(Integer expiresIn) {
        if (expiresIn == null) {
            expiresIn = DEFAULT_CIBA_POLICY_EXPIRES_IN;
        }
        this.expiresIn = expiresIn;
        persistRealmAttribute(CIBA_EXPIRES_IN, expiresIn);
    }

    public int getPoolingInterval(ClientModel clientModel) {
        String cltInterval = clientModel.getAttribute(CIBA_INTERVAL);
        if(StringUtil.isBlank(cltInterval))
            return poolingInterval;

        return Integer.parseInt(cltInterval);
    }

    public int getPoolingInterval() {
        return poolingInterval;
    }

    public void setPoolingInterval(String poolingInterval) {
        if (poolingInterval == null) {
            setPoolingInterval((Integer) null);
        } else {
            setPoolingInterval(Integer.parseInt(poolingInterval));
        }
    }

    public void setPoolingInterval(Integer interval) {
        if (interval == null) {
            interval = DEFAULT_CIBA_POLICY_INTERVAL;
        }
        this.poolingInterval = interval;
        persistRealmAttribute(CIBA_INTERVAL, interval);
    }

    public String getAuthRequestedUserHint() {
        return authRequestedUserHint;
    }

    public void setAuthRequestedUserHint(String hint) {
        if (hint == null) {
            hint = DEFAULT_CIBA_POLICY_AUTH_REQUESTED_USER_HINT;
        }
        this.authRequestedUserHint = hint;
        persistRealmAttribute(CIBA_AUTH_REQUESTED_USER_HINT, hint);
    }

    public boolean isOIDCCIBAGrantEnabled(ClientModel client) {
        String enabled = client.getAttribute(OIDC_CIBA_GRANT_ENABLED);
        return Boolean.parseBoolean(enabled);
    }

    public Algorithm getBackchannelAuthRequestSigningAlg(ClientModel client) {
        String alg = client.getAttribute(CIBA_BACKCHANNEL_AUTH_REQUEST_SIGNING_ALG);
        return alg==null ? null : Enum.valueOf(Algorithm.class, alg);
    }

    public String getBackchannelClientNotificationEndpoint(ClientModel client) {
        return client.getAttribute(CIBA_BACKCHANNEL_CLIENT_NOTIFICATION_ENDPOINT);
    }

    private Long getExpiryDurationInRawSec(String ttl) {
        boolean validate = validateTTL(ttl);
        if (!validate)
            ttl = "1h";
        String timeUnit = "";
        if (Character.isAlphabetic(ttl.charAt(ttl.length() - 1))) {
            timeUnit = ttl.substring(ttl.length() - 1);
            ttl = ttl.substring(0, ttl.length() - 1);
        } else {
            timeUnit = "h";
        }
        long duration = Long.parseLong(ttl);
        long expiry = 120;

        if (timeUnit.equals("d")) {
            expiry = TimeUnit.DAYS.toSeconds(duration);
        } else if (timeUnit.equals("h")) {
            expiry = TimeUnit.HOURS.toSeconds(duration);
        } else if (timeUnit.equals("s")) {
            expiry = duration;
        } else if(timeUnit.equals("m")) {
            expiry = TimeUnit.MINUTES.toSeconds(duration);
        }
        return expiry;
    }

    private static boolean validateTTL(String ttl) {
        if (ttl == null || ttl.length() == 0) {
            return false;
        }
        try {
            ttl = ttl.substring(0, ttl.length() - 1);
            Long.parseLong(ttl);
        } catch (Exception e) {
            return false;
        }
        return true;
    }
}
