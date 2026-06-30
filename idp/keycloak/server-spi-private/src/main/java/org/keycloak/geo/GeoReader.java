package org.keycloak.geo;

import com.maxmind.db.CHMCache;
import com.maxmind.db.Reader;
import com.maxmind.geoip2.DatabaseReader;
import com.maxmind.geoip2.exception.GeoIp2Exception;
import com.maxmind.geoip2.model.CityResponse;
import org.keycloak.common.models.Body;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.URL;
import java.net.URLConnection;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.time.ZoneId;

import static java.util.Objects.nonNull;

public class GeoReader {

    private static GeoReader instance;
    private static DatabaseReader databaseReader;
    private static final String MINIO_SERVER_URL = "MINIO_SERVER_URL";
    private static final String MINIO_PUBLIC_SERVER_URL = "MINIO_PUBLIC_URL";
    private static final String UNKNOWN = "UNKNOWN";

    private GeoReader() {
        // Private constructor to prevent instantiation.
        initialize();
    }

    public static GeoReader getInstance() {
        if (instance == null) {
            synchronized (GeoReader.class) {
                if (instance == null) {
                    instance = new GeoReader();
                }
            }
        }
        return instance;
    }

    private void initialize() {
        TrustManager[] trustAllCertificates = new TrustManager[]{
                new X509TrustManager() {
                    public X509Certificate[] getAcceptedIssuers() {
                        return null;
                    }

                    public void checkClientTrusted(X509Certificate[] certs, String authType) {
                        // NOP
                    }

                    public void checkServerTrusted(X509Certificate[] certs, String authType) {
                        // NOP
                    }
                }
        };

        try {

            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustAllCertificates, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());

            HttpsURLConnection.setDefaultHostnameVerifier((hostname, session) -> true);


            String minioServerUrl = getValue(MINIO_PUBLIC_SERVER_URL, MINIO_SERVER_URL);
            URL url = new URL(minioServerUrl + "/maxmind/GeoLite2-City.mmdb");
            URLConnection connection = url.openConnection();
            InputStream in = connection.getInputStream();
            // Initialize the reader
            databaseReader = new DatabaseReader
                    .Builder(in)
                    .fileMode(Reader.FileMode.MEMORY)
                    .withCache(new CHMCache())
                    .build();

        } catch (IOException | NullPointerException | NoSuchAlgorithmException | KeyManagementException e) {
            e.printStackTrace();
        }

    }

    public Body getCityResponse(String ipAddress) throws IOException, GeoIp2Exception {
        InetAddress ip = InetAddress.getByName(ipAddress);
        CityResponse cityResponse = databaseReader.city(ip);
        Body result = new Body();
        if (nonNull(cityResponse) && nonNull(cityResponse.getCity())) {

            String continent = (cityResponse.getContinent() != null) ? cityResponse.getContinent().getName() : UNKNOWN;
            String country = (cityResponse.getCountry() != null) ? cityResponse.getCountry().getName() : UNKNOWN;

            result.setCity(cityResponse.getCity().getName());
            result.setCountry(country);
            result.setContinent(continent);
            result.setLatitude(cityResponse.getLocation() != null ? cityResponse.getLocation().getLatitude() : 0);
            result.setLongitude(cityResponse.getLocation() != null ? cityResponse.getLocation().getLongitude() : 0);
            result.setIpAddress(ipAddress);
            result.setZoneId(cityResponse.getLocation() != null ? cityResponse.getLocation().getTimeZone() : String.valueOf(ZoneId.systemDefault()));

            return result;
        }

        result.setCity(UNKNOWN);
        result.setCountry(UNKNOWN);
        result.setContinent(UNKNOWN);
        result.setLatitude(0);
        result.setLongitude(0);
        result.setIpAddress(ipAddress);
        result.setZoneId(String.valueOf(ZoneId.systemDefault()));
        return result;
    }

    public static String getValue(String key) {
        return System.getenv(key);
    }

    public static String getValue(String key, String defaultValue) {
        String value = getValue(key);
        if (value == null || value.isEmpty())
            return defaultValue;

        return value;
    }
}
