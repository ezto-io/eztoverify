import 'package:eztoverify/model/custom_scheme_model.dart';
import 'package:package_info_plus/package_info_plus.dart';

const allowedSchemesInWebview = ["uaepass", "uaepassstg"];

class CustomSchemeService {
  Future<CustomSchemeModel?> preprocessUri(Uri? uri) async {
    if (uri == null) return null;

    bool schemeMatches = allowedSchemesInWebview
        .any((element) => uri.toString().startsWith(element));
    if (schemeMatches == false) return null;

    final scheme = uri.scheme;
    final PackageInfo packageInfo = await PackageInfo.fromPlatform();

    final sdkAppScheme = packageInfo.packageName;

    CustomSchemeModel? customSchemeModel;

    switch (scheme) {
      case "uaepass":
      case "uaepassstg":
        final initialUri = uri;
        final successLink = uri.queryParameters["successurl"];
        final failureLink = uri.queryParameters["failureurl"];
        if (successLink == null || failureLink == null) return null;

        final sdkAppCallbackUrl = Uri(scheme: sdkAppScheme, path: "uaepass");

        Uri appToAppUri = uri.replace(
          //this scheme check is to mitigate a bug in uaepass redirection
          scheme: uri.toString().contains("relyingParty%3Dsandbox_stage")
              ? "uaepassstg"
              : "uaepass",
          queryParameters: {
            ...uri.queryParameters,
            "successurl":
                "$sdkAppCallbackUrl/success?url=${Uri.encodeComponent(successLink)}",
            "failureurl":
                "$sdkAppCallbackUrl/failure?url=${Uri.encodeComponent(failureLink)}"
          },
        );

        customSchemeModel = CustomSchemeModel(
          initialUri: initialUri,
          successLink: successLink,
          failureLink: failureLink,
          appToAppUri: appToAppUri,
          scheme: scheme,
        );
        break;
      default:
    }

    return customSchemeModel;
  }

  Uri? postProcessUri(CustomSchemeModel customModelScheme, String url) {
    final uri = Uri.parse(url);
    final pathSegments = uri.pathSegments;
    if (pathSegments.isEmpty) return null;
    final identifier = pathSegments.first;
    switch (identifier) {
      case "uaepass":
        final result = uri.queryParameters["url"];
        if (result == null) return null;
        return Uri.parse(Uri.decodeComponent(result));
      default:
    }
    return uri;
  }
}
