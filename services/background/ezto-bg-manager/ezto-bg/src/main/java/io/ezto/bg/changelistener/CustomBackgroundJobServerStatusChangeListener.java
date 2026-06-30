package io.ezto.bg.changelistener;

import io.ezto.bg.helpers.EnvironmentReader;
import jakarta.annotation.PostConstruct;
import org.jobrunr.configuration.JobRunr;
import org.jobrunr.storage.BackgroundJobServerStatus;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.storage.listeners.BackgroundJobServerStatusChangeListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Component
public class CustomBackgroundJobServerStatusChangeListener implements BackgroundJobServerStatusChangeListener {

    @Autowired
    StorageProvider storageProvider;

    @Autowired
    ClassLoader classLoader;

    @PostConstruct
    public void init(){
        storageProvider.addJobStorageOnChangeListener(this);
    }


    /**
     * BG Job server fails randomly and fails to run any enqueued or scheduled tasks even if the dashboard shows nothing wrong
     * The only way to find is via the heart-beat time updated by the BG job server every 5 seconds
     * Here if the difference between last heart-beat time and current time is greater than 3 minutes we send status as false
     * This change listener will help to restart bg job server of jobrunr of current instance which seems the only way for now for open-source version of JOB Runner
     * This listener will be called by storage provider every 5 seconds.
     */
    @Override
    public void onChange(List<BackgroundJobServerStatus> backgroundJobServers) {
        int bgJobServerLastHeartBeatDiffTime = Integer.parseInt(EnvironmentReader.getValue("BG_JOB_SERVER_LAST_HEART_BEAT_DIFF_TIME", "3"));
        UUID id = JobRunr.getBackgroundJobServer().getConfiguration().getId();//WHY: to get current backgroundServer's ID
        backgroundJobServers.stream().filter(backgroundJobServerStatus -> backgroundJobServerStatus.getId().equals(id))
                .findFirst()
                .ifPresentOrElse(backgroundJobServerStatus -> stopAndThenStartBgJobServer(backgroundJobServerStatus, bgJobServerLastHeartBeatDiffTime), () -> {
                    Thread.currentThread().setContextClassLoader(classLoader);
                    JobRunr.getBackgroundJobServer().start();
                });
    }

    private void stopAndThenStartBgJobServer(BackgroundJobServerStatus backgroundJobServerStatus, int bgJobServerLastHeartBeatDiffTime) {
        Instant currentTime = Instant.now();
        if (Duration.between(backgroundJobServerStatus.getLastHeartbeat(), currentTime).toMinutes() >= bgJobServerLastHeartBeatDiffTime) {
            JobRunr.getBackgroundJobServer().stop();
            Thread.currentThread().setContextClassLoader(classLoader);
            JobRunr.getBackgroundJobServer().start();
        }
    }
}
