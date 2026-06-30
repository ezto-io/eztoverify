package io.ezto.license.job;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import io.ezto.bg.core.IJobProvider;
import io.ezto.license.job.models.CountryCodes;

import io.ezto.license.utils.Constants;
import io.ezto.helpers.GeneralHelper;
import io.ezto.helpers.JsonHelper;
import io.ezto.helpers.utility.HttpUtility;
import okhttp3.Response;
import org.jobrunr.jobs.context.JobContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

import static io.ezto.helpers.GeneralHelper.getValueFromConfig;


public class LicenseUsageJobProvider implements IJobProvider {
    private static final Logger LOG = LoggerFactory.getLogger(LicenseUsageJobProvider.class);
    private static final String JOB_ATTRIBUTES = "job_attributes";
    private static final String PAYLOAD = "payload";
    private HttpUtility httpUtility = new HttpUtility();
    private static HashMap<String, Double> whatsappPricing;
    private static HashMap<String,Double> smsPricing;
    private static HashMap<String,Double> ivrPricing;
    private static final double USDTOINR = 82.5;

    static {
        whatsappPricing = new HashMap<>();
        whatsappPricing.put("AF", 0.0178);
        whatsappPricing.put("AL", 0.0557);
        whatsappPricing.put("DZ", 0.0144);
        whatsappPricing.put("AO", 0.0144);
        whatsappPricing.put("AR", 0.0367);
        whatsappPricing.put("AM", 0.0557);
        whatsappPricing.put("AU", 0.0425);
        whatsappPricing.put("AT", 0.0378);
        whatsappPricing.put("AZ", 0.0557);
        whatsappPricing.put("BH", 0.0178);
        whatsappPricing.put("BD", 0.0425);
        whatsappPricing.put("BY", 0.0557);
        whatsappPricing.put("BE", 0.0378);
        whatsappPricing.put("BJ", 0.0144);
        whatsappPricing.put("BO", 0.0445);
        whatsappPricing.put("BW", 0.0144);
        whatsappPricing.put("BR", 0.0144);
        whatsappPricing.put("BG", 0.0557);
        whatsappPricing.put("BF", 0.0144);
        whatsappPricing.put("BI", 0.0144);
        whatsappPricing.put("KH", 0.0425);
        whatsappPricing.put("CM", 0.0144);
        whatsappPricing.put("CA", 0.0135);
        whatsappPricing.put("TD", 0.0144);
        whatsappPricing.put("CL", 0.0527);
        whatsappPricing.put("CN", 0.0425);
        whatsappPricing.put("CO", 0.0077);
        whatsappPricing.put("CG", 0.0144);
        whatsappPricing.put("CR", 0.0445);
        whatsappPricing.put("HR", 0.0557);
        whatsappPricing.put("CZ", 0.0557);
        whatsappPricing.put("DK", 0.0378);
        whatsappPricing.put("DO", 0.0445);
        whatsappPricing.put("EC", 0.0445);
        whatsappPricing.put("EG", 0.0618);
        whatsappPricing.put("SV", 0.0445);
        whatsappPricing.put("ER", 0.0144);
        whatsappPricing.put("ET", 0.0144);
        whatsappPricing.put("FI", 0.0378);
        whatsappPricing.put("FR", 0.0691);
        whatsappPricing.put("GA", 0.0144);
        whatsappPricing.put("GM", 0.0144);
        whatsappPricing.put("GE", 0.0557);
        whatsappPricing.put("DE", 0.0768);
        whatsappPricing.put("GH", 0.0144);
        whatsappPricing.put("GR", 0.0557);
        whatsappPricing.put("GT", 0.0445);
        whatsappPricing.put("GW", 0.0144);
        whatsappPricing.put("HT", 0.0445);
        whatsappPricing.put("HN", 0.0445);
        whatsappPricing.put("HK", 0.0425);
        whatsappPricing.put("HU", 0.0557);
        whatsappPricing.put("IN", 0.0042);
        whatsappPricing.put("ID", 0.03);
        whatsappPricing.put("IQ", 0.0178);
        whatsappPricing.put("IE", 0.0378);
        whatsappPricing.put("IL", 0.0169);
        whatsappPricing.put("IT", 0.0378);
        whatsappPricing.put("CI", 0.0144);
        whatsappPricing.put("JM", 0.0445);
        whatsappPricing.put("JP", 0.0425);
        whatsappPricing.put("JO", 0.0178);
        whatsappPricing.put("KE", 0.0144);
        whatsappPricing.put("KW", 0.0178);
        whatsappPricing.put("LA", 0.0425);
        whatsappPricing.put("LV", 0.0557);
        whatsappPricing.put("LB", 0.0178);
        whatsappPricing.put("LS", 0.0144);
        whatsappPricing.put("LR", 0.0144);
        whatsappPricing.put("LY", 0.0144);
        whatsappPricing.put("LT", 0.0557);
        whatsappPricing.put("MK", 0.0557);
        whatsappPricing.put("MG", 0.0144);
        whatsappPricing.put("MW", 0.0144);
        whatsappPricing.put("MY", 0.018);
        whatsappPricing.put("ML", 0.0144);
        whatsappPricing.put("MR", 0.0144);
        whatsappPricing.put("MX", 0.0239);
        whatsappPricing.put("MD", 0.0557);
        whatsappPricing.put("MN", 0.0425);
        whatsappPricing.put("MA", 0.0144);
        whatsappPricing.put("MZ", 0.0144);
        whatsappPricing.put("NA", 0.0144);
        whatsappPricing.put("NP", 0.0425);
        whatsappPricing.put("NL", 0.072);
        whatsappPricing.put("NZ", 0.0425);
        whatsappPricing.put("NI", 0.0445);
        whatsappPricing.put("NE", 0.0144);
        whatsappPricing.put("NG", 0.0287);
        whatsappPricing.put("NO", 0.0378);
        whatsappPricing.put("OM", 0.0178);
        whatsappPricing.put("Other", 0.0304);
        whatsappPricing.put("PK", 0.0228);
        whatsappPricing.put("PA", 0.0445);
        whatsappPricing.put("PG", 0.0425);
        whatsappPricing.put("PY", 0.0445);
        whatsappPricing.put("PE", 0.0377);
        whatsappPricing.put("PH", 0.0425);
        whatsappPricing.put("PL", 0.0557);
        whatsappPricing.put("PT", 0.0378);
        whatsappPricing.put("PR", 0.0445);
        whatsappPricing.put("QA", 0.0178);
        whatsappPricing.put("RO", 0.0557);
        whatsappPricing.put("RU", 0.0429);
        whatsappPricing.put("RW", 0.0144);
        whatsappPricing.put("SA", 0.0226);
        whatsappPricing.put("SN", 0.0144);
        whatsappPricing.put("RS", 0.0557);
        whatsappPricing.put("SL", 0.0144);
        whatsappPricing.put("SG", 0.0425);
        whatsappPricing.put("SK", 0.0557);
        whatsappPricing.put("SI", 0.0557);
        whatsappPricing.put("SO", 0.0144);
        whatsappPricing.put("ZA", 0.018);
        whatsappPricing.put("SS", 0.0144);
        whatsappPricing.put("ES", 0.0342);
        whatsappPricing.put("LK", 0.0425);
        whatsappPricing.put("SD", 0.0144);
        whatsappPricing.put("SZ", 0.0144);
        whatsappPricing.put("SE", 0.0378);
        whatsappPricing.put("CH", 0.0378);
        whatsappPricing.put("TW", 0.0425);
        whatsappPricing.put("TJ", 0.0178);
        whatsappPricing.put("TZ", 0.0144);
        whatsappPricing.put("TH", 0.0425);
        whatsappPricing.put("TG", 0.0144);
        whatsappPricing.put("TN", 0.0144);
        whatsappPricing.put("TR", 0.0083);
        whatsappPricing.put("TM", 0.0178);
        whatsappPricing.put("UG", 0.0144);
        whatsappPricing.put("UA", 0.0557);
        whatsappPricing.put("AE", 0.0178);
        whatsappPricing.put("GB", 0.0358);
        whatsappPricing.put("US", 0.0135);
        whatsappPricing.put("UY", 0.0445);
        whatsappPricing.put("UZ", 0.0178);
        whatsappPricing.put("VE", 0.0445);
        whatsappPricing.put("VN", 0.0425);
        whatsappPricing.put("YE", 0.0178);
        whatsappPricing.put("ZM", 0.0144);
    }
    static {
        smsPricing = new HashMap<>();
        smsPricing.put("Other",0.1);
        smsPricing.put("AD",0.1317);
        smsPricing.put("AE",0.1092);
        smsPricing.put("AF",0.2337);
        smsPricing.put("AG",0.098);
        smsPricing.put("AI",0.077);
        smsPricing.put("AL",0.091);
        smsPricing.put("AM",0.1384);
        smsPricing.put("AO",0.0778);
        smsPricing.put("AR",0.0743);
        smsPricing.put("AS",0.1881);
        smsPricing.put("AT",0.0979);
        smsPricing.put("AU",0.0515);
        smsPricing.put("AW",0.1034);
        smsPricing.put("AZ",0.3274);
        smsPricing.put("BA",0.084);
        smsPricing.put("BB",0.0984);
        smsPricing.put("BD",0.2653);
        smsPricing.put("BE",0.105);
        smsPricing.put("BF",0.1329);
        smsPricing.put("BG",0.1234);
        smsPricing.put("BH",0.0265);
        smsPricing.put("BI",0.2578);
        smsPricing.put("BJ",0.1445);
        smsPricing.put("BM",0.1186);
        smsPricing.put("BN",0.02);
        smsPricing.put("BO",0.1221);
        smsPricing.put("BR",0.0599);
        smsPricing.put("BS",0.0525);
        smsPricing.put("BT",0.2509);
        smsPricing.put("BW",0.0998);
        smsPricing.put("BY",0.2521);
        smsPricing.put("BZ",0.2738);
        smsPricing.put("CA",0.0079);
        smsPricing.put("CD",0.1377);
        smsPricing.put("CF",0.101);
        smsPricing.put("CG",0.1005);
        smsPricing.put("CH",0.0725);
        smsPricing.put("CI",0.1255);
        smsPricing.put("CK",0.0764);
        smsPricing.put("CL",0.0651);
        smsPricing.put("CM",0.1383);
        smsPricing.put("CN",0.0368);
        smsPricing.put("CO",0.0525);
        smsPricing.put("CR",0.042);
        smsPricing.put("CU",0.084);
        smsPricing.put("CV",0.1228);
        smsPricing.put("CY",0.0864);
        smsPricing.put("CZ",0.0572);
        smsPricing.put("DE",0.094);
        smsPricing.put("DJ",0.1126);
        smsPricing.put("DK",0.0524);
        smsPricing.put("DM",0.1248);
        smsPricing.put("DO",0.0599);
        smsPricing.put("DZ",0.208);
        smsPricing.put("EC",0.1485);
        smsPricing.put("EE",0.0958);
        smsPricing.put("EG",0.1851);
        smsPricing.put("ER",0.1166);
        smsPricing.put("ES",0.0875);
        smsPricing.put("ET",0.2282);
        smsPricing.put("FI",0.0861);
        smsPricing.put("FJ",0.1073);
        smsPricing.put("FK",0.136);
        smsPricing.put("FM",0.0735);
        smsPricing.put("FO",0.0473);
        smsPricing.put("FR",0.0798);
        smsPricing.put("GA",0.1629);
        smsPricing.put("GB",0.042);
        smsPricing.put("GD",0.1623);
        smsPricing.put("GE",0.1193);
        smsPricing.put("GF",0.1842);
        smsPricing.put("GH",0.1468);
        smsPricing.put("GI",0.0848);
        smsPricing.put("GL",0.041);
        smsPricing.put("GM",0.1071);
        smsPricing.put("GN",0.1517);
        smsPricing.put("GP",0.135);
        smsPricing.put("GQ",0.1029);
        smsPricing.put("GR",0.062);
        smsPricing.put("GT",0.1269);
        smsPricing.put("GU",0.0687);
        smsPricing.put("GW",0.1575);
        smsPricing.put("GY",0.1787);
        smsPricing.put("HK",0.0572);
        smsPricing.put("HN",0.0847);
        smsPricing.put("HR",0.0768);
        smsPricing.put("HT",0.1658);
        smsPricing.put("HU",0.091);
        smsPricing.put("ID",0.3603);
        smsPricing.put("IE",0.0735);
        smsPricing.put("IL",0.112);
        smsPricing.put("IN",0.0682);
        smsPricing.put("IQ",0.2078);
        smsPricing.put("IR",0.1833);
        smsPricing.put("IS",0.0661);
        smsPricing.put("IT",0.0927);
        smsPricing.put("JM",0.1644);
        smsPricing.put("JO",0.1791);
        smsPricing.put("JP",0.084);
        smsPricing.put("KE",0.105);
        smsPricing.put("KG",0.2428);
        smsPricing.put("KH",0.1409);
        smsPricing.put("KI",0.123);
        smsPricing.put("KM",0.1571);
        smsPricing.put("KN",0.0931);
        smsPricing.put("KR",0.0494);
        smsPricing.put("KW",0.1632);
        smsPricing.put("KY",0.1337);
        smsPricing.put("KZ",0.2192);
        smsPricing.put("LA",0.061);
        smsPricing.put("LB",0.1503);
        smsPricing.put("LC",0.1028);
        smsPricing.put("LI",0.0334);
        smsPricing.put("LK",0.2428);
        smsPricing.put("LR",0.0937);
        smsPricing.put("LS",0.1024);
        smsPricing.put("LT",0.0431);
        smsPricing.put("LU",0.074);
        smsPricing.put("LV",0.0715);
        smsPricing.put("LY",0.3242);
        smsPricing.put("MA",0.1463);
        smsPricing.put("MC",0.1856);
        smsPricing.put("MD",0.063);
        smsPricing.put("ME",0.081);
        smsPricing.put("MG",0.217);
        smsPricing.put("MH",0.1589);
        smsPricing.put("MK",0.0744);
        smsPricing.put("ML",0.1196);
        smsPricing.put("MM",0.1894);
        smsPricing.put("MN",0.2246);
        smsPricing.put("MO",0.059);
        smsPricing.put("MQ",0.132);
        smsPricing.put("MR",0.1463);
        smsPricing.put("MS",0.0915);
        smsPricing.put("MT",0.065);
        smsPricing.put("MU",0.1349);
        smsPricing.put("MV",0.0587);
        smsPricing.put("MW",0.227);
        smsPricing.put("MX",0.0515);
        smsPricing.put("MY",0.0905);
        smsPricing.put("MZ",0.1094);
        smsPricing.put("NA",0.0626);
        smsPricing.put("NC",0.1761);
        smsPricing.put("NE",0.1766);
        smsPricing.put("NF",0.02817);
        smsPricing.put("NG",0.1982);
        smsPricing.put("NI",0.0774);
        smsPricing.put("NL",0.09);
        smsPricing.put("NL",0.0966);
        smsPricing.put("NO",0.0651);
        smsPricing.put("NP",0.1516);
        smsPricing.put("NU",0.1928);
        smsPricing.put("NZ",0.105);
        smsPricing.put("OM",0.1015);
        smsPricing.put("PA",0.0986);
        smsPricing.put("PE",0.0525);
        smsPricing.put("PF",0.1797);
        smsPricing.put("PG",0.1419);
        smsPricing.put("PH",0.1699);
        smsPricing.put("PK",0.2184);
        smsPricing.put("PL",0.0431);
        smsPricing.put("PM",0.1507);
        smsPricing.put("PR",0.042);
        smsPricing.put("PS",0.3267);
        smsPricing.put("PT",0.0473);
        smsPricing.put("PW",0.0998);
        smsPricing.put("PY",0.0573);
        smsPricing.put("QA",0.0641);
        smsPricing.put("QK",0.1522);
        smsPricing.put("RE",0.1911);
        smsPricing.put("RO",0.0737);
        smsPricing.put("RS",0.0831);
        smsPricing.put("RU",0.7096);
        smsPricing.put("RW",0.1849);
        smsPricing.put("SA",0.0872);
        smsPricing.put("SB",0.083);
        smsPricing.put("SC",0.0372);
        smsPricing.put("SD",0.1085);
        smsPricing.put("SD",0.229);
        smsPricing.put("SE",0.0578);
        smsPricing.put("SG",0.0415);
        smsPricing.put("SI",0.1491);
        smsPricing.put("SK",0.0883);
        smsPricing.put("SL",0.1049);
        smsPricing.put("SM",0.1096);
        smsPricing.put("SN",0.2044);
        smsPricing.put("SO",0.1879);
        smsPricing.put("SR",0.1301);
        smsPricing.put("ST",0.1307);
        smsPricing.put("SV",0.0566);
        smsPricing.put("SY",0.2488);
        smsPricing.put("SZ",0.0919);
        smsPricing.put("TC",0.0921);
        smsPricing.put("TD",0.1847);
        smsPricing.put("TG",0.0578);
        smsPricing.put("TH",0.0305);
        smsPricing.put("TJ",0.2792);
        smsPricing.put("TL",0.1135);
        smsPricing.put("TM",0.1617);
        smsPricing.put("TN",0.2091);
        smsPricing.put("TO",0.1245);
        smsPricing.put("TR",0.0305);
        smsPricing.put("TT",0.1298);
        smsPricing.put("TV",0.105);
        smsPricing.put("TW",0.0567);
        smsPricing.put("TZ",0.1804);
        smsPricing.put("UA",0.115);
        smsPricing.put("UG",0.2122);
        smsPricing.put("US",0.0079);
        smsPricing.put("UY",0.0852);
        smsPricing.put("UZ",0.3143);
        smsPricing.put("VC",0.1317);
        smsPricing.put("VE",0.0852);
        smsPricing.put("VG",0.1434);
        smsPricing.put("VI",0.02);
        smsPricing.put("VN",0.1552);
        smsPricing.put("VU",0.1657);
        smsPricing.put("WF",0.11);
        smsPricing.put("WS",0.1459);
        smsPricing.put("YE",0.2391);
        smsPricing.put("ZA",0.0284);
        smsPricing.put("ZM",0.2229);
        smsPricing.put("ZW",0.1407);
    }

