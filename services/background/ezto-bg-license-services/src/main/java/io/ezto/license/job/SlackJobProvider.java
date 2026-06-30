package io.ezto.license.job;

import io.ezto.license.job.models.SlackPayload;
import io.ezto.bg.core.IJobProvider;
import com.slack.api.Slack;
import com.slack.api.model.block.LayoutBlock;
import com.slack.api.webhook.WebhookResponse;
import io.ezto.helpers.JsonHelper;
import org.jobrunr.jobs.context.JobContext;
import org.jobrunr.jobs.context.JobRunrDashboardLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.slack.api.model.block.Blocks.header;
import static com.slack.api.model.block.Blocks.section;
import static com.slack.api.model.block.composition.BlockCompositions.markdownText;
import static com.slack.api.model.block.composition.BlockCompositions.plainText;
import static com.slack.api.webhook.WebhookPayloads.payload;
import static io.ezto.helpers.GeneralHelper.getValueFromConfig;

public class SlackJobProvider implements IJobProvider {

    private static final String JOB_ATTRIBUTES = "job_attributes";
    private static final String PAYLOAD = "payload";
    private static final String SLACK_HOOK = "slack_hook";
    private Slack slack = Slack.getInstance();

    Logger log = new JobRunrDashboardLogger(LoggerFactory.getLogger(SlackJobProvider.class));

    @Override
    public String getProviderId() {
        return "slack";
    }

    @Override
    public void doExecute(Map<String, Object> taskRequest, JobContext jobContext) {
        Map<String, String> providerAttributes = (Map<String, String>) getValueFromConfig(taskRequest, JOB_ATTRIBUTES);
        Object payload = getValueFromConfig(taskRequest, PAYLOAD);
        SlackPayload slackPayload = JsonHelper.getInstance().getObjectMapper().convertValue(payload, SlackPayload.class);
        List<LayoutBlock> layoutBlocks = new ArrayList<>();
        layoutBlocks.add(header(head -> head.text(plainText(slackPayload.getTitle()))));
        if(slackPayload.getParams() != null) {
            slackPayload.getParams().forEach((k, v) ->
                layoutBlocks.add(section(sec -> sec.text(markdownText("*" + k + ":*\n" + v))))
            );
        }

        WebhookResponse webhookResponse = null;
        try {
            webhookResponse = slack.send(providerAttributes.get(SLACK_HOOK), payload(p -> p.text(slackPayload.getTitle()).blocks(layoutBlocks)));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        if (webhookResponse.getCode() != 200) {
            log.error(webhookResponse.getBody());
        }
        else
        {
            log.info(webhookResponse.getBody());
        }
    }

    @Override
    public List<String> validateRequest(Map<String, Object> taskRequest) {
        List<String> errors = new ArrayList<>();
        Map<String, String> jobAttributes = (Map<String, String>) getValueFromConfig(taskRequest, JOB_ATTRIBUTES);
        if (jobAttributes == null || jobAttributes.size() == 0) {
            errors.add(JOB_ATTRIBUTES+" are mandatory");
            return errors;
        }

        if(!jobAttributes.containsKey(SLACK_HOOK))
        {
            errors.add(SLACK_HOOK+" is mandatory");
            return errors;
        }

        String slackUrl = jobAttributes.get(SLACK_HOOK);
        if(slackUrl == null ||  slackUrl.isEmpty())
        {
            errors.add(SLACK_HOOK+" is mandatory");
            return errors;
        }

        Object payload = getValueFromConfig(taskRequest, PAYLOAD);
        if (payload == null) {
            errors.add(PAYLOAD+" are mandatory");
            return errors;
        }

        SlackPayload slackPayload = JsonHelper.getInstance().getObjectMapper().convertValue(payload, SlackPayload.class);
        if (slackPayload == null) {
            errors.add("invalid payload");
            return errors;
        }

        if(slackPayload.getTitle() == null || slackPayload.getTitle().isEmpty())
        {
            errors.add("title is mandatory");
            return errors;
        }

        if(slackPayload.getParams() == null || slackPayload.getParams().isEmpty())
        {
            errors.add("params are mandatory");
            return errors;
        }

        return errors;
    }
}
