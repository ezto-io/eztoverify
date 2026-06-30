import 'dart:convert';
import 'dart:io';
import 'package:device_info_plus/device_info_plus.dart';
import 'package:eztoverify/model/force_update_model.dart';
import 'package:eztoverify/model/get_token_model.dart';
import 'package:eztoverify_internal/service/push_service_interface.dart';
import 'package:eztoverify_internal/service/service_locator.dart';
import 'package:eztoverify_internal/widget/custom_button_widget.dart';
import 'package:eztoverify_internal/widget/custom_text_field_widget.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:bot_toast/bot_toast.dart';
import 'package:eztoverify/controller/ezto_verify.dart';
import 'package:eztoverify/ezto_verify_app.dart';
import 'package:eztoverify_internal/service/api_service.dart';
import 'package:fluttertoast/fluttertoast.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:package_info_plus/package_info_plus.dart';

Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();
  await initSl();
  await sl<PushServiceInterface>().init();
  runApp(const MyApp());
}

class MyApp extends StatefulWidget {
  const MyApp({super.key});

  @override
  State<MyApp> createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  final DeviceInfoPlugin deviceInfo = DeviceInfoPlugin();
  final TextEditingController textEditingController = TextEditingController();
  final TextEditingController pushTextEditingController =
      TextEditingController();
  String appVersion = "";
  // final _appAttestationPlugin = AppDeviceIntegrity();

  @override
  void initState() {
    super.initState();
    getAppVersion();
    sl<PushServiceInterface>().checkNotificationDataOnLaunch();
  }

  void getAppVersion() async {
    PackageInfo packageInfo = await PackageInfo.fromPlatform();
    appVersion = "V ${packageInfo.version}+${packageInfo.buildNumber}";
    setState(() {});
  }

  @override
  Widget build(BuildContext context) {
    return EztoVerifyApp(
      theme: ThemeData(
        progressIndicatorTheme: const ProgressIndicatorThemeData(
          color: Colors.yellow,
        ),
      ),
      forceUpdate: const ForceUpdate(
        android: UpgradeConfig(shouldForceUpdate: true),
        ios: UpgradeConfig(shouldForceUpdate: true),
        mac: UpgradeConfig(
          shouldForceUpdate: true,
          url:
              "https://your-domain.example.com/app-updates/<APPLICATION_ID>",
        ),
        windows: UpgradeConfig(
          shouldForceUpdate: true,
          url:
              "https://your-domain.example.com/app-updates/<APPLICATION_ID>",
        ),
      ),
      onPermissionDenied: (deniedPermission) async {
        BotToast.showText(
            text: "Please enable required permissions $deniedPermission");
        return true;
      },
      onSocketPushUrlRequest: () async {
        final SharedPreferences prefs = await SharedPreferences.getInstance();
        final url =
            prefs.getString("push_socket_url") ?? "https://socket.eztovrfy.com";
        pushTextEditingController.text = url;
        return url;
      },
      onResult: (String reqId, String authReqId) async {
        await Clipboard.setData(ClipboardData(text: authReqId));
        debugPrint("Auth Request ID copied to clipboard");
        BotToast.showText(text: "Auth Request ID copied to clipboard");
      },
      onPushTokenRequest: () async => sl<PushServiceInterface>().getToken(),
      onError: (reason) {
        BotToast.showText(text: "On Error Reason : $reason");
      },
      onClose: (reason) {
        debugPrint("On Closed Reason : $reason");
        BotToast.showText(text: "onClose: ${reason.name}");
      },
      onFileDownloadStart: (fileName, mimeType) {
        debugPrint("onFileDownloadStart");
        if (Platform.isIOS) {
          Fluttertoast.showToast(msg: "Downloading File");
          return;
        }
        BotToast.showText(text: "Downloading File");
      },
      onFileDownloadEnd: (fileName, mimeType, path) {
        debugPrint("onFileDownloadEnd $fileName $path");
        if (Platform.isIOS) {
          Fluttertoast.showToast(msg: "File Downloaded");
          return;
        }
        BotToast.showText(text: "File Downloaded");
      },
      onTransactionRequest: (data, isFromDeeplink) async {
        if (isFromDeeplink) {
          sl<PushServiceInterface>().showTransactionFromDeeplink(data);
        } else {
          sl<PushServiceInterface>().showNotification(
            id: 1,
            payload: data,
            title: data["title"],
            body: data["body"],
          );
        }
      },
      child: MaterialApp(
        title: 'ezto verify',
        theme: ThemeData(
          progressIndicatorTheme: ProgressIndicatorThemeData(
            color: Colors.red,
          ),
          colorScheme: ColorScheme.fromSeed(
            seedColor: Colors.deepPurple,
            surface: Colors.white,
          ),
          useMaterial3: true,
        ),
        builder: BotToastInit(),
        navigatorObservers: [BotToastNavigatorObserver()],
        home: Scaffold(
          appBar: AppBar(
            title: const Text('ezto verify'),
          ),
          bottomSheet: Container(
            color: Colors.white,
            child: Padding(
              padding: const EdgeInsets.all(8.0),
              child: Text(appVersion),
            ),
          ),
          body: bodyWidget(),
        ),
      ),
    );
  }