    static{
        ivrPricing = new HashMap<>();
        ivrPricing.put("IN",0.0368);
        ivrPricing.put("AE",0.245);
        ivrPricing.put("US",0.014);
        ivrPricing.put("Other",3.3);
    }

    public LicenseUsageJobProvider() {}

    @Override
    public String getProviderId() {
        return "usage";
    }

    @Override
    public void doExecute(Map<String, Object> taskRequest, JobContext jobContext) {

        Map<String, String> jobAttributes = (Map<String, String>) getValueFromConfig(taskRequest, JOB_ATTRIBUTES);
        Object payloadObj = getValueFromConfig(taskRequest, PAYLOAD);
        HashMap<String, Object> payload = (HashMap<String, Object>) JsonHelper.getInstance().getObjectMapper().convertValue(payloadObj, Map.class);
        HashMap<String, Object> attrs = JsonHelper.getInstance().getObjectMapper().convertValue(payload.get("attributes"), HashMap.class);

        if (payload.containsKey("pricing")) {
            String url = jobAttributes.get("url");

            String product = String.valueOf(payload.get("product_id"));

            Map<String, Object> body = addAmountAndTransactionDetailsToBody(payload, product, attrs);

            Response response = httpUtility.callRequest("application/json", "POST", url, JsonHelper.getInstance().getStringified(body), null);
            if (response.isSuccessful()) {
                httpUtility.callRequest("application/json", "GET", String.valueOf(payload.get(Constants.TENANT_SUBSCRIPTION_URL)), null, null);
            }
        } else if (payload.containsKey("subscription")) {
            String url = jobAttributes.get("url");
            HashMap<String, Object> body = new HashMap<>();
            body.put("tenantId", String.valueOf(payload.get("tenant_id")));
            body.put("productId", "subscription");
            body.put(Constants.AMOUNT, attrs.get("outstanding_amount"));
            body.put(Constants.TRANSACTION_DETAILS,attrs);

            Response response = httpUtility.callRequest("application/json", "POST", url, JsonHelper.getInstance().getStringified(body), null);
            if (response.isSuccessful()) {
                httpUtility.callRequest("application/json", "GET", String.valueOf(payload.get(Constants.TENANT_SUBSCRIPTION_URL)), null, null);
            }

        }else {
            String url = jobAttributes.get("url");
            HashMap<String, Object> body = new HashMap<>();
            body.put("usage", payload.get("usage"));
            body.put("timestamp", payload.get("timestamp"));
            Object subItemId = payload.get("product_id") != null ? payload.get("product_id") : payload.get(
                    "subItemId");
            body.put("subItemId", String.valueOf(subItemId));

            Response response = httpUtility.callRequest("application/json", "POST", url, JsonHelper.getInstance().getStringified(body), null);
            if (response.isSuccessful()) {
                httpUtility.callRequest("application/json", "GET", String.valueOf(jobAttributes.get(Constants.TENANT_SUBSCRIPTION_URL)), null, null);
            }
        }

    }

