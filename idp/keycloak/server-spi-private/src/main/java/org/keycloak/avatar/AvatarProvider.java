package org.keycloak.avatar;

import io.minio.GetObjectResponse;
import org.keycloak.provider.Provider;

import java.io.InputStream;

public interface AvatarProvider extends Provider {

    void saveAvatarImage(String realmName, String userId, InputStream input);

    InputStream loadAvatarImage(String realmId, String userId);

    boolean removeAvatarImage(String realmId, String userId);

    boolean removeRealmData(String realmId);

    void saveEmailTemplate(String realmName,String templateName, InputStream input);

    InputStream loadEmailTemplate(String realmName,String templateName);

    String saveImgAndReturnUrl(String realmName,String objectPath,String fileName,String fileType,InputStream input);

    boolean removeBrandData(String realmName,String brandName);

    GetObjectResponse getObject(String objectPath, String realmName);

    void saveFile(String realmName,String templateName, InputStream inputStream);

    InputStream loadFile(String realmName,String templateName);

}
