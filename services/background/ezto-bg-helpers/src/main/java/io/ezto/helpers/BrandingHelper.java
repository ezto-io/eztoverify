package io.ezto.helpers;

import java.util.HashMap;
import java.util.Map;

public class BrandingHelper {

    static Map<String, Map<String,String>> brandingDetails = new HashMap<>();

    static {
        // CIAM colors
        Map<String, String> ciamColors = new HashMap<>();
        ciamColors.put("bodyColor", "#2E2E2E");
        ciamColors.put("footerColor", "#2E2E2E");
        brandingDetails.put("ciam", ciamColors);

        // WIAM colors
        Map<String, String> wiamColors = new HashMap<>();
        wiamColors.put("bodyColor", "#4CA355");
        wiamColors.put("footerColor", "#2d5b3a");
        brandingDetails.put("wiam", wiamColors);

        // BO colors
        Map<String, String> boColors = new HashMap<>();
        boColors.put("bodyColor", "#4B72FA");
        boColors.put("footerColor", "#05173f");
        brandingDetails.put("bo", boColors);
    }

    public static Map<String, String> getBrandingDetails(String brandType) {
        if(brandType == null){
            brandType="bo";
        }
        return brandingDetails.get(brandType);
    }

}