    public Map<String, Object> addAmountAndTransactionDetailsToBody(Map<String, Object> payload, String product, Map<String, Object> attrs){

        HashMap<String, Object> body = new HashMap<>();

        body.put("tenantId", String.valueOf(payload.get("tenant_id")));
        body.put("productId", product);
        Object price = attrs.getOrDefault("price", null);
        if (product.equals("sms")) {

            body.put(Constants.AMOUNT,
                    price == null ? getSMSPricing(String.valueOf(attrs.get(Constants.TO_PHONE)), String.valueOf(payload.get(Constants.CURRENCY))): price);
            body.put(Constants.TRANSACTION_DETAILS,attrs == null ? new HashMap<>() :attrs);
        }
        if (product.equals("whatsapp")) {
            body.put(Constants.AMOUNT,
                    price == null ? getWhatsappPricing(String.valueOf(attrs.get(Constants.TO_PHONE)), String.valueOf(payload.get(Constants.CURRENCY))): price);
            body.put(Constants.TRANSACTION_DETAILS,attrs == null ? new HashMap<>() :attrs);
        }

        if (product.equals("email")) {
            body.put(Constants.AMOUNT, 0.0);
            body.put(Constants.TRANSACTION_DETAILS, attrs == null ? new HashMap<>() :attrs );
        }

        if (product.equals("ivr")) {
            body.put(Constants.AMOUNT, 
                    price == null ? getIvrPricing(String.valueOf(attrs.get(Constants.TO_PHONE)), String.valueOf(payload.get(Constants.CURRENCY))): price);
            body.put(Constants.TRANSACTION_DETAILS,attrs);
        }

        if(product.equals("bo_tran"))
        {
            DoubleSummaryStatistics collect = attrs.values().stream().collect(Collectors.summarizingDouble(e -> (Double) e));
            body.put(Constants.AMOUNT, collect.getSum());
            body.put(Constants.TRANSACTION_DETAILS,attrs == null ? new HashMap<>() :attrs);
        }
        return body;
    }

