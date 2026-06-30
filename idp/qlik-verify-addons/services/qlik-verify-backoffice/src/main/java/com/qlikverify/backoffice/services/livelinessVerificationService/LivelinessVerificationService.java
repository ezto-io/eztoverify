package com.qlikverify.backoffice.services.livelinessVerificationService;

import java.io.File;
import java.util.Map;

public interface LivelinessVerificationService {
    Map<String, Object> generateFaceTemplate(String imageData, String urlPath, String dataToBeSent);
    Map<String, Object> verifyLiveliness(String urlPath, String dataForRequestBody);
}
