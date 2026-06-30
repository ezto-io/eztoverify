package com.qlikverify.models.ivr;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FlowExecutionContext {
    private String account_sid;
    private Context context;
    private String execution_sid;
    private String flow_sid;
    private String step_sid;
    private String url;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Context {
        private Contact contact;
        private Flow flow;
        private Trigger trigger;
        private Map<String, Map<String, Object>> widgets;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Contact {
        private Channel channel;

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        public static class Channel {
            private String address;
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Flow {
        private Channel channel;
        private Data data;
        private String flow_sid;
        private String sid;

        @lombok.Data
        @NoArgsConstructor
        @AllArgsConstructor
        public static class Channel {
            private String address;
        }

        @lombok.Data
        @NoArgsConstructor
        @AllArgsConstructor
        public static class Data {
            private String ivr_purpose_message;
            private String ivr_redirect_url;
            private String ivr_sender;
            private String ivr_validation_message;
            private String username;
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Trigger {
        private Request request;

        @Data
        @NoArgsConstructor
        @AllArgsConstructor
        public static class Request {
            private String from;
            private Parameters parameters;
            private String to;

            @Data
            @NoArgsConstructor
            @AllArgsConstructor
            public static class Parameters {
                private String ivr_purpose_message;
                private String ivr_redirect_url;
                private String ivr_sender;
                private String ivr_validation_message;
                private String username;
            }
        }
    }
//
//    @Data
//    @NoArgsConstructor
//    @AllArgsConstructor
//    @JsonIgnoreProperties(ignoreUnknown = true)
//    public static class Widget {
//        private String AccountSid;
//        private String ApiVersion;
//        private String CallSid;
//        private String CallStatus;
//        private String Called;
//        private String CalledCity;
//        private String CalledCountry;
//        private String CalledState;
//        private String CalledZip;
//        private String Caller;
//        private String CallerCity;
//        private String CallerCountry;
//        private String CallerState;
//        private String CallerZip;
//        private String Direction;
//        private String From;
//        private String FromCity;
//        private String FromCountry;
//        private String FromState;
//        private String FromZip;
//        private String To;
//        private String ToCity;
//        private String ToCountry;
//        private String ToState;
//        private String ToZip;
//    }
}