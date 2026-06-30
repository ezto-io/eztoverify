package io.ezto.bg.controllers;

import io.ezto.bg.core.IJobProvider;
import io.ezto.bg.core.models.GeneralResponse;
import io.ezto.bg.utils.Constants;
import io.ezto.core.EztoJobProvider;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.coyote.BadRequestException;
import org.jobrunr.jobs.Job;
import org.jobrunr.jobs.JobId;
import org.jobrunr.jobs.RecurringJob;
import org.jobrunr.jobs.context.JobContext;
import org.jobrunr.scheduling.BackgroundJob;
import org.jobrunr.scheduling.JobScheduler;
import org.jobrunr.storage.StorageProvider;
import org.jobrunr.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import static org.jobrunr.scheduling.RecurringJobBuilder.aRecurringJob;

@Tag(name = "Job", description = "Rest Api to trigger jobs")
@RestController()
@RequestMapping("/jobs")
public class JobController {

    @Autowired
    EztoJobProvider eztoJobProvider;

    @Autowired
    JobScheduler jobScheduler;

    @Autowired
    StorageProvider storageProvider;


    private static final Logger LOG = LoggerFactory.getLogger(JobController.class);


    @PostMapping("/{providerId}/queue")
    public GeneralResponse subscribe(@PathVariable String providerId, @RequestBody Map<String, Object> request) throws Exception {
        if (providerId == null)
            throw new Exception(Constants.PROVIDER_ID_MANDATORY);

        IJobProvider jobProvider = eztoJobProvider.getJob(providerId);
        if (jobProvider == null)
            throw new Exception(Constants.INVALID_PROVIDER_ID);

        List<String> validationResults = jobProvider.validateRequest(request);
        if(!validationResults.isEmpty()){
            GeneralResponse response = new GeneralResponse<>();
            response.setStatus(400);
            response.setBody(validationResults);
            return response;
        }



        final JobId enqueuedJobId = jobScheduler.enqueue(() -> jobProvider.doExecute(request, JobContext.Null));

        GeneralResponse response = new GeneralResponse<>();
        response.setStatus(200);
        response.setBody(enqueuedJobId.toString());
        return response;
    }

    @PostMapping("/{providerId}/batch/queue")
    public GeneralResponse doBatchEnqueue(@PathVariable String providerId, @RequestBody List<HashMap<String, Object>> requests) throws Exception {
        if (providerId == null)
            throw new Exception(Constants.PROVIDER_ID_MANDATORY);

        IJobProvider jobProvider = eztoJobProvider.getJob(providerId);
        if (jobProvider == null)
            throw new Exception(Constants.INVALID_PROVIDER_ID);

        if(requests.size() > 100) {
            Map<String, Object> responseBody = new HashMap();
            responseBody.put("success", false);
            responseBody.put("data", "Request data length exceeds limit 100");

            GeneralResponse response = new GeneralResponse();
            response.setStatus(400);
            response.setBody(responseBody);
            return response;
        }

        List<Map<String, Object>> responseData = new ArrayList<>();
        requests.forEach(request -> {
            List<String> validationResults = jobProvider.validateRequest(request);

            Map<String, Object> response = new HashMap<>();
            response.put("id", request.get("ID"));

            if (!validationResults.isEmpty()) {
                response.put(Constants.ENQUEUED, false);
                response.put("error", validationResults);
                responseData.add(response);
                return;
            }

            try {
                final JobId enqueuedJobId = jobScheduler.enqueue(() -> jobProvider.doExecute(request, JobContext.Null));
                response.put(Constants.ENQUEUED, true);
                response.put("data", enqueuedJobId.toString());
            } catch (Exception ex) {
                response.put(Constants.ENQUEUED, false);
                response.put("error", ex.getMessage());
            }
            responseData.add(response);
        }
        );

        Map<String, Object> responseBody = new HashMap();
        responseBody.put("success", true);
        responseBody.put("data", responseData);

        GeneralResponse response = new GeneralResponse();
        response.setStatus(200);
        response.setBody(responseBody);
        return response;
    }

    @PostMapping("/{providerId}/scheduleJob")
    public GeneralResponse scheduleJob(@PathVariable String providerId, @RequestBody Map<String, Object> request) throws Exception {
        LOG.info("JobController :: scheduleJob");

        if (providerId == null) {
            LOG.info("JobController :: scheduleJob :: provider id is null");
            throw new Exception(Constants.PROVIDER_ID_MANDATORY);
        }

        IJobProvider jobProvider = eztoJobProvider.getJob(providerId);
        if (jobProvider == null) {
            LOG.info("JobController :: scheduleJob :: jobProvider is null");
            throw new Exception(Constants.INVALID_PROVIDER_ID);
        }

        List<String> validationResults = jobProvider.validateRequest(request);
        if(!validationResults.isEmpty()) {
            LOG.info("JobController :: scheduleJob :: " + providerId +  "RequestBody validation failed");
            GeneralResponse response = new GeneralResponse();
            response.setStatus(400);
            response.setBody(validationResults);
            return response;
        }

        JobId jobId = jobScheduler.schedule(
               getJobIdIfProvided( request.get("JOB_ID")),
                LocalDateTime.now().plusSeconds((Integer)request.get("scheduled_time")),
                () -> jobProvider.doExecute(request, JobContext.Null)
        );

        LOG.error("-----------------------------------------------");
        LOG.error("jobId : " + jobId);
        LOG.error("-----------------------------------------------");

        GeneralResponse response = new GeneralResponse();
        response.setStatus(200);
        response.setBody(jobId.toString());
        return response;
    }

