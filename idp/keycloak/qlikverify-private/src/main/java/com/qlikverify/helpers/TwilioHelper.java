package com.qlikverify.helpers;

import com.qlikverify.exchangeRates.spi.ExchangeRatesService;
import com.qlikverify.models.TwilioMessageModel;
import com.qlikverify.models.ivr.FlowExecutionContext;
import com.qlikverify.models.ivr.TwilioCallDetails;
import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.broker.provider.util.SimpleHttp;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.SmsConfigModel;
import org.keycloak.util.JsonSerialization;

import java.io.IOException;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;

public class TwilioHelper {

    private static final Logger logger = Logger.getLogger(TwilioHelper.class);

    private static final String TWILIO_PROVIDER = "twilio";
    private static final String WHATSAPP_PROVIDER = "whatsapp";
    private static final String ACCOUNT_SID_KEY = "accountsid";
    private static final String AUTH_TOKEN_KEY = "authtoken";

    /**
     * Fetches the Twilio configuration based on the message type (SMS or WhatsApp).
     *
     * @param session     The Keycloak session.
     * @param realm       The realm from which the configuration is fetched.
     * @param messageType The type of message ("sms" or "whatsapp").
     * @return The configuration for Twilio.
     * @throws IllegalArgumentException If the configuration is missing.
     */
    private static SmsConfigModel getTwilioConfig(KeycloakSession session, RealmModel realm, String messageType) {
        SmsConfigModel config = ("sms".equalsIgnoreCase(messageType))
                ? getConfig(session, realm, TWILIO_PROVIDER, realm.isEnabledOwnSmsProvider())
                : getConfig(session, realm, WHATSAPP_PROVIDER, realm.isEnabledOwnWhatsappProvider());

        if (config == null || config.getConfig().isEmpty()) {
            throw new IllegalArgumentException("Twilio Configuration is missing.");
        }
        return config;
    }

    private static SmsConfigModel getConfig(KeycloakSession session, RealmModel realm, String providerId, boolean isEnabledOwnProvider) {
        return isEnabledOwnProvider
                ? realm.getSmsConfigByProviderId(providerId)
                : session.realms().getRealmByName(Config.getAdminRealm()).getSmsConfigByProviderId(providerId);
    }

    /**
     * Sends an HTTP request to the Twilio API.
     *
     * @param session    The Keycloak session.
     * @param accountSid The Twilio account SID.
     * @param authToken  The Twilio auth token.
     * @param url        The URL for the Twilio API endpoint.
     * @return The HTTP response from Twilio.
     * @throws RuntimeException If the request fails.
     */
    private static SimpleHttp.Response sendRequest(KeycloakSession session, String accountSid, String authToken, String url) {
        SimpleHttp.Response response = null;
        int retry = 0;
        int maxRetries = 2;
        try {
            while (retry < maxRetries) {
                response = SimpleHttp.doGet(url, session).authBasic(accountSid, authToken).asResponse();
                if (response.getStatus() == 200 && response.asJson().get("price").textValue() == null) {
                    logger.info("TwilioHelper :: sendRequest :: Retrying the request :: " + retry);
                    response = SimpleHttp.doGet(url, session).authBasic(accountSid, authToken).asResponse();
                    retry += 1;
                } else break;
            }
            return response;
        } catch (IOException e) {
            logger.error("Error sending request to Twilio: " + e.getMessage(), e);
            throw new RuntimeException("Failed to communicate with Twilio API.", e);
        }
    }

    /**
     * Parses the HTTP response from Twilio into a TwilioMessageModel.
     *
     * @param response The HTTP response from Twilio.
     * @return The parsed TwilioMessageModel.
     * @throws IOException If parsing fails.
     */
    private static TwilioMessageModel parseResponse(SimpleHttp.Response response) throws IOException {
        if (response.getStatus() != 200) {
            logger.error("Twilio API error: " + response.getStatus());
            logger.error(response.asJson());
            return null;
        }
        try {
            return JsonSerialization.readValue(response.asString(), TwilioMessageModel.class);
        } catch (IOException e) {
            logger.error("Failed to parse Twilio response: " + e.getMessage(), e);
            return null;
        }
    }

