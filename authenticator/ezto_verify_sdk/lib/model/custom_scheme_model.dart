class CustomSchemeModel {
  final Uri initialUri;
  final String? successLink;
  final String? failureLink;
  final Uri appToAppUri;
  final String scheme;

  const CustomSchemeModel({
    required this.initialUri,
    required this.successLink,
    required this.failureLink,
    required this.appToAppUri,
    required this.scheme,
  });
}
