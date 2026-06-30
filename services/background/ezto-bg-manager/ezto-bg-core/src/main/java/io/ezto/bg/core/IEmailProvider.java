package io.ezto.bg.core;


import io.ezto.bg.core.models.GeneralResponse;

import java.util.Map;

public interface IEmailProvider {

    String getProviderId();
    GeneralResponse send(IEmailTemplate emailTemplate, Map<String,String> config);
}
