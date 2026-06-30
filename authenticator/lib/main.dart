import 'dart:io';
import 'package:eztoverify/enums/push_support.dart';
import 'package:eztoverify/ezto_verify_app.dart';
import 'package:eztoverify/model/force_update_model.dart';
import 'package:eztoverify/model/get_token_model.dart';
import 'package:eztoverify_getting_started/constants/image_constants.dart';
import 'package:eztoverify_getting_started/core/locator/locator.dart';
import 'package:eztoverify_getting_started/core/scanQrBloc/bloc/scan_qr_bloc.dart';
import 'package:eztoverify_getting_started/core/services/permissions_service/permission_service.dart';
import 'package:eztoverify_getting_started/core/services/notification_service/push_notification_service.dart';
import 'package:eztoverify_getting_started/core/services/notification_service/push_socket_service.dart';
import 'package:eztoverify_getting_started/core/services/storage_service/secure_storage_service.dart';
import 'package:eztoverify_getting_started/generated/flavor/generated_constants.g.dart';
import 'package:eztoverify_getting_started/generated/flavor/url_generated.g.dart';
import 'package:firebase_core/firebase_core.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:flutter_localizations/flutter_localizations.dart';
import 'package:window_manager/window_manager.dart';
import 'feature/about/Ui/screens/about_page.dart';
import 'feature/home/Ui/screens/home_page.dart';
import 'generated/l10n.dart';
import 'package:tray_manager/tray_manager.dart';

Future<void> main(List<String> flavor) async {
  WidgetsFlutterBinding.ensureInitialized();
  await setupServiceLocator();
  await setUpFirebase();
  await handleDesktopConfigurations();

  runApp(const MyApp());
}

Future<void> setUpFirebase() async {
  if (Platform.isAndroid || Platform.isIOS) {
    await Firebase.initializeApp();
    // if (Platform.isAndroid) await Push.isAutoInitEnabled();
    final pushNotificationService = PushNotificationService.I;
    pushNotificationService.requestPermission();
    pushNotificationService.backgroundNotification();
  }
}

Future<void> handleDesktopConfigurations() async {
  if (Platform.isMacOS || Platform.isWindows || Platform.isLinux) {
    await windowManager.ensureInitialized();
    WindowOptions windowOptions = const WindowOptions(
      center: true,
      size: Size(830, 700),
      minimumSize: Size(830, 700),
    );
    windowManager.waitUntilReadyToShow(windowOptions, () async {
      await windowManager.show();
      await windowManager.focus();
    });
    initTrayManager();
  }
}

Future<void> initTrayManager() async {
  await trayManager.setIcon(
    ImageConstants.logoFullSvg,
  );
  Menu menu = Menu(
    items: [
      MenuItem(
        key: 'open_app',
        label: 'Open App',
      ),
      MenuItem.separator(),
      MenuItem(
        key: 'hide_app',
        label: 'Hide App',
      ),
      MenuItem.separator(),
      MenuItem(
        key: 'exit_app',
        label: 'Exit App',
      ),
    ],
  );
  await trayManager.setContextMenu(menu);
}

class MyApp extends StatefulWidget {
  const MyApp({super.key});

