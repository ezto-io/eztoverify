package com.ezto.bg.license;

import io.ezto.license.job.LicenseUsageJobProvider;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class LicenseUsageJobTests {

    @Test
    public void phonecodetesting_91(){
        LicenseUsageJobProvider licenseUsageJobProvider = new LicenseUsageJobProvider();
        int countryCode = licenseUsageJobProvider.getCountryCode("+919789024698");
        assertTrue(countryCode == 91);
    }

    @Test
    public void phonecodetesting_44(){
        LicenseUsageJobProvider licenseUsageJobProvider = new LicenseUsageJobProvider();
        int countryCode = licenseUsageJobProvider.getCountryCode("+44 7911 123456");
        assertTrue(countryCode == 44);
    }


    @Test
    public void whatsapppricing_inr_91(){
        LicenseUsageJobProvider licenseUsageJobProvider = new LicenseUsageJobProvider();
        double charges = licenseUsageJobProvider.getWhatsappPricing("+919789024698","INR");
        double expectedCharges = (0.0042+0.005)*82.5;
        assertTrue(charges == expectedCharges);
    }

    @Test
    public void whatsapppricing_usd_91(){
        LicenseUsageJobProvider licenseUsageJobProvider = new LicenseUsageJobProvider();
        double charges = licenseUsageJobProvider.getWhatsappPricing("+919789024698","USD");
        double expectedCharges = (0.0042+0.005);
        assertTrue(charges == expectedCharges);
    }

    @Test
    public void whatsapppricing_inr_unknown(){
        LicenseUsageJobProvider licenseUsageJobProvider = new LicenseUsageJobProvider();
        double charges = licenseUsageJobProvider.getWhatsappPricing("123445452353","INR");
        double expectedCharges = (0.0304+0.005)*82.5;
        assertTrue(charges == expectedCharges);
    }

    @Test
    public void smspricing_inr_91(){
        LicenseUsageJobProvider licenseUsageJobProvider = new LicenseUsageJobProvider();
        double charges = licenseUsageJobProvider.getSMSPricing("+919789024698","INR");
        double expectedCharges = (0.0682)*82.5;
        assertTrue(charges == expectedCharges);
    }

    @Test
    public void smspricing_usd_91(){
        LicenseUsageJobProvider licenseUsageJobProvider = new LicenseUsageJobProvider();
        double charges = licenseUsageJobProvider.getSMSPricing("+919789024698","USD");
        double expectedCharges = (0.0682);
        assertTrue(charges == expectedCharges);
    }


    @Test
    public void smspricing_inr_unknown(){
        LicenseUsageJobProvider licenseUsageJobProvider = new LicenseUsageJobProvider();
        double charges = licenseUsageJobProvider.getSMSPricing("123445452353","INR");
        double expectedCharges = (0.1)*82.5;
        assertTrue(charges == expectedCharges);
    }
}
