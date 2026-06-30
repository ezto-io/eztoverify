import 'dart:io';

import 'package:app_links/app_links.dart';
import 'package:eztoverify/enums/close_reason.dart';
import 'package:eztoverify/service/custom_scheme_service.dart';
import 'package:eztoverify/service/ezto_result_service.dart';
import 'package:eztoverify/service/navigation_service.dart';
import 'package:eztoverify/service/platform_webview_controller.dart';
import 'package:eztoverify/service/service_locator.dart';
import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:flutter_inappwebview/flutter_inappwebview.dart';
import 'package:eztoverify/bloc/web_view/web_view_cubit.dart';
import 'package:eztoverify/model/payload_data_model.dart';
import 'package:eztoverify/service/java_script_handler_service.dart';
import 'package:eztoverify/widget/confirm_exit_alert_dialog.dart';
import 'package:url_launcher/url_launcher.dart';

const enableRemoteDebugging = false;

class WebViewScreen extends StatefulWidget {
  final DataPayload pushDataModel;

  const WebViewScreen({
    super.key,
    required this.pushDataModel,
  });

  @override
  State<WebViewScreen> createState() => _WebViewScreenState();
}

class _WebViewScreenState extends State<WebViewScreen> {
  final GlobalKey webViewKey = GlobalKey();
  late final WebViewCubit _cubit;
  late final JavaScriptHandlerService _handlerService;

  @override
  void initState() {
    super.initState();
    _handlerService = JavaScriptHandlerService(
      socketInfo: widget.pushDataModel.payload.data.socket,
      context: context,
    );
    _cubit = WebViewCubit();
    startWebview();
  }

  void startWebview() async {
    _cubit.setupWebview(widget.pushDataModel);
    if (Platform.isAndroid && enableRemoteDebugging) {
      await InAppWebViewController.setWebContentsDebuggingEnabled(true);
    }
  }

  @override
  void dispose() {
    _handlerService.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    InAppWebViewSettings options = InAppWebViewSettings(
      supportZoom: false,
      transparentBackground: true,
      javaScriptCanOpenWindowsAutomatically: true,
      cacheEnabled: true,
      clearCache: false,
      javaScriptEnabled: true,
      incognito: true,
      builtInZoomControls: false,
      displayZoomControls: false,
      supportMultipleWindows: true,
      safeBrowsingEnabled: true,
      cacheMode: CacheMode.LOAD_DEFAULT,
      useHybridComposition: true,
      allowsInlineMediaPlayback: true,
      allowsAirPlayForMediaPlayback: true,
      allowsBackForwardNavigationGestures: false,
      useOnDownloadStart: true,
      useShouldOverrideUrlLoading: true,
      mediaPlaybackRequiresUserGesture: false,
    );

    return PopScope(
      canPop: false,
      onPopInvokedWithResult: (didPop, result) {
        if (didPop) {
          return;
        }
        _closeByUser();
      },
      child: BlocConsumer<WebViewCubit, WebViewStates>(
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
            resizeToAvoidBottomInset: false,
            appBar: AppBar(
                automaticallyImplyLeading: false,
                title: Align(
                  alignment: Alignment.bottomRight,
                  child: IconButton(
                    onPressed: () async {
                      _closeByUser();
                    },
                    icon: const Icon(
                      Icons.close_rounded,
                    ),
                  ),
                )),
            body: Stack(
              children: [
                if (state is! WebViewInitial)
                  Padding(
                    padding: EdgeInsets.only(bottom: MediaQuery.of(context).viewPadding.bottom + 16), 
                    child: InAppWebView(
                      key: webViewKey,
                      initialUrlRequest: URLRequest(
                          url: WebUri(widget.pushDataModel.payload.data.action)),
                      initialSettings: options,
                      onWebViewCreated: (controller) {
                        _handlerService.register(
                          PlatformWebViewController(
                            mobileWebviewController: controller,
                          ),
                          socketInfo: widget.pushDataModel.payload.data.socket,
                        );
                      },
                      shouldOverrideUrlLoading:
                          (controller, navigationAction) async {
                        final CustomSchemeService customSchemeService =
                            CustomSchemeService();
                        final uri = navigationAction.request.url;
                        final customScheme =
                            await customSchemeService.preprocessUri(uri);
                        if (customScheme != null) {
                          await launchUrl(customScheme.appToAppUri);

                          final callback =
                              await AppLinks().stringLinkStream.first;

                          final callbackUri = customSchemeService.postProcessUri(
                            customScheme,
                            callback,
                          );
                          if (callbackUri == null) {
                            return NavigationActionPolicy.CANCEL;
                          }
                          controller.loadUrl(
                            urlRequest: URLRequest(
                              url: WebUri.uri(callbackUri),
                            ),
                          );
                          return NavigationActionPolicy.CANCEL;
                        } else {
                          final urlString = uri.toString();
                          if (!urlString.startsWith("http") &&
                              !urlString.startsWith('https')) {
                            await launchUrl(uri!);
                          }
                        }
                        return NavigationActionPolicy.ALLOW;
                      },
                      onReceivedError:
                          (controller, resourceRequest, resourceError) {
                        if (Platform.isIOS &&
                            resourceRequest.url
                                .toString()
                                .toLowerCase()
                                .contains(".pdf")) {
                          //iOS triggers an error during download, this can be ignored
                        } else if (resourceError.description ==
                            "net::ERR_BLOCKED_BY_ORB") {
                          //Can be ignored
                        } else {
                          _cubit.onInternetError();
                        }
                      },
                      onCreateWindow: (controller, createWindowRequest) async {
                        if (createWindowRequest.request.url != null) {
                          await launchUrl(createWindowRequest.request.url!);
                          return true;
                        }
                        return false;
                      },
                      onReceivedServerTrustAuthRequest:
                          (controller, challenge) async {
                        if (challenge.protectionSpace.sslError?.code ==
                            SslErrorType.UNSPECIFIED) {
                          return ServerTrustAuthResponse(
                            action: ServerTrustAuthResponseAction.PROCEED,
                          );
                        }
                        _cubit.onSslError();
                        return ServerTrustAuthResponse(
                          action: ServerTrustAuthResponseAction.CANCEL,
                        );
                      },
                      onPermissionRequest: (controller, request) async {
                        debugPrint("on permission request -> $request");
                        return _cubit.onPermissionRequest(
                          request: request,
                        );
                      },
                      onConsoleMessage: (controller, consoleMessage) {
                        debugPrint(
                            "console messge ----- ${consoleMessage.message}");
                      },
                      onProgressChanged: (controller, progress) {
                        _cubit.onLoading();
                        if (progress == 100) {
                         _cubit.onLoaded();
                        }
                      },
                    ),
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
                      child: CircularProgressIndicator(),
                    ),
                  ),
              ],
            ),
          );
        },
      ),
    );
  }

  Future<bool> _closeByUser() async {
    return confirmExitAlertDialog(
      context,
      socketInfo: widget.pushDataModel.payload.data.socket,
    );
  }
}
