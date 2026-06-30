package com.qlikverify.services.policies.action;

import com.qlikverify.services.helpers.GeneralHelper;
import com.qlikverify.services.policies.action.models.IPRange;
import com.qlikverify.services.policies.action.models.IPRangeModel;
import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.models.ActionPolicy;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.UserModel;
import org.keycloak.policy.action.ActionPolicyProvider;
import org.keycloak.policy.action.ActionPolicyProviderFactory;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;

public class IPRangePolicyProvider implements ActionPolicyProvider, ActionPolicyProviderFactory {

    public static final String ID = "iprange";

    private static final Logger LOGGER = Logger.getLogger(IPRangePolicyProvider.class);

    @Override
    public boolean validate(KeycloakSession session, ActionPolicy policy, UserModel user) {
        IPRangeModel policyConfig = policy.getPolicyConfig(ID);
        if (policyConfig == null)
            return false;

        if(session.getContext().getConnection() == null)
            return false;

        //Run Unit Tests in case u touch the logic
        return isNotInRange(policyConfig,session.getContext().getConnection().getRemoteAddr());
    }

    @Override
    public IPRangeModel parseConfig(String value) {
        if (value == null || value.isEmpty())
            return null;

        String[] collections = value.trim().split(";");

        IPRangeModel ipRangeModel = new IPRangeModel();
        Arrays.stream(collections).forEach(x -> {
            if (x.contains("to")) {
                String[] parsed = x.trim().split("to");
                IPRange ipRange = new IPRange();
                ipRange.setFrom(parsed[0]);
                ipRange.setTo(parsed[1]);
                ipRangeModel.getIpRanges().add(ipRange);
            } else {
                ipRangeModel.getStaticIps().add(x);
            }
        });
        return ipRangeModel;
    }

    @Override
    public String getDisplayName() {
        return "not-in-ip-range";
    }

    @Override
    public String getConfigType() {
        return ActionPolicyProvider.STRING_CONFIG_TYPE;
    }

    @Override
    public String getDefaultConfigValue() {
        return "";
    }

    @Override
    public boolean isMultiplSupported() {
        return false;
    }

    @Override
    public ActionPolicyProvider create(KeycloakSession session) {
        return this;
    }

    @Override
    public void init(Config.Scope config) {
        // NOP
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        // NOP
    }

    @Override
    public void close() {
        // NOP
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public String[] supportedModes() {
        return new String[] {"wiam", "ciam", "bo"};
    }

    public boolean isNotInRange(IPRangeModel rangeModel, String inputIp)
    {
        boolean isNotInRange = true;

        if(inputIp == null || rangeModel == null)
            return isNotInRange;


        if(rangeModel.getStaticIps().size() > 0 &&  (rangeModel.getStaticIps().stream().filter(x -> x.equals(inputIp)).count() > 0)){
                isNotInRange = false;
        }

        if(isNotInRange &&  (rangeModel.isHasIPRange()) &&  (rangeModel.getIpRanges().stream().filter(x -> {
                    try {
                        return checkIPv4IsInRangeByConvertingToInt(inputIp, x.getFrom(), x.getTo());
                    } catch (UnknownHostException e) {
                        e.printStackTrace();
                    }
                    return false;
                }).count() > 0)) {
                    isNotInRange = false;
        }

        return isNotInRange;
    }

    public boolean checkIPv4IsInRangeByConvertingToInt(String inputIP, String rangeStartIP, String rangeEndIP)
            throws UnknownHostException {
        long startIPAddress = GeneralHelper.getInstance().ipToLongInt(InetAddress.getByName(rangeStartIP));
        long endIPAddress = GeneralHelper.getInstance().ipToLongInt(InetAddress.getByName(rangeEndIP));
        long inputIPAddress = GeneralHelper.getInstance().ipToLongInt(InetAddress.getByName(inputIP));

        return (inputIPAddress >= startIPAddress && inputIPAddress <= endIPAddress);
    }
}
