class ForceUpdate {
  final UpgradeConfig android;
  final UpgradeConfig ios;
  final UpgradeConfig mac;
  final UpgradeConfig windows;

  const ForceUpdate({
    this.android = const UpgradeConfig(shouldForceUpdate: false),
    this.ios = const UpgradeConfig(shouldForceUpdate: false),
    this.mac = const UpgradeConfig(shouldForceUpdate: false),
    this.windows = const UpgradeConfig(shouldForceUpdate: false),
  });
}

class UpgradeConfig {
  final bool shouldForceUpdate;
  final String? url;

  const UpgradeConfig({
    required this.shouldForceUpdate,
    this.url,
  });
}