    /**
     * Fetches the details of a Twilio message.
     *
     * @param session     The Keycloak session.
     * @param realm       The realm to fetch the configuration from.
     * @param messageType The type of message ("sms" or "whatsapp").
     * @param messageId   The ID of the message to fetch.
     * @return The details of the Twilio message.
     * @throws IOException If the request or parsing fails.
     */
    public static TwilioMessageModel fetchMessageDetails(KeycloakSession session, RealmModel realm, String messageType, String messageId) throws IOException {
        SmsConfigModel config = getTwilioConfig(session, realm, messageType);
        String accountSid = config.getConfig().get(ACCOUNT_SID_KEY);
        String authToken = config.getConfig().get(AUTH_TOKEN_KEY);
        String url = String.format("https://api.twilio.com/2010-04-01/Accounts/%s/Messages/%s.json", accountSid, messageId);

        logger.info("Fetching Twilio message details for messageId: " + messageId);
        return parseResponse(sendRequest(session, accountSid, authToken, url));
    }

    /**
     * Calculates the price for a Twilio message.
     *
     * @param session        The Keycloak session.
     * @param realm          The realm for configuration.
     * @param messageType    The type of message ("sms" or "whatsapp").
     * @param messageDetails The Twilio message details.
     * @return The calculated price for the message.
     * @throws IOException If parsing or configuration fetching fails.
     */
    public static double calculateMessagePrice(KeycloakSession session, RealmModel realm, String messageType, TwilioMessageModel messageDetails) throws IOException {
        String price = messageDetails.getPrice();
        if (price == null) {
            messageDetails = fetchMessageDetails(session, realm, messageType, messageDetails.getSid());
        }
        if (messageDetails == null || messageDetails.getPrice() == null) {
            return 0D;
        }

        try {
            double actualPrice = Double.parseDouble(messageDetails.getPrice());
            if (actualPrice >= 0) return 0D;

            String subscriptionStr = realm.getSubscription();
            String currency = "USD";
            if (subscriptionStr != null && !subscriptionStr.isEmpty()) {
                try {
                    com.fasterxml.jackson.databind.JsonNode node = JsonSerialization.readValue(subscriptionStr, com.fasterxml.jackson.databind.JsonNode.class);
                    if (node.has("body") && node.get("body").has("walletCurrency")) {
                        currency = node.get("body").get("walletCurrency").asText();
                    }
                } catch (Exception e) {
                    logger.error("Failed to parse subscription currency: " + e.getMessage());
                }
            }
            Optional<Double> rate = session.getProvider(ExchangeRatesService.class).getRate(currency);
            return rate.map(r -> Math.abs(actualPrice) * r).orElse(0D);

        } catch (Exception e) {
            logger.error("Error while calculating price: " + e.getMessage(), e);
            return 0D;
        }
    }

    /**
     * Retrieves the pricing for a Twilio message based on its ID.
     *
     * @param session     The Keycloak session.
     * @param realm       The realm for configuration.
     * @param messageType The type of message ("sms" or "whatsapp").
     * @param messageId   The ID of the message.
     * @return The pricing for the message.
     */
    public static double getMessagePricing(KeycloakSession session, RealmModel realm, String messageType, String messageId) {
        TwilioMessageModel messageDetails;
        try {
            messageDetails = fetchMessageDetails(session, realm, messageType, messageId);
            return (messageDetails == null) ? 0D : calculateMessagePrice(session, realm, messageType, messageDetails);
        } catch (IOException e) {
            return 0D;
        }
    }

    /**
     * Retrieves the pricing for a Twilio call based on the execution context and authentication string.
     *
     * @param session          The Keycloak session.
     * @param executionContext The execution context URL.
     * @param authString       The Base64-encoded authentication string (account SID and auth token).
     * @return The pricing for the call.
     */
    public static double getCallPricing(KeycloakSession session, String executionContext, String authString) {
        return fetchCallDetails(session, executionContext, authString);
    }

    /**
     * Fetches the details of a Twilio call and calculates its price.
     *
     * @param session          The Keycloak session.
     * @param executionContext The execution context URL.
     * @param authString       The Base64-encoded authentication string.
     * @return The calculated price for the call.
     */
    private static double fetchCallDetails(KeycloakSession session, String executionContext, String authString) {
        String[] credentials = new String(Base64.getDecoder().decode(authString.substring("Basic ".length()))).split(":", 2);
        String accountSid = credentials[0];
        String authToken = credentials[1];

        try {
            SimpleHttp.Response response = sendRequest(session, accountSid, authToken, executionContext);
            if (response.getStatus() != 200) {
                logger.error("Failed to fetch call details: " + response.getStatus());
                return 0D;
            }

            FlowExecutionContext context = JsonSerialization.readValue(response.asString(), FlowExecutionContext.class);
            return processWidgets(session, accountSid, authToken, context);

        } catch (IOException e) {
            logger.error("Failed to fetch call details: " + e.getMessage(), e);
            return 0D;
        } catch (Exception e){
            logger.error(e.getMessage());
            logger.error(e.getStackTrace());
            return 0D;
        }
    }

