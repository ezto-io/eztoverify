package com.qlikverify.services.scheduleresult.backgroundScheduledJob;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.qlikverify.services.backgroundService.BackgroundJobRequestBody;
import lombok.Builder;
import lombok.Data;

import javax.annotation.Nullable;

@Data
@Builder
public class BackgroundScheduledJobBody implements BackgroundJobRequestBody {
    @Nullable
    @JsonProperty("scheduled_time")
    private Long scheduled_time;
    @Nullable
    @JsonProperty("INTERVAL")
    private Long interval;
    @JsonProperty("JOB_ID")
    private String jobId;
    private RestPayload payload;
}
