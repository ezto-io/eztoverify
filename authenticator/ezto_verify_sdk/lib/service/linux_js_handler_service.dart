import 'dart:convert';
import 'package:desktop_webview_window/desktop_webview_window.dart';
import 'package:eztoverify/service/result_handler_service.dart';
import 'package:eztoverify/model/payload_data_model.dart';
import 'package:flutter/material.dart';

//TODO: Should be updated when implementing linux
class LinuxJavaScriptHandlerService {
  final ResultHandlerService resultHandlerService = ResultHandlerService();

  Future<void> register(
    Webview controller, {
    required Socket socketInfo,
  }) async {
    //Success
    controller.registerJavaScriptMessageHandler("success", (name, body) {
      debugPrint("success called");
      resultHandlerService.handleSuccess(linuxWebview: controller);
    });

    //Failure
    controller.registerJavaScriptMessageHandler("failure", (name, body) {
      resultHandlerService.handleFailure(
          socketInfo: socketInfo, linuxWebview: controller);
    });

    //Check biometric
    controller.registerJavaScriptMessageHandler("checkBiometric",
        (name, body) async {
      handleDynamicHandler(name, body, controller);
    });

    //Registration biometric
    controller.registerJavaScriptMessageHandler("registerBiometric",
        (name, body) {
      handleDynamicHandler(name, body, controller);
    });

    //Verify biometric
    controller.registerJavaScriptMessageHandler("verifyBiometric",
        (name, body) {
      handleDynamicHandler(name, body, controller);
    });

    //Registration ashield android
    controller.registerJavaScriptMessageHandler("registerAshield",
        (name, body) {
      handleDynamicHandler(name, body, controller);
    });

    //Authentication ashield android
    controller.registerJavaScriptMessageHandler("authenticateAshield",
        (name, body) {
      handleDynamicHandler(name, body, controller);
    });

    //Register Fido
    controller.registerJavaScriptMessageHandler("registerFido", (name, body) {
      handleDynamicHandler(name, body, controller);
    });

    // //Sign Fido
    controller.registerJavaScriptMessageHandler("signFido", (name, body) {
      handleDynamicHandler(name, body, controller);
    });

    //Get Push Token
    controller.registerJavaScriptMessageHandler("getFcmToken", (name, body) {
      handleDynamicHandler(name, body, controller);
    });

    // //Check miracl user
    // controller.registerJavaScriptMessageHandler("getMiraclUser", (name, body) {
    // sl<MiraclHandler>().isUserRegistered;});

    // //Register miracl user
    // controller.registerJavaScriptMessageHandler("registerMiraclUser", (name, body) {
    // sl<MiraclHandler>().registerMiraclUser;});

    // //Verify miracl user
    // controller.registerJavaScriptMessageHandler("loginMiraclUser", (name, body) {sl<MiraclHandler>().loginMiraclUser;});

    controller.registerJavaScriptMessageHandler("isJsFidoSupported",
        (name, body) {
      handleDynamicHandler(name, body, controller);
    });

    controller.registerJavaScriptMessageHandler("registerSilentBinding",
        (name, body) {
      handleDynamicHandler(name, body, controller);
    });

    controller.registerJavaScriptMessageHandler("verifySilentBinding",
        (name, body) {
      handleDynamicHandler(name, body, controller);
    });

    //Get Device Details
    controller.registerJavaScriptMessageHandler("readDeviceDetails",
        (name, body) {
      handleDynamicHandler(name, body, controller);
    });

    controller.registerJavaScriptMessageHandler('copyToClipboard',
        (name, body) {
      handleDynamicHandler(name, body, controller);
    });
  }

  Future<void> handleDynamicHandler(
      String handlerName, dynamic params, Webview controller) async {
    final jsonString = jsonEncode(params ?? {});
    final resultParams = jsonDecode(jsonString);
    final args = [resultParams];
    final response =
        await resultHandlerService.getHandlerInstance(handlerName).call(args);
    final result = resultHandlerService.getResult(
      handlerName: handlerName,
      params: response,
    );
    controller.postWebMessageAsJson(jsonEncode(result));
  }
}
