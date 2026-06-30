import 'package:eztoverify/bloc/cubit/updater_cubit.dart';
import 'package:eztoverify/enums/close_reason.dart';
import 'package:eztoverify/enums/error_code.dart';
import 'package:eztoverify/enums/push_support.dart';
import 'package:eztoverify/interface/result_interface.dart';
import 'package:eztoverify/model/force_update_model.dart';
import 'package:eztoverify/model/get_token_model.dart';
import 'package:eztoverify/service/navigation_service.dart';
import 'package:eztoverify/service/socket_push_service.dart';
import 'package:flutter/material.dart';
import 'package:eztoverify/enums/system_permission.dart';
import 'package:eztoverify/interface/permission_handler.dart';
import 'package:eztoverify/interface/push_token_interface.dart';
import 'package:eztoverify/service/service_locator.dart';
import 'package:flutter_bloc/flutter_bloc.dart';

class EztoVerifyApp extends StatefulWidget {
  final Widget child;
  final Future<bool> Function(List<SystemPermission> deniedPermissions)?
      onPermissionDenied;
  //onPushTokenRequest only happens on Android and iOS platforms
  final Future<GetTokenModel> Function()? onPushTokenRequest;

  final void Function(String reqId, String authReqId)? onResult;
  final void Function(CloseReason reason)? onClose;
  final void Function(ErrorCode reason)? onError;
  final Future<String> Function() onSocketPushUrlRequest;
  final void Function(
    String path,
    String mimeType,
    String fileName,
  )? onFileDownloadEnd;
  final void Function(
    String fileName,
    String mimeType,
  )? onFileDownloadStart;
  final void Function(
    Map<String, dynamic> data,
    bool isFromDeeplink,
  )? onTransactionRequest;
  final ForceUpdate forceUpdate;
  final bool closeImmediatelyOnResult;
  final ThemeData theme;

  EztoVerifyApp({
    super.key,
    required this.child,
    required this.onResult,
    this.onPermissionDenied,
    this.onPushTokenRequest,
    this.onTransactionRequest,
    Future<String> Function()? onSocketPushUrlRequest,
    this.onClose,
    this.onError,
    this.onFileDownloadEnd,
    this.onFileDownloadStart,
    this.forceUpdate = const ForceUpdate(),
    this.closeImmediatelyOnResult = false,
    ThemeData? theme,
  })  : onSocketPushUrlRequest = onSocketPushUrlRequest ?? _defaultSocketUrl,
        theme = theme ??
            ThemeData(
              useMaterial3: true,
            );

  static Future<String> _defaultSocketUrl() async {
    return "https://socket.eztovrfy.com";
  }

  @override
  State<EztoVerifyApp> createState() => _EztoVerifyAppState();
}

class _EztoVerifyAppState extends State<EztoVerifyApp>
    implements PermissionHandler, PushTokenInterface, ResultInterface {
  final UpdaterCubit _updaterCubit = UpdaterCubit();
  @override
  void initState() {
    initServices();
    super.initState();
  }

  void initServices() async {
    await initSl(
      permissionHandler: this,
      pushTokenInterface: this,
      resultInterface: this,
      closeImmediatelyOnResult: widget.closeImmediatelyOnResult,
    );
    _updaterCubit.checkForUpdates(forceUpdate: widget.forceUpdate);
    final url = await onSocketPushUrlRequest();
    await sl.allReady();
    sl<SocketPushService>().registerSocketPush(url);
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      theme: widget.theme,
      navigatorKey: eztoNavigatorKey,
      home: BlocListener<UpdaterCubit, UpdaterState>(
        bloc: _updaterCubit,
        child: widget.child,
        listener: (context, state) {
          if (state is UpdaterUpdateAvailable) {
            showAdaptiveDialog(
              context: context,
              barrierDismissible: false,
              useRootNavigator: false,
              builder: (context) {
                return PopScope(
                  canPop: false,
                  child: AlertDialog.adaptive(
                    title: const Text("Update Available"),
                    content: const Text(
                      "To use this app, download the latest version",
                    ),
                    actions: [
                      TextButton(
                        onPressed: () {
                          _updaterCubit.doUpdate();
                        },
                        child: const Text("Update"),
                      ),
                    ],
                  ),
                );
              },
            );
          }
        },
      ),
    );
  }

  @override
  Future<bool> onPermissionDenied(
    List<SystemPermission> deniedPermissions,
  ) async {
    return (await widget.onPermissionDenied?.call(deniedPermissions)) ?? false;
  }

  @override
  void onResult(String requestId, String authReqId) {
    widget.onResult?.call(requestId, authReqId);
  }

  @override
  Future<GetTokenModel> getToken() async {
    return (await widget.onPushTokenRequest?.call()) ??
        GetTokenModel(
          pushToken: "",
          pushType: PushSupport.firebase,
        );
  }

  @override
  void onClose(CloseReason reason) {
    widget.onClose?.call(reason);
  }

  @override
  void onError(ErrorCode error) {
    widget.onError?.call(error);
  }

  @override
  void onTransactionRequest(Map<String, dynamic> data, bool isFromDeeplink) {
    widget.onTransactionRequest?.call(data, isFromDeeplink);
  }

  @override
  Future<String> onSocketPushUrlRequest() {
    return widget.onSocketPushUrlRequest();
  }

  @override
  void onFileDownloadEnd(String fileName, String mimeType, String path) {
    widget.onFileDownloadEnd?.call(fileName, mimeType, path);
  }

  @override
  void onFileDownloadStart(String fileName, String mimeType) {
    widget.onFileDownloadStart?.call(fileName, mimeType);
  }
}