  @override
  State<MyApp> createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> with WindowListener, TrayListener {
  final PushSocketService _pushSocketService = PushSocketService();
  String? updateUrl = '';

  @override
  void initState() {
    super.initState();
    if (Platform.isAndroid || Platform.isIOS) {
      PushNotificationService.I.init();
    } else {
      trayManager.addListener(this);
    }
  }

  @override
  Widget build(BuildContext context) {
    if (Platform.isAndroid) {
      List<DeviceOrientation> orientations =
          MediaQuery.of(context).size.shortestSide <= 600
              ? [DeviceOrientation.portraitUp]
              : [
                  DeviceOrientation.portraitUp,
                  DeviceOrientation.portraitDown,
                  DeviceOrientation.landscapeLeft,
                  DeviceOrientation.landscapeRight
                ];
      SystemChrome.setPreferredOrientations(orientations);
    }
    if (GeneratedConstants().isProd) {
      updateUrl = '';
    } else {
      if (Platform.isMacOS) {
        updateUrl = UrlConstants().macUpdate;
      } else if (Platform.isWindows) {
        updateUrl = UrlConstants().windowsUpdate;
      }
    }
    return EztoVerifyApp(
      forceUpdate: ForceUpdate(
        android: const UpgradeConfig(shouldForceUpdate: true),
        ios: const UpgradeConfig(shouldForceUpdate: true),
        mac: UpgradeConfig(
          shouldForceUpdate: true,
          url: updateUrl,
        ),
        windows: UpgradeConfig(
          shouldForceUpdate: true,
          url: updateUrl,
        ),
      ),
      onPermissionDenied: (deniedPermission) async {
        debugPrint("Please enable required permissions");

        final permissionToRequest =
            PermissionService().statusMapperErrorPermissionTo(deniedPermission);
        return PermissionService()
            .requestPermission(permissionToRequest: permissionToRequest);
      },
      onResult: (String reqId, String authReqId) async {
        //This will be called if the Result Hook notification way in
        //ezto verfiy dashboard is set to Mobile_Sdk
        //Only called if the authentication flow is completed
        //successfully
        if (GeneratedConstants().isCopyAuthCode) {
          await Clipboard.setData(ClipboardData(text: authReqId));
        }
      },
      onPushTokenRequest: () async {
        if (Platform.isAndroid || Platform.isIOS) {
          final token = await PushNotificationService.I.getToken();
          return GetTokenModel(
            pushToken: token!,
            pushType: PushSupport.firebase,
          );
        } else {
          return GetTokenModel(
            pushToken: '',
            pushType: PushSupport.firebase,
          );
        }
      },
      onClose: (reason) {
        if (!reason.name.toLowerCase().contains('completed')) {
          debugPrint("Transaction Closed : $reason");
        }
      },
      onError: (reason) {
        debugPrint("Transaction Failed : $reason");
      },
      onSocketPushUrlRequest: GeneratedConstants().isProd
          ? null
          : () async {
              var socketUrl = await StorageService().getSocketUrl();
              if (socketUrl != null && socketUrl.trim() != "") {
                return socketUrl;
              }
              return "{your_socket_url}";
            },
      onTransactionRequest: (data, isFromDeeplink) async {
        if (isFromDeeplink) {
          _pushSocketService.showTransactionFromDeeplink(data);
        } else {
          _pushSocketService.showNotification(
            id: 1,
            payload: data,
            title: data["title"],
            body: data["body"],
          );
        }
      },
      onFileDownloadStart: (fileName, mimeType) {
        // The SDK throws the fileName and mimeType when any file download starts.
      },
      onFileDownloadEnd: (fileName, mimeType, path) {
        // The SDK throws the fileName, mimeType and path when any file download ends.
      },
      child: MultiBlocProvider(
        providers: [
          BlocProvider(
            create: (context) => ScanQrBloc(),
          )
        ],
        child: MaterialApp(
          debugShowCheckedModeBanner: !GeneratedConstants().isProd,
          localizationsDelegates: const [
            S.delegate,
            GlobalMaterialLocalizations.delegate,
            GlobalCupertinoLocalizations.delegate,
            GlobalWidgetsLocalizations.delegate
          ],
          supportedLocales: S.delegate.supportedLocales,
          routes: {
            '/home': (context) => const HomePage(),
            '/about': (context) => const AboutPage(),
          },
          initialRoute: '/home',
        ),
      ),
    );
  }

  @override
  void onTrayIconMouseDown() {
    // to open the tray bar
    trayManager.popUpContextMenu();
  }

  @override
  void onTrayMenuItemClick(MenuItem menuItem) {
    if (menuItem.key == 'open_app') {
      windowManager.show();
    } else if (menuItem.key == 'hide_app') {
      windowManager.hide();
    } else if (menuItem.key == 'exit_app') {
      SystemNavigator.pop();
    }
  }

  @override
  void dispose() {
    trayManager.removeListener(this);
    super.dispose();
  }
}
