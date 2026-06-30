package com.qlikverify.services.policies.action.models;

import java.util.ArrayList;
import java.util.List;

public class IPRangeModel {
    private boolean hasIPRange;
    private List<IPRange> ipRanges;
    private List<String> staticIps;

    public boolean isHasIPRange() {
        return ipRanges != null && ipRanges.size() > 0;
    }

    public List<IPRange> getIpRanges() {
        if (ipRanges == null)
            ipRanges = new ArrayList<>();
        return ipRanges;
    }

    public List<String> getStaticIps() {
        if (staticIps == null)
            staticIps = new ArrayList<>();
        return staticIps;
    }
}
