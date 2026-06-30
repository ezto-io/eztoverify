abstract class BaseAppSigning {
  abstract final String appTeamId;
  //To generate iosProvisioningProfileId
  //1. Generate certificate - Apple Distribution
  //2. Generate provisioning profile
  //3. Install both on mac
  //4. Run `security cms -D -i  '/path-to-profile/ezto_verify_demo.mobileprovision'` to find provisioning profile UUID
  abstract final String iosProvisioningProfileId;
  abstract final String macOsProvisioningProfileName;
}
