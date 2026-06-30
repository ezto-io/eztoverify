package io.ezto.extensions.job;

import io.ezto.bg.core.IJobProvider;
import org.jobrunr.jobs.context.JobContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DefaultJobProvider implements IJobProvider {

    private static final Logger LOG = LoggerFactory.getLogger(DefaultJobProvider.class);

    @Override
    public String getProviderId() {
        return "default";
    }

    @Override
    public void doExecute(Map<String, Object> taskRequest, JobContext jobContext) {
        jobContext.saveMetadata("test","test");
        LOG.info("Job Request is working fine");
    }

    @Override
    public List<String> validateRequest(Map<String, Object> taskRequest) {
        return new ArrayList<>();
    }

}