    @Override
    public List<String> validateRequest(Map<String, Object> taskRequest) {
        List<String> errors = new ArrayList<>();
        Map<String, String> jobAttributes = (Map<String, String>) getValueFromConfig(taskRequest, JOB_ATTRIBUTES);
        if (jobAttributes == null || jobAttributes.size() == 0) {
            errors.add(JOB_ATTRIBUTES + " are mandatory");
            return errors;
        }
        Object payload = getValueFromConfig(taskRequest, PAYLOAD);
        if (payload == null) {
            errors.add(PAYLOAD + " are mandatory");
            return errors;
        }

        return errors;
    }

    public double getSMSPricing(String phone, String currency) {
        int countryPhoneCode = getCountryCode(phone);
        String countryISOCode = CountryCodes.getISOCode(String.valueOf(countryPhoneCode));
        if (countryISOCode == null || !smsPricing.containsKey(countryISOCode))
            countryISOCode = "Other";

        double charge = smsPricing.get(countryISOCode);

        if (currency.equalsIgnoreCase("inr"))
            return GeneralHelper.round(charge * USDTOINR,4);

        return charge;
    }

    public double getWhatsappPricing(String phone, String currency) {
        int countryPhoneCode = getCountryCode(phone);
        String countryISOCode = CountryCodes.getISOCode(String.valueOf(countryPhoneCode));
        if (countryISOCode == null || !whatsappPricing.containsKey(countryISOCode))
            countryISOCode = "Other";

        double charge = whatsappPricing.get(countryISOCode);
        charge += 0.005;

        if (currency.equalsIgnoreCase("inr"))
            return GeneralHelper.round(charge * USDTOINR,4);

        return charge;
    }

    public double getIvrPricing(String phone, String currency) {
        int countryPhoneCode = getCountryCode(phone);
        String countryISOCode = CountryCodes.getISOCode(String.valueOf(countryPhoneCode));
        if (countryISOCode == null || !whatsappPricing.containsKey(countryISOCode))
            countryISOCode = "Other";

        double charge = ivrPricing.get(countryISOCode);

        if (currency.equalsIgnoreCase("inr"))
            return GeneralHelper.round(charge * USDTOINR,4);

        return charge;
    }

    public int getCountryCode(String phone) {
        PhoneNumberUtil phoneUtil = PhoneNumberUtil.getInstance();
        try {
            // phone must begin with '+'
            Phonenumber.PhoneNumber numberProto = phoneUtil.parse(phone, "");
            return numberProto.getCountryCode();
        } catch (NumberParseException e) {
            LOG.error("NumberParseException was thrown: " + e.toString());
        }
        return 0;
    }
}
