package io.ezto.helpers;

import org.springframework.core.io.ClassPathResource;
import org.springframework.util.FileCopyUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

public class GeneralHelper {

    private GeneralHelper(){

    }

    private static final String CHARS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVW1234567890";

    public static String getRandomValue(int len) {
        SecureRandom r = new SecureRandom();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < len; i++) {
            char c = CHARS.charAt(r.nextInt(CHARS.length()));
            sb.append(c);
        }
        return sb.toString();
    }

    public static long getEpochDuration(String ttl) {
        String timeUnit = "";
        if (Character.isAlphabetic(ttl.charAt(ttl.length() - 1))) {
            timeUnit = ttl.substring(ttl.length() - 1);
            ttl = ttl.substring(0, ttl.length() - 1);
        } else {
            timeUnit = "h";
        }
        long duration = Long.parseLong(ttl);
        long expiry;

        if (timeUnit.equals("d")) {
            expiry = System.currentTimeMillis() + TimeUnit.DAYS.toMillis(duration);
        } else if (timeUnit.equals("h")) {
            expiry = System.currentTimeMillis() + TimeUnit.HOURS.toMillis(duration);
        } else if (timeUnit.equals("s")) {
            expiry = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(duration);
        } else if (timeUnit.equals("m")) {
            expiry = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(duration);
        } else {
            expiry = System.currentTimeMillis() + TimeUnit.HOURS.toMillis(duration);
        }
        return (long)Math.floor((double)expiry / 1000);
    }

    public static String epochToFormattedDate(long timestamp) {
        Date date = new Date(timestamp);
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss");
        formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
        return formatter.format(date);
    }

    public static String getCurrentYear(){
        return String.valueOf(Calendar.getInstance().get(Calendar.YEAR));
    }

    public static long getCurrentTime() {
        return (System.currentTimeMillis() / 1000);
    }

    public static String getLicenseMessage(String licenseType) {
        if (licenseType.equals("cloud_pay_as_go")) {
            return "You have Cloud Pay-As-Go license";
        }

        return "You have OnPremise license";
    }

    public static String getContentFromPath(String path) {

        ClassPathResource cpr = new ClassPathResource(path);
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(cpr.getInputStream()));
            return FileCopyUtils.copyToString(reader);
        } catch (IOException e){
            e.printStackTrace();
        }
        return null;
    }


    public static Object getValueFromConfig(Map<String, Object> taskRequest, String key) {
        return taskRequest.get(key);
    }

    public static double round(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();

        long factor = (long) Math.pow(10, places);
        value = value * factor;
        long tmp = Math.round(value);
        return (double) tmp / factor;
    }
}
