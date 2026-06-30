import 'dart:async';
import 'dart:convert';

import 'package:eztoverify/bloc/qr_scanner/qr_scanner_cubit.dart';
import 'package:eztoverify/enums/close_reason.dart';
import 'package:eztoverify/enums/error_code.dart';
import 'package:eztoverify/service/ezto_result_service.dart';
import 'package:eztoverify/service/navigation_service.dart';
import 'package:eztoverify/service/service_locator.dart';
import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:mobile_scanner/mobile_scanner.dart';
import 'package:eztoverify/exceptions/encryption_key_exception.dart';
import 'package:eztoverify/model/qr_scanner_result.dart';

class QrScannerScreen extends StatefulWidget {
  final String? encryptionKey;
  const QrScannerScreen({
    super.key,
    this.encryptionKey,
  });

  @override
  State<QrScannerScreen> createState() => _QrScannerScreenState();
}

class _QrScannerScreenState extends State<QrScannerScreen>
    with WidgetsBindingObserver {
  bool isFirst = true;
  final QrScannerCubit _cubit = QrScannerCubit();
  // Barcode? _barcode;
  StreamSubscription<Object?>? _subscription;
  // final MobileScannerController controller = MobileScannerController(
  //   facing: CameraFacing.back,
  //   detectionTimeoutMs: 500,
  // );

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    // If the controller is not ready, do not try to start or stop it.
    // Permission dialogs can trigger lifecycle changes before the controller is ready.
    // if (!controller.value.isInitialized) {
    //   return;
    // }

    switch (state) {
      case AppLifecycleState.detached:
      case AppLifecycleState.hidden:
      case AppLifecycleState.paused:
        return;
      case AppLifecycleState.resumed:
      // Restart the scanner when the app is resumed.
      // Don't forget to resume listening to the barcode events.
      // _subscription = controller.barcodes.listen(_handleBarcode);

      // unawaited(controller.start());
      case AppLifecycleState.inactive:
        // Stop the scanner when the app is paused.
        // Also stop the barcode events subscription.
        unawaited(_subscription?.cancel());
        _subscription = null;
      // unawaited(controller.stop());
    }
  }

  @override
  void initState() {
    super.initState();
    _cubit.checkCameraPermission();
    // Start listening to lifecycle changes.
    WidgetsBinding.instance.addObserver(this);
    // Start listening to the barcode events.
    // _subscription = controller.barcodes.listen(_handleBarcode);

    // Finally, start the scanner itself.
    // unawaited(controller.start());
  }

  @override
  Future<void> dispose() async {
    // Stop listening to lifecycle changes.
    WidgetsBinding.instance.removeObserver(this);
    // Stop listening to the barcode events.
    unawaited(_subscription?.cancel());
    _subscription = null;
    // Dispose the widget itself.
    super.dispose();
    // Finally, dispose of the controller.
    // await controller.dispose();
  }

  // void _handleBarcode(BarcodeCapture barcodes) {
  //   if (mounted) {
  //     setState(() {
  //       _barcode = barcodes.barcodes.firstOrNull;
  //     });
  //   }
  // }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      resizeToAvoidBottomInset: false,
      appBar: AppBar(
          automaticallyImplyLeading: false,
          title: Align(
            alignment: Alignment.bottomRight,
            child: IconButton(
              onPressed: () async {
                sl<EztoResultService>().onClose(CloseReason.byUser);
                sl<NavigationService>().popAll();
              },
              icon: const Icon(
                Icons.close_rounded,
              ),
            ),
          )),
      body: BlocConsumer<QrScannerCubit, QrScannerState>(
        bloc: _cubit,
        listener: (context, state) {},
        builder: (context, state) {
          if (state is QrScannerSuccess) {
            return Padding(
              padding: const EdgeInsets.all(16.0),
              child: ClipRRect(
                borderRadius: BorderRadius.circular(16),
                child: MobileScanner(
                  // controller: controller,
                  fit: BoxFit.cover,
                  placeholderBuilder: (context) {
                    return Container(
                      color: Colors.white,
                      child: const Center(
                        child: CircularProgressIndicator(),
                      ),
                    );
                  },
                  onDetect: (capture) {
                    debugPrint("Capture -> ${capture.barcodes.first.rawValue}");
                    if (isFirst) {
                      setState(() {
                        isFirst = false;
                      });
                      if (capture.barcodes.first.rawValue != null) {
                        String result = capture.barcodes.first.rawValue!;
                        String json = jsonEncode({"payload": result});
                        Map<String, dynamic> jsonResult = jsonDecode(json);
                        try {
                          Navigator.pop(
                            context,
                            QrScannerResult(
                              status: QrStatus.success,
                              data: jsonResult,
                            ),
                          );
                        } catch (e) {
                          debugPrint(e.toString());
                          sl<EztoResultService>().onError(
                            (e is EncryptionKeyException)
                                ? ErrorCode.missingEncryptionKey
                                : ErrorCode.invalidPayload,
                          );
                          sl<NavigationService>().popAll();
                        }
                      }
                    }
                  },
                ),
              ),
            );
          }
          if (state is QrScannerFailure) {
            return const Center(
              child: Text(
                "Camera permission required for QR, Please enable camera permission from settings!",
                textAlign: TextAlign.center,
              ),
            );
          }
          return const Center(child: CircularProgressIndicator());
        },
      ),
    );
  }
}
