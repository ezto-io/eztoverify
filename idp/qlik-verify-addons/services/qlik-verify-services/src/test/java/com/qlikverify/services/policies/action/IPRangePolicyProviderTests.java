package com.qlikverify.services.policies.action;

import com.qlikverify.services.policies.action.models.IPRangeModel;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class IPRangePolicyProviderTests {

    @Test
    public void testConfig1() {
        IPRangePolicyProvider policyProvider = new IPRangePolicyProvider();
        IPRangeModel rangeModel = policyProvider.parseConfig("127.0.0.1");
        assertEquals(rangeModel.getStaticIps().get(0),"127.0.0.1");
    }

    @Test
    public void testConfig2() {
        IPRangePolicyProvider policyProvider = new IPRangePolicyProvider();
        IPRangeModel rangeModel = policyProvider.parseConfig("127.0.0.1;127.0.0.2");
        assertEquals(rangeModel.getStaticIps().size(),2);
        assertEquals(rangeModel.isHasIPRange(),false);
        assertEquals(rangeModel.getStaticIps().get(1),"127.0.0.2");
    }

    @Test
    public void testConfig3() {
        IPRangePolicyProvider policyProvider = new IPRangePolicyProvider();
        IPRangeModel rangeModel = policyProvider.parseConfig("127.0.0.1;127.0.0.2;127.0.0.0to192.2.0.2");
        assertEquals(rangeModel.getStaticIps().size(),2);
        assertEquals(rangeModel.getStaticIps().get(1),"127.0.0.2");
        assertEquals(rangeModel.isHasIPRange(),true);
        assertEquals(rangeModel.getIpRanges().size(),1);
        assertEquals(rangeModel.getIpRanges().get(0).getFrom(),"127.0.0.0");
        assertEquals(rangeModel.getIpRanges().get(0).getTo(),"192.2.0.2");
    }

    @Test
    public void testConfig4() {
        IPRangePolicyProvider policyProvider = new IPRangePolicyProvider();
        IPRangeModel rangeModel = policyProvider.parseConfig("127.0.0.1;127.0.0.2;127.0.0.0to192.2.0.2;127.0.0.3");
        assertEquals(rangeModel.getStaticIps().size(),3);
        assertEquals(rangeModel.getStaticIps().get(1),"127.0.0.2");
        assertEquals(rangeModel.getStaticIps().get(2),"127.0.0.3");
        assertEquals(rangeModel.isHasIPRange(),true);
        assertEquals(rangeModel.getIpRanges().size(),1);
        assertEquals(rangeModel.getIpRanges().get(0).getFrom(),"127.0.0.0");
        assertEquals(rangeModel.getIpRanges().get(0).getTo(),"192.2.0.2");
    }

    @Test
    public void testConfig5() {
        IPRangePolicyProvider policyProvider = new IPRangePolicyProvider();
        IPRangeModel rangeModel = policyProvider.parseConfig("127.0.0.0to192.2.0.2;");
        assertEquals(rangeModel.getStaticIps().size(),0);
        assertEquals(rangeModel.isHasIPRange(),true);
        assertEquals(rangeModel.getIpRanges().size(),1);
        assertEquals(rangeModel.getIpRanges().get(0).getFrom(),"127.0.0.0");
        assertEquals(rangeModel.getIpRanges().get(0).getTo(),"192.2.0.2");
    }

    @Test
    public void testConfig6() {
        IPRangePolicyProvider policyProvider = new IPRangePolicyProvider();
        IPRangeModel rangeModel = policyProvider.parseConfig("127.0.0.0to192.2.0.2;127.0.0.1to192.2.0.1;");
        assertEquals(rangeModel.getStaticIps().size(),0);
        assertEquals(rangeModel.isHasIPRange(),true);
        assertEquals(rangeModel.getIpRanges().size(),2);
        assertEquals(rangeModel.getIpRanges().get(0).getFrom(),"127.0.0.0");
        assertEquals(rangeModel.getIpRanges().get(0).getTo(),"192.2.0.2");
        assertEquals(rangeModel.getIpRanges().get(1).getFrom(),"127.0.0.1");
        assertEquals(rangeModel.getIpRanges().get(1).getTo(),"192.2.0.1");
    }

    @Test
    public void testEvaluation1() {
        IPRangePolicyProvider policyProvider = new IPRangePolicyProvider();
        IPRangeModel rangeModel = policyProvider.parseConfig("127.0.0.0to192.2.0.2;127.0.0.1to192.2.0.1;");
        boolean isNotInRange = policyProvider.isNotInRange(rangeModel,"127.0.0.1");
        assertEquals(isNotInRange,false);
    }

    @Test
    public void testEvaluation2() {
        IPRangePolicyProvider policyProvider = new IPRangePolicyProvider();
        IPRangeModel rangeModel = policyProvider.parseConfig("127.0.0.1;127.0.0.0to192.2.0.2;127.0.0.1to192.2.0.1;");
        boolean isNotInRange = policyProvider.isNotInRange(rangeModel,"127.0.0.1");
        assertEquals(isNotInRange,false);
    }

    @Test
    public void testEvaluation3() {
        IPRangePolicyProvider policyProvider = new IPRangePolicyProvider();
        IPRangeModel rangeModel = policyProvider.parseConfig("127.0.0.1;127.0.0.0to192.2.0.2;127.0.0.1to192.2.0.1;");
        boolean isNotInRange = policyProvider.isNotInRange(rangeModel,"126.0.0.1");
        assertEquals(isNotInRange,true);
    }

    @Test
    public void testEvaluation4() {
        IPRangePolicyProvider policyProvider = new IPRangePolicyProvider();
        IPRangeModel rangeModel = policyProvider.parseConfig("192.0.0.0to192.2.0.2;127.0.0.1to192.2.0.1;");
        boolean isNotInRange = policyProvider.isNotInRange(rangeModel,"126.0.0.1");
        assertEquals(isNotInRange,true);
    }

    @Test
    public void testEvaluation5() {
        IPRangePolicyProvider policyProvider = new IPRangePolicyProvider();
        IPRangeModel rangeModel = policyProvider.parseConfig("");
        boolean isNotInRange = policyProvider.isNotInRange(rangeModel,"126.0.0.1");
        assertEquals(isNotInRange,true);
    }

    @Test
    public void testEvaluation6() {
        IPRangePolicyProvider policyProvider = new IPRangePolicyProvider();
        IPRangeModel rangeModel = policyProvider.parseConfig(null);
        boolean isNotInRange = policyProvider.isNotInRange(rangeModel,"126.0.0.1");
        assertEquals(isNotInRange,true);
    }
}
