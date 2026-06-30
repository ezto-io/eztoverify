package io.ezto.seed.job;

import io.ezto.bg.core.IEmailProvider;
import io.ezto.bg.core.IJobProvider;
import io.ezto.bg.core.exceptions.MailValidationException;
import io.ezto.core.EztoEmailProvider;
import org.jobrunr.jobs.context.JobContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CheckJobProvider implements IJobProvider {
    private static final Logger logger = LoggerFactory.getLogger(CheckJobProvider.class);

    @Override
    public String getProviderId() {
        return "check";
    }

    @Override
    public void doExecute(Map<String, Object> data, JobContext jobContext) {
        try {
            IEmailProvider emailProvider = EztoEmailProvider.getEmailProvider("mandrill");
            logger.info(emailProvider.getProviderId());
        } catch (MailValidationException e) {
            e.printStackTrace();
        }
        logger.info("Job Provider Trigger from external!!");
    }

    @Override
    public List<String> validateRequest(Map<String, Object> taskRequest) {
        return new ArrayList<>();
    }
}