    @DeleteMapping("/{jobId}/deleteScheduledJob")
    public GeneralResponse deleteScheduledJob(@PathVariable String jobId, @RequestBody Map<String, String> request){
        LOG.info("JobController :: deleteScheduledJob");
        if(StringUtils.isNullOrEmpty(jobId)){
            LOG.info("JobController :: deleteScheduledJob :: scheduled job Id to cancel deletion not found.");
            GeneralResponse response = new GeneralResponse();
            response.setStatus(400);
            response.setBody("Deletion schedule not found");
            return response;
        }
        storageProvider.deletePermanently(UUID.fromString(jobId));
        LOG.info("JobController :: deleteScheduledJob :: scheduled tenant deletion is cancelled");
        GeneralResponse response = new GeneralResponse();
        response.setStatus(200);
        response.setBody("Workspace has been deleted.");
        return response;
    }

    @PostMapping("/{providerId}/schedule")
    public GeneralResponse scheduleRecurringJob(@PathVariable String providerId, @RequestBody Map<String, Object> request) throws Exception {
        if (providerId == null)
            throw new Exception(Constants.PROVIDER_ID_MANDATORY);

        IJobProvider jobProvider = eztoJobProvider.getJob(providerId);
        if (jobProvider == null)
            throw new Exception(Constants.INVALID_PROVIDER_ID);

        List<String> validationResults = jobProvider.validateRequest(request);
        if(!validationResults.isEmpty()) {
            GeneralResponse response = new GeneralResponse();
            response.setStatus(400);
            response.setBody(validationResults);
            return response;
        }

        long jobInterval = Long.parseLong(String.valueOf(request.get("INTERVAL")));
        String jobId = (String) request.get("JOB_ID");
        final String enqueuedJobId;
        if(jobId == null) {
            enqueuedJobId = jobScheduler.createRecurrently(aRecurringJob()
                    .withDuration(Duration.ofSeconds(jobInterval))
                    .withDetails(() -> jobProvider.doExecute(request, JobContext.Null)));
        } else {
            enqueuedJobId = jobScheduler.createRecurrently(aRecurringJob()
                    .withId(jobId)
                    .withDuration(Duration.ofSeconds(jobInterval))
                    .withDetails(() -> jobProvider.doExecute(request, JobContext.Null)));
        }

        GeneralResponse response = new GeneralResponse();
        response.setStatus(200);
        response.setBody(enqueuedJobId);
        return response;
    }

    @DeleteMapping("/queue/{jobid}")
    public GeneralResponse deleteJob(@PathVariable String jobid) throws Exception {
        if (jobid == null)
            throw new Exception(Constants.JOB_ID_MANDATORY);

        BackgroundJob.deleteRecurringJob(jobid);
        GeneralResponse response = new GeneralResponse<>();
        response.setStatus(200);
        response.setBody("Job removed successfully");
        return response;
    }

    @GetMapping("/status/{jobid}")
    public GeneralResponse status(@PathVariable String jobid) throws Exception {
        if (jobid == null)
            throw new Exception(Constants.JOB_ID_MANDATORY);

        Job job = storageProvider.getJobById(UUID.fromString(jobid));
        if (job == null){
            GeneralResponse response = new GeneralResponse<>();
            response.setStatus(400);
            return response;

        }


        GeneralResponse response = new GeneralResponse<>();
        response.setStatus(200);
        response.setBody(job.getState());
        return response;
    }

    @GetMapping("/recurring/{jobId}")
    public GeneralResponse getRecurringJob(@PathVariable String jobId) throws BadRequestException {
        if (jobId == null)
            throw new BadRequestException(Constants.JOB_ID_MANDATORY);

        Optional<RecurringJob> recurringJob = storageProvider.getRecurringJobs().stream().filter(rJob -> rJob.getId().equalsIgnoreCase(jobId)).findAny();

        if (recurringJob.isPresent()) {
            GeneralResponse response = new GeneralResponse<>();
            response.setStatus(200);
            response.setBody(recurringJob.get().getId());
            return response;
        }

        GeneralResponse response = new GeneralResponse<>();
        response.setStatus(404);
        response.setBody("Recurring job not found");
        return response;
    }

    @GetMapping("/meta/{jobid}/{metakey}")
    public GeneralResponse getData(@PathVariable String jobid, @PathVariable String metakey) throws Exception {
        if (jobid == null)
            throw new Exception(Constants.JOB_ID_MANDATORY);

        if (metakey == null)
            throw new Exception("Metakey is mandatory");

        Job job = storageProvider.getJobById(UUID.fromString(jobid));
        if (job == null){
            GeneralResponse response = new GeneralResponse<>();
            response.setStatus(400);
            return response;
        }


        Map<Object, Object> responseBody = new HashMap<>();
        responseBody.put("jobStatus", job.getState());
        responseBody.put(metakey, job.getMetadata().get(metakey));

        GeneralResponse response = new GeneralResponse<>();
        response.setStatus(200);
        response.setBody(responseBody);
        return response;
    }

    private UUID getJobIdIfProvided(Object jobId) {
        if (jobId != null) {
            if (jobId instanceof String id) {
                try {
                    return UUID.fromString(id);

                } catch (IllegalArgumentException e) {
                    return UUID.randomUUID();
                }
            }
        }
        return UUID.randomUUID();

    }
}
