import 'dart:convert';
import 'dart:io';
import 'package:eztoverify/controller/ezto_verify.dart';
import 'package:eztoverify_getting_started/constants/image_constants.dart';
import 'package:eztoverify_getting_started/core/scanQrBloc/bloc/scan_qr_bloc.dart';
import 'package:eztoverify_getting_started/core/scanQrBloc/bloc/scan_qr_event.dart';
import 'package:eztoverify_getting_started/core/services/encryption_service/enc_key_service.dart';
import 'package:eztoverify_getting_started/core/widgets/custom_text.dart';
import 'package:eztoverify_getting_started/generated/flavor/generated_constants.g.dart';
import 'package:eztoverify_getting_started/generated/flavor/theme_constants.g.dart';
import 'package:eztoverify_getting_started/generated/l10n.dart';
import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:flutter_svg/flutter_svg.dart';
import '../../../../core/widgets/custom_button.dart';

class HomeFooterWidget extends StatefulWidget {
  const HomeFooterWidget({super.key});

  @override
  State<HomeFooterWidget> createState() => _HomeFooterWidgetState();
}

class _HomeFooterWidgetState extends State<HomeFooterWidget> {
  final TextEditingController automationTestController =
      TextEditingController();

  @override
  void dispose() {
    automationTestController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final double bottomSystemPadding = MediaQuery.viewPaddingOf(context).bottom;
    return LayoutBuilder(
        builder: (BuildContext context, BoxConstraints constraints) {
      return Center(
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.center,
          children: [
            if (Platform.isAndroid || Platform.isIOS)
              CustomText(
                text: S.current.home_page_sub1,
                fontSize: constraints.maxWidth > 600 ? 20 : 18,
              ),
            if (Platform.isAndroid || Platform.isIOS)
              CustomText(
                  text: S.current.home_page_sub2,
                  fontSize: constraints.maxWidth > 600 ? 20 : 18,
                  fontWeight: FontWeight.w600),
            if (Platform.isAndroid || Platform.isIOS)
              SizedBox(
                height: MediaQuery.of(context).size.height * 0.04,
              ),
            if (GeneratedConstants().isAutoTest)
              Padding(
                padding: const EdgeInsets.all(10.0),
                child: Row(
                  children: [
                    Expanded(
                      flex: 2,
                      child: TextFormField(
                        controller: automationTestController,
                        decoration: const InputDecoration(
                          border: OutlineInputBorder(),
                        ),
                      ),
                    ),
                    Expanded(
                      child: Padding(
                        padding: const EdgeInsets.only(left: 8.0),
                        child: ElevatedButton(
                          style: ElevatedButton.styleFrom(
                              elevation: 5,
                              backgroundColor:
                                  Color(ThemeConstants().primaryColor),
                              shape: RoundedRectangleBorder(
                                  borderRadius: BorderRadius.circular(14))),
                          onPressed: () async {
                            String result = automationTestController.text;
                            if (result != "") {
                              String json = jsonEncode({"payload": result});
                              Map<String, dynamic> jsonResult =
                                  jsonDecode(json);
                              var encryptionKey =
                                  await EncKeyService.getEncKey();
                              EztoVerify(encryptionKey: encryptionKey)
                                  .onPushReceived(
                                data: jsonResult,
                              );
                            }
                          },
                          child: const Text(
                            "SUBMIT",
                            style: TextStyle(
                                color: Colors.white,
                                fontWeight: FontWeight.w500,
                                fontSize: 16),
                          ),
                        ),
                      ),
                    ),
                  ],
                ),
              ),
            if (Platform.isAndroid || Platform.isIOS)
              CustomButton(
                text: S.current.home_page_scan_button,
                fontSize: constraints.maxWidth > 600 ? 20 : 18,
                image: SvgPicture.asset(
                  ImageConstants.scanSvg,
                  height: 28,
                  width: 28,
                ),
                onPress: () {
                  context.read<ScanQrBloc>().add(ScanQr());
                },
              ),
            if (Platform.isMacOS || Platform.isWindows)
              const Padding(
                padding: EdgeInsets.only(bottom: 16.0),
                child: Center(
                    child: CustomText(
                  alignment: TextAlign.center,
                  text:
                      '© 2024 ezto, Trademark Registered. All Rights Reserved.',
                )),
              ),
            if (Platform.isAndroid || Platform.isIOS)
              SizedBox(
                  height: bottomSystemPadding + constraints.maxWidth > 600
                      ? MediaQuery.of(context).size.height * 0.08
                      : MediaQuery.of(context).size.height * 0.04)
          ],
        ),
      );
    });
  }
}