    /**
     * Processes widgets in the flow execution context to fetch call details.
     *
     * @param session    The Keycloak session.
     * @param accountSid The Twilio account SID.
     * @param authToken  The Twilio auth token.
     * @param context    The flow execution context.
     * @return The calculated price for the call.
     */
    private static double processWidgets(KeycloakSession session, String accountSid, String authToken, FlowExecutionContext context) {
        try {
            if (context.getContext() == null || context.getContext().getWidgets().isEmpty()) {
                logger.info("updateIVRTransaction processWidgets - context or widgets null");
                return 0D;
            }

            Map<String, Object> widget = context.getContext().getWidgets().get("InitiateCallToUser");
            if (widget == null || !widget.containsKey("CallSid")) {
                logger.info("updateIVRTransaction processWidgets - callSid or widget is null");
                return 0D;
            }

            return calculateCallPricing(session, (String) widget.get("CallSid"), accountSid, authToken);
        } catch (Exception e){
            logger.error(e.getMessage());
            logger.error(e.getStackTrace());
            return 0D;
        }
    }

    /**
     * Calculates the price for a Twilio call.
     *
     * @param session    The Keycloak session.
     * @param callSid    The SID of the call.
     * @param accountSid The Twilio account SID.
     * @param authToken  The Twilio auth token.
     * @return The calculated price for the call.
     */
    private static double calculateCallPricing(KeycloakSession session, String callSid, String accountSid, String authToken){
        try {
            String url = String.format("https://api.twilio.com/2010-04-01/Accounts/%s/Calls/%s.json", accountSid, callSid);
            SimpleHttp.Response response = sendRequest(session, accountSid, authToken, url);

            if (response.getStatus() != 200) {
                logger.error("Failed to fetch call pricing: " + response.getStatus());
                return 0D;
            }
            TwilioCallDetails callDetails;
            callDetails = JsonSerialization.readValue(response.asString(), TwilioCallDetails.class);
            callDetails = fetchCallPricing(callDetails, session, accountSid, authToken, url, 5);
            if(callDetails == null)
                return 0D;

            String subscriptionStr = session.getContext().getRealm().getSubscription();
            String currency = "USD";
            if (subscriptionStr != null && !subscriptionStr.isEmpty()) {
                try {
                    com.fasterxml.jackson.databind.JsonNode node = JsonSerialization.readValue(subscriptionStr, com.fasterxml.jackson.databind.JsonNode.class);
                    if (node.has("body") && node.get("body").has("walletCurrency")) {
                        currency = node.get("body").get("walletCurrency").asText();
                    }
                } catch (Exception e) {
                    logger.error("Failed to parse subscription currency: " + e.getMessage());
                }
            }
            Optional<Double> rate = session.getProvider(ExchangeRatesService.class).getRate(currency);
            TwilioCallDetails finalCallDetails = callDetails;
            return rate.map(r -> Math.abs(Double.parseDouble(finalCallDetails.getPrice())) * r).orElse(0D);
        } catch (Exception e){
            logger.error(e.getMessage());
            logger.error(e.getStackTrace());
            return 0D;
        }
    }

    private static TwilioCallDetails fetchCallPricing(TwilioCallDetails callDetails, KeycloakSession session, String accountSid, String authToken, String url,
                                                Integer retries) {
        if (callDetails.getPrice() == null) {
            try {
                Thread.sleep(2000);

                SimpleHttp.Response response = sendRequest(session, accountSid, authToken, url);

                if (response.getStatus() != 200) {
                    logger.error("Failed to fetch call pricing: " + response.getStatus());
                    return null;
                }

                callDetails = JsonSerialization.readValue(response.asString(), TwilioCallDetails.class);
                return fetchCallPricing(callDetails, session, accountSid, authToken, url, retries - 1);

            } catch (IOException | InterruptedException e) {
                logger.error(e.getMessage());
                logger.error(e.getStackTrace());
                return null;
            }
        }
        return callDetails;
    }
}