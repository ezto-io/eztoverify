import 'package:branding/branding/config/base/base_app_signing.dart';

class AppSigning implements BaseAppSigning {
  @override
  String get iosProvisioningProfileId => "{iosProvisioningProfileId}";

  @override
  String get appTeamId => "{appTeamId}";

  @override
  String get macOsProvisioningProfileName => throw UnimplementedError();
}