  Widget bodyWidget() {
    return Center(
      child: ListView(
        padding: const EdgeInsets.all(24),
        children: <Widget>[
          Row(
            children: [
              Expanded(
                  child: CustomTextField(
                controller: textEditingController,
                hintText: "Encryption key",
              )),
              CustomButtonWidget(
                buttonName: "Save",
                onPressed: () async {
                  if (textEditingController.text.isNotEmpty) {
                    final SharedPreferences prefs =
                        await SharedPreferences.getInstance();
                    await prefs.setString(
                      "push_encryption_key",
                      textEditingController.text,
                    );
                    BotToast.showText(
                      text: "Encryption key saved successfully!",
                    );
                  }
                },
              )
            ],
          ),
          const SizedBox(
            height: 16,
          ),
          Row(
            children: [
              Expanded(
                  child: CustomTextField(
                controller: pushTextEditingController,
                hintText: "Push Socket URL",
              )),
              CustomButtonWidget(
                buttonName: "Save",
                onPressed: () async {
                  final SharedPreferences prefs =
                      await SharedPreferences.getInstance();
                  await prefs.setString(
                    "push_socket_url",
                    pushTextEditingController.text,
                  );
                  BotToast.showText(
                    text: "Please restart app to continue",
                  );
                },
              )
            ],
          ),
          const SizedBox(
            height: 16,
          ),
          CustomButtonWidget(
              buttonName: kIsWeb
                  ? "Send web"
                  : (Platform.isAndroid || Platform.isIOS)
                      ? "SEND FIREBASE TOKEN"
                      : "SEND PUSH TOKEN",
              onPressed: () {
                sendToken();
              }),
          const SizedBox(
            height: 16,
          ),
          // const CustomButtonWidget(
          //   buttonName: "CAMERA AND VIDEO PERMISSION",
          //   pushUrl: "https://www.veed.io/tools/webcam-test",
          // ),
          // const CustomButtonWidget(
          //   buttonName: "MICROPHONE PERMISSION",
          //   pushUrl: "https://www.vidyard.com/mic-test/",
          // ),
          // const CustomButtonWidget(
          //   buttonName: "File PERMISSION",
          //   pushUrl: "https://cgi-lib.berkeley.edu/ex/fup.html",
          // ),
          if (!kIsWeb)
            if (Platform.isAndroid || Platform.isIOS)
              CustomButtonWidget(
                  buttonName: "QR CODE SCANNER",
                  onPressed: () async {
                    final SharedPreferences prefs =
                        await SharedPreferences.getInstance();
                    final key = prefs.getString("push_encryption_key");
                    final EztoVerify controller = EztoVerify(
                      encryptionKey: key,
                    );
                    controller.scanQr();
                  }),
          const SizedBox(
            height: 16,
          ),
          CustomButtonWidget(
            buttonName: "TEST SITE",
            onPressed: () async {
              final SharedPreferences prefs =
                  await SharedPreferences.getInstance();
              final key = prefs.getString("push_encryption_key");
              final EztoVerify controller = EztoVerify(
                encryptionKey: key,
              );
              controller.onPushReceived(
                data: {
                  "payload": jsonEncode({
                    "version": 1,
                    "id": "qv_auth",
                    "isEncrypted": false,
                    "data": {
                      "action": "https://your-verification-url.example.com/",
                      "type": "url",
                      "socket": {
                        "requestId": "ddf0a6f7-f3e4-47e5-80ef-90334f390677",
                        "url": "https://socket.example.com"
                      }
                    }
                  })
                },
              );
            },
          ),

          CustomButtonWidget(
            buttonName: "App integrity",
            onPressed: () async {
              // final result = await IproovHandler().register([
              //   {
              //     "streamingUrl": "wss://eu.rp.secure.iproov.me/ws",
              //     "token":
              //         "82546ce92a78776a7961581032ffcec9f523aa3984a8cf3db50209011801vi07"
              //   }
              // ]);
              // debugPrint("Result : $result");
              // String? tokenReceived;

              // try {
              //   String sessionId = '9780a3d7-4bcb-4bc4-9146-0eac284d24d7';

              //   if (Platform.isAndroid) {
              //     int gpc = REPLACE_ME; // YOUR GCP PROJECT NUMBER IN ANDROID
              //     tokenReceived =
              //         await _appAttestationPlugin.getAttestationServiceSupport(
              //             challengeString: sessionId, gcp: gpc);
              //     debugPrint("token received : $tokenReceived");
              //     return;
              //   }
              //   tokenReceived = await _appAttestationPlugin
              //       .getAttestationServiceSupport(challengeString: sessionId);
              //   debugPrint("token received second : $tokenReceived");
              // } on PlatformException {
              //   tokenReceived = 'Failed to get token';
              // }
            },
          ),
        ],
      ),
    );
  }

  void sendToken() async {
    debugPrint("send Token called");
    GetTokenModel? token = await sl<PushServiceInterface>().getToken();
    debugPrint("Firebase or Push token -> ${token.pushToken}r");
    await Clipboard.setData(ClipboardData(text: token.pushToken));
    if (kReleaseMode) {
      late String device;
      if (Platform.isAndroid) {
        device = (await deviceInfo.androidInfo).model;
      } else if (Platform.isIOS) {
        device = (await deviceInfo.iosInfo).utsname.machine;
      } else if (Platform.isMacOS) {
        device = (await deviceInfo.macOsInfo).computerName;
      } else if (Platform.isWindows) {
        device = (await deviceInfo.windowsInfo).computerName;
      } else if (Platform.isLinux) {
        device = (await deviceInfo.linuxInfo).name;
      }
      String result = await ApiService.sendToken(
        token: token.pushToken,
        device: device,
      );
      BotToast.showText(text: result);
    }
  }
}
