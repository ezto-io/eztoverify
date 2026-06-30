package io.ezto.extensions.mail;

import io.ezto.bg.core.IEmailProvider;
import io.ezto.bg.core.IEmailTemplate;
import io.ezto.bg.core.exceptions.MailValidationException;
import io.ezto.bg.core.models.Body;
import io.ezto.bg.core.models.GeneralResponse;
import io.ezto.extensions.helpers.FreemarkerTemplateProcessor;
import jakarta.mail.Address;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class SmtpImpl implements IEmailProvider {

    private static final Logger LOG = LoggerFactory.getLogger(SmtpImpl.class);

    @Override
    public String getProviderId() {
        return "smtp";
    }


    @Override
    public GeneralResponse send(IEmailTemplate emailTemplate, Map<String, String> configMap ) {
        Transport transport = null;
        try {
            boolean auth = Boolean.parseBoolean(configMap.get("ENABLE_AUTH"));


            Properties props = addPropertiestoProps(configMap, auth);



            props.setProperty("mail.smtp.timeout", "10000");
            props.setProperty("mail.smtp.connectiontimeout", "10000");

            String from = configMap.get("FROM");
            String fromDisplayName = configMap.get("FROM_DISPLAY_NAME");
            String replyTo = configMap.get("REPLY_TO");
            String replyToDisplayName = configMap.get("REPLY_TO_DISPLAY_NAME");

            Session session = Session.getInstance(props);
            session.setDebug(false);

            String htmlMessage = FreemarkerTemplateProcessor.getInstance(emailTemplate.getLocale()).processTemplate(emailTemplate.getTemplatePath(), emailTemplate.getPlaceHolders());

//            String htmlMessage;
//            if(emailTemplate.getPlaceHolders().get("htmlBody") != null) {
//                htmlMessage = FreemarkerTemplateProcessor.getInstance(emailTemplate.getLocale()).getHtmlBody(emailTemplate);
//            }
//            else
//                htmlMessage = FreemarkerTemplateProcessor.getInstance(emailTemplate.getLocale()).processTemplate(emailTemplate.getTemplatePath(), emailTemplate.getPlaceHolders());

            MimeMessage msg = new MimeMessage(session);

            if (htmlMessage != null) {
                msg.setContent(htmlMessage, "text/html");
            }


            msg.setFrom(toInternetAddress(from, fromDisplayName));

            msg.setReplyTo(new Address[]{toInternetAddress(from, fromDisplayName)});
            if (replyTo != null && !replyTo.isEmpty()) {
                msg.setReplyTo(new Address[]{toInternetAddress(replyTo, replyToDisplayName)});
            }
            if(emailTemplate.getSubject() != null){
                if(emailTemplate.getPlaceHolders().containsKey("subjectAttributes")){
                    msg.setSubject(FreemarkerTemplateProcessor.getInstance(emailTemplate.getLocale()).getSubject(emailTemplate.getSubject(), List.of(emailTemplate.getPlaceHolders().get("subjectAttributes"))).replace("QlikVerify", emailTemplate.getPlaceHolders().getOrDefault("variant", "QlikVerify")),"UTF-8");
                }
                else if(emailTemplate.getPlaceHolders().containsKey("tenant")){
                    msg.setSubject(FreemarkerTemplateProcessor.getInstance(emailTemplate.getLocale()).getSubject(emailTemplate.getSubject(), List.of(emailTemplate.getPlaceHolders().get("tenant"))).replace("QlikVerify", emailTemplate.getPlaceHolders().getOrDefault("variant", "QlikVerify")),"UTF-8");
                }
                else{
                    msg.setSubject(FreemarkerTemplateProcessor.getInstance(emailTemplate.getLocale()).getSubject(emailTemplate.getSubject()).replace("QlikVerify", emailTemplate.getPlaceHolders().getOrDefault("variant", "QlikVerify")), "UTF-8");
                }
            }

            msg.setHeader("To", emailTemplate.getToEmail());
            msg.saveChanges();
            msg.setSentDate(new Date());

            transport = session.getTransport("smtp");
            if (auth) {
                transport.connect(configMap.get("USERNAME"), configMap.get("PASSWORD"));
            } else {
                transport.connect();
            }
            transport.sendMessage(msg, new InternetAddress[]{new InternetAddress(emailTemplate.getToEmail())});

            GeneralResponse response = new GeneralResponse();
            response.setStatus(200);
            return response;
        } catch (Exception e) {
            LOG.error(e.toString());

            Body responseBody = new Body();
            responseBody.setMessage(e.getMessage());

            GeneralResponse failureResponse = new GeneralResponse<>();
            failureResponse.setStatus(400);
            failureResponse.setBody(responseBody);
            return failureResponse;
        } finally {
            if (transport != null) {
                try {
                    transport.close();
                } catch (   MessagingException e) {
                    LOG.warn("Failed to close transport", e);
                }
            }
        }
    }

    public Properties addPropertiestoProps(Map<String, String> configMap, boolean auth){

        Properties props = new Properties();

        boolean ssl = Boolean.parseBoolean(configMap.get("ENABLE_SSL"));
        boolean starttls = Boolean.parseBoolean(configMap.get("ENABLE_STARTTLS"));

        if (configMap.containsKey("HOST")) {
            props.setProperty("mail.smtp.host", configMap.get("HOST"));
        }

        if (configMap.containsKey("PORT") && configMap.get("PORT") != null) {
            props.setProperty("mail.smtp.port", configMap.get("PORT"));
        }

        if (auth) {
            props.setProperty("mail.smtp.auth", "true");
        }

        if (starttls) {
            props.setProperty("mail.smtp.starttls.enable", "true");
        }

        if (ssl || starttls) {
            props.put("mail.smtp.socketFactory.class",
                    "javax.net.ssl.SSLSocketFactory");
        }

        return props;
    }

    protected InternetAddress toInternetAddress(String email, String displayName) throws UnsupportedEncodingException, MailValidationException, AddressException, AddressException {
        if (email == null || "".equals(email.trim())) {

            Body responseBody = new Body();
            responseBody.setMessage("Provided email is invalid");

            GeneralResponse failureResponse = new GeneralResponse<>();
            failureResponse.setStatus(400);
            failureResponse.setBody(responseBody);
            throw new MailValidationException(failureResponse);
        }
        if (displayName == null || "".equals(displayName.trim())) {
            return new InternetAddress(email);
        }
        return new InternetAddress(email, displayName, "utf-8");
    }
}
