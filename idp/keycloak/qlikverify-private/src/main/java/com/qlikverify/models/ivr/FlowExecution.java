package com.qlikverify.models.ivr;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FlowExecution {

    private String account_sid;
    private String contact_channel_address;
    private Context context;
    private String date_created;
    private String date_updated;
    private String flow_sid;
    private Links links;
    private String sid;
    private String status;
    private String url;

    @Data
    @AllArgsConstructor
    public static class Context {
    }

    @Data
    @NoArgsConstructor
    public static class Links {
        private String execution_context;
        private String steps;

        @JsonCreator
        public Links(@JsonProperty("execution_context") String execution_context,
                     @JsonProperty("steps") String steps) {
            this.execution_context = execution_context;
            this.steps = steps;
        }
    }
}