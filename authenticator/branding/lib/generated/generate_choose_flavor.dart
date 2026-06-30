// Generated file. Do not modify.

import 'package:branding/models/flavor_model.dart';
import '../branding/config/flavor/ezto_prod/index.dart' as ezto_prod;
import '../branding/config/flavor/ezto_dev/index.dart' as ezto_dev;

class FlavorSwitcher {
  static FlavorModel chooseFlavor(String key) {
    switch (key) {
      case "ezto_prod":
        return FlavorModel(
          permissions: ezto_prod.Permissions(),
          deeplinks: ezto_prod.Deeplinks(),
          appInfo: ezto_prod.Appinfo(),
          flavorName: "ezto_prod",
          theme: ezto_prod.Theme(),
          appSigning: ezto_prod.AppSigning(),
        );
      case "ezto_dev":
        return FlavorModel(
          permissions: ezto_dev.Permissions(),
          deeplinks: ezto_dev.Deeplinks(),
          appInfo: ezto_dev.Appinfo(),
          flavorName: "ezto_dev",
          theme: ezto_dev.Theme(),
          appSigning: ezto_dev.AppSigning(),
        );
      default:
        throw ArgumentError("Unknown flavor key: $key");
    }
  }
}
