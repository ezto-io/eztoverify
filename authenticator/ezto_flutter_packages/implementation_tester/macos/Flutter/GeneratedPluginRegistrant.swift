//
//  Generated file. Do not edit.
//

import FlutterMacOS
import Foundation

import ezto_fido
import flutter_local_authentication
import geolocator_apple
import local_auth_darwin
import location
import package_info_plus

func RegisterGeneratedPlugins(registry: FlutterPluginRegistry) {
  FidoPlugin.register(with: registry.registrar(forPlugin: "FidoPlugin"))
  FlutterLocalAuthenticationPlugin.register(with: registry.registrar(forPlugin: "FlutterLocalAuthenticationPlugin"))
  GeolocatorPlugin.register(with: registry.registrar(forPlugin: "GeolocatorPlugin"))
  LocalAuthPlugin.register(with: registry.registrar(forPlugin: "LocalAuthPlugin"))
  LocationPlugin.register(with: registry.registrar(forPlugin: "LocationPlugin"))
  FPPPackageInfoPlusPlugin.register(with: registry.registrar(forPlugin: "FPPPackageInfoPlusPlugin"))
}
