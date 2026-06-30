package org.keycloak.policy.password;

import org.keycloak.models.KeycloakContext;
import org.keycloak.models.PasswordPolicy;
import org.keycloak.models.UserModel;
import org.keycloak.policy.PolicyError;
import org.keycloak.policy.password.BlacklistPasswordPolicyProviderFactory.FileBasedPasswordBlacklist;
import org.keycloak.policy.password.BlacklistPasswordPolicyProviderFactory.PasswordBlacklist;

/**
 * Checks a password against a configured password blacklist.
 *
 * @author <a href="mailto:thomas.darimont@gmail.com">Thomas Darimont</a>
 */
public class BlacklistPasswordPolicyProvider implements PasswordPolicyProvider {

  public static final String ERROR_MESSAGE = "invalidPasswordBlacklistedMessage";

  private final KeycloakContext context;

  private final BlacklistPasswordPolicyProviderFactory factory;

  public BlacklistPasswordPolicyProvider(KeycloakContext context, BlacklistPasswordPolicyProviderFactory factory) {
    this.context = context;
    this.factory = factory;
  }

  /**
   * Checks whether the provided password is contained in the configured blacklist.
   *
   * @param username
   * @param password
   * @return {@literal null} if the password is not blacklisted otherwise a {@link PolicyError}
   */
  @Override
  public PolicyError validate(PasswordPolicy policy,String username, String password) {

    Object policyConfig = policy.getPolicyConfig(BlacklistPasswordPolicyProviderFactory.ID);
    if (policyConfig == null) {
      return null;
    }

    if (!(policyConfig instanceof PasswordBlacklist)) {
      return null;
    }

    PasswordBlacklist blacklist = (FileBasedPasswordBlacklist) policyConfig;

    if (!blacklist.contains(password.toLowerCase())) {
      return null;
    }

    return new PolicyError(ERROR_MESSAGE);
  }

  @Override
  public PolicyError validate(PasswordPolicy policy, UserModel user, String password) {
    return validate(policy,user.getUsername(), password);
  }

  @Override
  public Object parseConfig(String blacklistName) {

    if (blacklistName == null) {
      return null;
    }

    return factory.resolvePasswordBlacklist(blacklistName);
  }

  @Override
  public String isValidConfig(Object config) {
    return "";
  }

  @Override
  public void close() {
    //noop
  }
}
