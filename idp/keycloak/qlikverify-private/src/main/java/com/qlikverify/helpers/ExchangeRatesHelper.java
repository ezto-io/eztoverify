package com.qlikverify.helpers;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qlikverify.exchangeRates.spi.ExchangeRatesService;
import org.apache.http.util.TextUtils;
import org.jboss.logging.Logger;
import org.keycloak.EnvironmentReader;
import org.keycloak.broker.provider.util.SimpleHttp;
import org.keycloak.models.KeycloakSession;

import java.io.IOException;
import java.util.Map;

public class ExchangeRatesHelper {
    private static final Logger logger = Logger.getLogger(ExchangeRatesHelper.class);

    public static void getUpdatedExchangeRates(KeycloakSession session) {
        String exchangeRateApiKey = EnvironmentReader.getValue("EXCHANGE_RATE_API_KEY");
        if (TextUtils.isBlank(exchangeRateApiKey)) {
            logger.error("ExchangeRateHelper::getUpdatedExchangeRates:: API key is missing.");
            return;
        }

        String url = String.format("https://v6.exchangerate-api.com/v6/%s/latest/USD", exchangeRateApiKey);
        try {
            SimpleHttp.Response response = SimpleHttp.doGet(url, session).asResponse();
            String responseJson = response.asString();
            logger.info("ExchangeRateHelper::getUpdatedExchangeRates:: Response received: " + responseJson);

            JsonNode rootNode = new ObjectMapper().readTree(responseJson);
            String result = rootNode.path("result").asText();

            if ("success".equalsIgnoreCase(result)) {
                logger.info("ExchangeRateHelper::getUpdatedExchangeRates:: Exchange rates updated successfully.");
                JsonNode conversionRates = rootNode.path("conversion_rates");
                ObjectMapper mapper = new ObjectMapper();
                Map<String, Double> exchangeRates = mapper.convertValue(conversionRates, new TypeReference<Map<String, Double>>() {
                });
                ExchangeRatesService exchangeRatesService = session.getProvider(ExchangeRatesService.class);
                if (exchangeRatesService != null) {
                    exchangeRatesService.updateRates(exchangeRates);
                }
            } else {
                logger.error("ExchangeRateHelper::getUpdatedExchangeRates:: Error in response: " + responseJson);
            }
        } catch (IOException e) {
            logger.error("ExchangeRateHelper::getUpdatedExchangeRates:: Failed to fetch exchange rates. ", e);
            throw new RuntimeException(e);
        }
    }
}

