package io.ezto.license.job.models;

import lombok.Data;

import java.util.Map;

@Data
public class SlackPayload {
    private String title;
    private Map<String,Object> params;
}
