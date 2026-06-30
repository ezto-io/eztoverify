package com.qlikverify.services.backgroundService;

import com.qlikverify.services.backgroundService.backgroundEmailService.BackgroundEmailRequestBody;
import com.qlikverify.services.backgroundService.backgroundHookNotifierService.BackgroundHookNotifierRequestBody;
import com.qlikverify.services.backgroundService.backgroundSyncService.BackgroundSyncCancelRequestBody;
import com.qlikverify.services.backgroundService.backgroundSyncService.BackgroundSyncRequestBody;
import com.qlikverify.services.scheduleresult.backgroundScheduledJob.BackgroundScheduledJobBody;

public interface BackgroundService {
    public void createBackgroundEmailJob(BackgroundEmailRequestBody requestBody);

    public void createBackgroundSyncJob(BackgroundSyncRequestBody requestBody, String providerId);

    public void createBackgroundTriggerJob(BackgroundJobRequestBody requestBody, String providerId);

    public void createBackgroundSyncCancelJob(BackgroundSyncCancelRequestBody requestBody, String jobId, String connectionId);

    public void createBackgroundHookNotifierJob(BackgroundHookNotifierRequestBody requestBody);

    public void createBackgroundRecurringScheduledJob(BackgroundScheduledJobBody requestBody, String providerId);

    public void createBackgroundScheduledJob(BackgroundJobRequestBody requestBody, String providerId);

    public boolean getRecurringJobStatus(String jobId);

    public boolean getJobStatus(String jobId);
}
