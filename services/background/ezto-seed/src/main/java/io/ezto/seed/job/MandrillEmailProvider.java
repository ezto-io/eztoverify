package io.ezto.seed.job;

import io.ezto.bg.core.IEmailProvider;
import io.ezto.bg.core.IEmailTemplate;
import io.ezto.bg.core.models.GeneralResponse;

import java.util.Map;

public class MandrillEmailProvider implements IEmailProvider {
    @Override
    public String getProviderId() {
        return "mandrill";
    }

    @Override
    public GeneralResponse send(IEmailTemplate emailTemplate, Map<String, String> config) {
        return null;
    }
}
