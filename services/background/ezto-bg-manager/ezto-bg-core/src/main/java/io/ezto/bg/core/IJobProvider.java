package io.ezto.bg.core;

import org.jobrunr.jobs.context.JobContext;

import java.util.List;
import java.util.Map;

public interface IJobProvider {

    String getProviderId();

    void doExecute(Map<String,Object> taskRequest, JobContext jobContext);

    List<String> validateRequest(Map<String,Object> taskRequest);
}
