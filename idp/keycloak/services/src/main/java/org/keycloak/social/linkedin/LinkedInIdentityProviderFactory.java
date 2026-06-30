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
package org.keycloak.social.linkedin;

import org.keycloak.AvatarConstants;
import org.keycloak.EnvironmentReader;
import org.keycloak.broker.oidc.OAuth2IdentityProviderConfig;
import org.keycloak.broker.provider.AbstractIdentityProviderFactory;
import org.keycloak.broker.provider.IdentityProviderFactory;
import org.keycloak.models.Constants;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.broker.social.SocialIdentityProviderFactory;
import org.keycloak.models.KeycloakSession;

/**
 * @author Vlastimil Elias (velias at redhat dot com)
 */
public class LinkedInIdentityProviderFactory extends AbstractIdentityProviderFactory<LinkedInIdentityProvider>
		implements SocialIdentityProviderFactory<LinkedInIdentityProvider> {

	public static final String PROVIDER_ID = "linkedin";
	private static String DEFAULT_SERVER_URL = EnvironmentReader.getValue(AvatarConstants.MINIO_PUBLIC_SERVER_URL);

	@Override
	public String getName() {
		return "LinkedIn";
	}

	@Override
	public String getProviderClass() {
		return (IdentityProviderFactory.class).getSimpleName();
	}

	@Override
	public String getSetupGuideLink() {
		return DEFAULT_SERVER_URL + "/Setup-Guide/Social-Connections/LinkedIn.md";
	}

	@Override
	public LinkedInIdentityProvider create(KeycloakSession session, IdentityProviderModel model) {
		return new LinkedInIdentityProvider(session, new OAuth2IdentityProviderConfig(model));
	}

	@Override
	public OAuth2IdentityProviderConfig createConfig() {
		return new OAuth2IdentityProviderConfig();
	}

	@Override
	public String getId() {
		return PROVIDER_ID;
	}
}
