package io.ezto.email.job;

import io.ezto.bg.core.IEmailProvider;
import io.ezto.bg.core.IEmailTemplate;
import io.ezto.bg.core.IJobProvider;
import io.ezto.bg.core.exceptions.MailValidationException;
import io.ezto.bg.core.models.GeneralResponse;
import io.ezto.core.EztoEmailProvider;
import io.ezto.helpers.BrandingHelper;
import io.ezto.helpers.NotificationHelper;
import io.ezto.helpers.notification.enums.NotificationType;
import io.ezto.helpers.notification.models.Notification;
import org.jobrunr.jobs.context.JobContext;
import org.jobrunr.jobs.context.JobRunrDashboardLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static io.ezto.email.utils.Constants.*;
import static io.ezto.helpers.GeneralHelper.getValueFromConfig;

public class EmailJobProvider implements IJobProvider {
    private static final String PROVIDER = "provider";
    private static final String TEMPLATE = "template";
    private static final String LOCALE = "locale";
    private static final String TO_EMAIL = "to";
    private static final String SUBJECT = "subject";
    private static final String TEMPLATE_ATTRIBUTES = "template_attributes";
    private static final String PROVIDER_ATTRIBUTES = "provider_attributes";

    Logger log = new JobRunrDashboardLogger(LoggerFactory.getLogger(EmailJobProvider.class));

    private final NotificationHelper notificationHelper = new NotificationHelper();

    @Override
    public String getProviderId() {
        return "email";
    }

    @Override
    public void doExecute(Map<String, Object> taskRequest, JobContext jobContext) {
        try {
            String provider = (String) getValueFromConfig(taskRequest, PROVIDER);
            String template = (String) getValueFromConfig(taskRequest, TEMPLATE);
            String locale = (String) getValueFromConfig(taskRequest, LOCALE);
            IEmailProvider emailProvider = EztoEmailProvider.getEmailProvider(provider);
            IEmailTemplate emailTemplate = EztoEmailProvider.getEmailTemplate(template).clone();

            Map<String, String> templateAttributes = (Map<String, String>) getValueFromConfig(taskRequest, TEMPLATE_ATTRIBUTES);
            String to = (String) getValueFromConfig((Map<String, Object>) getValueFromConfig(taskRequest, TEMPLATE_ATTRIBUTES), "to");

            templateAttributes.put(TO_EMAIL, to);
            emailTemplate.setSubject((String) getValueFromConfig(taskRequest, SUBJECT));
            emailTemplate.setToEmail(to);
            if (locale != null) {
                emailTemplate.setLocale(new Locale.Builder().setLanguage(locale).build());
            } else  {
                emailTemplate.setLocale(Locale.ENGLISH);
            }
            templateAttributes.putAll(BrandingHelper.getBrandingDetails(templateAttributes.get("mode")));
            emailTemplate.setPlaceHolders(templateAttributes);

            Map<String, String> providerAttributes = (Map<String, String>) getValueFromConfig(taskRequest, PROVIDER_ATTRIBUTES);
            GeneralResponse response = emailProvider.send(emailTemplate, providerAttributes);

            if (response.getStatus() != 400 && response.getStatus() != 500) {
                String isAdminEvent = providerAttributes.get(ADMIN_EVENT) != null ? providerAttributes.get(ADMIN_EVENT) : "false";
                String email = getEmailFromProviderAttributes(providerAttributes, emailTemplate);

                String clientName = emailTemplate.getPlaceHolders().get(CLIENT_NAME) != null ? emailTemplate.getPlaceHolders().get(CLIENT_NAME) : null;
                notificationHelper.sendNotification(
                        new Notification(
                                200,
                                "Mail sent successfully",
                                providerAttributes.get("realm"),
                                email,
                                NotificationType.NOTIFICATION,
                                isAdminEvent,
                                clientName
                        ), providerAttributes.get("actionToken")
                );
            } else if (response.getStatus() == 400) {
                String isAdminEvent = providerAttributes.get(ADMIN_EVENT) != null ? providerAttributes.get(ADMIN_EVENT) : "false";
                String email = getEmailFromProviderAttributes(providerAttributes, emailTemplate);
                String clientName = emailTemplate.getPlaceHolders().get(CLIENT_NAME) != null ? emailTemplate.getPlaceHolders().get(CLIENT_NAME) : null;
                notificationHelper.sendNotification(
                        new Notification(
                                500,
                                String.valueOf(response.getBody()),
                                providerAttributes.get("realm"),
                                email,
                                NotificationType.NOTIFICATION_ERROR,
                                isAdminEvent,
                                clientName
                        ), providerAttributes.get("actionToken")
                );

            }
            if (response.getBody() != null) {
                log.info("-------Email response-------");
                log.info(response.toString());
                log.info("-------Email response-------");
            }
        } catch (MailValidationException e) {
            throw new RuntimeException(e);
        }
    }

    public String getEmailFromProviderAttributes(Map<String, String> providerAttributes, IEmailTemplate emailTemplate) {
        String email = null;
        if (providerAttributes.get(USER_ID) != null) {
            email = providerAttributes.get(USER_ID);
        } else {
            email = emailTemplate.getToEmail();
        }
        return email;
    }

    @Override
    public List<String> validateRequest(Map<String, Object> taskRequest) {
        List<String> errors = new ArrayList<>();

        String templateId = (String) getValueFromConfig(taskRequest, TEMPLATE);
        if (templateId == null || templateId.isEmpty()) {
            errors.add(TEMPLATE + " property is mandatory");
            return errors;
        }

        String to = (String) getValueFromConfig((Map<String, Object>) getValueFromConfig(taskRequest, TEMPLATE_ATTRIBUTES), "to");
        if (to == null || to.isEmpty()) {
            log.info("-------------------------------- to missing -----------------------------------------------------------");
            errors.add(TO_EMAIL + " property is mandatory");
            return errors;
        }

        Map<String, String> providerAttributes = (Map<String, String>) getValueFromConfig(taskRequest, PROVIDER_ATTRIBUTES);
        if (providerAttributes == null || providerAttributes.isEmpty()) {
            errors.add("provider_attributes are mandatory");
            return errors;
        }

        return errors;
    }
}
