import 'package:eztoverify/enums/close_reason.dart';
import 'package:eztoverify/service/ezto_result_service.dart';
import 'package:eztoverify/service/navigation_service.dart';
import 'package:eztoverify/service/service_locator.dart';
import 'package:eztoverify/service/windows_js_handler_service.dart';
import 'package:eztoverify/widget/confirm_exit_alert_dialog.dart';
import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:eztoverify/bloc/web_view/web_view_cubit.dart';
import 'package:eztoverify/model/payload_data_model.dart';
import 'package:webview_windows/webview_windows.dart';

class WindowsWebViewScreen extends StatefulWidget {
  final DataPayload pushDataModel;

  const WindowsWebViewScreen({
    super.key,
    required this.pushDataModel,
  });

  @override
  State<WindowsWebViewScreen> createState() => _WindowsWebViewScreenState();
}

class _WindowsWebViewScreenState extends State<WindowsWebViewScreen> {
  late final WebViewCubit _cubit;
  final controller = WebviewController();
  late final WindowsJavaScriptHandlerService _windowsWebView;
  @override
  void initState() {
    super.initState();
    _windowsWebView = WindowsJavaScriptHandlerService(
      widget.pushDataModel.payload.data.socket,
      context,
    );

    _cubit = WebViewCubit();
    startWebview();
  }

  void startWebview() async {
    _cubit.setupWindowsWebview(
        dataPayload: widget.pushDataModel,
        controller: controller,
        jsHandler: _windowsWebView);
  }

  @override
  void dispose() {
    _cubit.windowsWebviewDispose(controller);
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return BlocConsumer<WebViewCubit, WebViewStates>(
      bloc: _cubit,
      listener: (context, state) async {
        if (state is WebViewFailed) {
          sl<EztoResultService>().onError(state.errorCode);
          sl<EztoResultService>().onClose(CloseReason.transactionCompleted);
          sl<NavigationService>().popAll();
        }
      },
      builder: (context, state) {
        return Scaffold(
          appBar: AppBar(
              automaticallyImplyLeading: false,
              title: Align(
                alignment: Alignment.bottomRight,
                child: IconButton(
                  onPressed: () async {
                    confirmExitAlertDialog(
                      context,
                      socketInfo: widget.pushDataModel.payload.data.socket,
                    );
                  },
                  icon: const Icon(
                    Icons.close_rounded,
                  ),
                ),
              )),
          body: Stack(
            children: [
              if (state is! WebViewInitial)
                Webview(
                  controller,
                  permissionRequested: (url, permissionKind, isUserInitiated) {
                    return _cubit.onWindowsPermissionRequest(
                        request: permissionKind);
                  },
                ),
              if (state is WebViewLoading || state is WebViewInitial)
                Opacity(
                  opacity: 0.7,
                  child: Container(
                    color: Colors.white,
                  ),
                ),
              if (state is WebViewLoading || state is WebViewInitial)
                Container(
                  color: Colors.transparent,
                  child: const Center(
                    child: CircularProgressIndicator(
                      color: Colors.black,
                    ),
                  ),
                ),
            ],
          ),
        );
      },
    );
  }
}
