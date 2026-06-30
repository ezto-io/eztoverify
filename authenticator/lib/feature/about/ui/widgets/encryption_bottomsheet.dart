import 'dart:io';
import 'package:eztoverify_getting_started/core/services/encryption_service/enc_key_service.dart';
import 'package:eztoverify_getting_started/core/services/storage_service/secure_storage_service.dart';
import 'package:eztoverify_getting_started/generated/flavor/theme_constants.g.dart';
import 'package:flutter/material.dart';
import '../../../../core/widgets/custom_text.dart';

class EncryptionBottomsheet extends StatelessWidget {
  const EncryptionBottomsheet({
    super.key,
    required this.textController,
    required this.socketTextController,
  });

  final TextEditingController textController;
  final TextEditingController socketTextController;

  @override
  Widget build(BuildContext context) {
    return Container(
      decoration: BoxDecoration(
        color: Color(ThemeConstants().whiteColor),
        borderRadius: const BorderRadius.only(
          topLeft: Radius.circular(30.0),
          topRight: Radius.circular(30.0),
        ),
      ),
      height: MediaQuery.of(context).size.height * 0.85,
      child: Padding(
        padding: const EdgeInsets.fromLTRB(20, 20, 20, 20),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              mainAxisAlignment: MainAxisAlignment.end,
              children: [
                IconButton(
                  icon: const Icon(Icons.close),
                  onPressed: () {
                    Navigator.of(context).pop();
                  },
                ),
              ],
            ),
            const SizedBox(
              height: 50,
            ),
            const CustomText(
              text: 'Update Encryption Key?',
              fontSize: 20,
              fontWeight: FontWeight.w500,
            ),
            const SizedBox(
              height: 15,
            ),
            Row(
              children: [
                Expanded(
                  child: SizedBox(
                    height: 50,
                    child: TextField(
                      controller: textController,
                      style: TextStyle(
                        fontFamily: ThemeConstants().fontName,
                        fontSize: 16,
                      ),
                      decoration: InputDecoration(
                          contentPadding: const EdgeInsets.all(14.0),
                          hintText: 'Enter your encryption key',
                          border: OutlineInputBorder(
                            borderRadius: BorderRadius.circular(10.0),
                          )),
                    ),
                  ),
                ),
                Padding(
                  padding: const EdgeInsets.symmetric(horizontal: 10.0),
                  child: ElevatedButton(
                    onPressed: () async {
                      if (textController.text.isNotEmpty) {
                        EncKeyService.setEncKey(textController.text);

                        debugPrint("Encryption key updated!");
                        Navigator.pushNamed(context, "/home");
                      } else {
                        debugPrint("Encryption key cannot be empty!");
                      }
                    },
                    style: ButtonStyle(
                      backgroundColor: WidgetStateProperty.all<Color>(
                        Theme.of(context).primaryColor,
                      ),
                      shape: WidgetStateProperty.all<RoundedRectangleBorder>(
                        RoundedRectangleBorder(
                          borderRadius: BorderRadius.circular(10),
                        ),
                      ),
                    ),
                    child: const CustomText(
                      text: 'Update',
                      fontSize: 16,
                      color: Colors.white,
                      fontWeight: FontWeight.w500,
                    ),
                  ),
                )
              ],
            ),
            if (Platform.isMacOS || Platform.isWindows)
              const SizedBox(
                height: 30,
              ),
            if (Platform.isMacOS || Platform.isWindows)
              const CustomText(
                text: 'Update Socket Url?',
                fontSize: 20,
                fontWeight: FontWeight.w500,
              ),
            if (Platform.isMacOS || Platform.isWindows)
              const SizedBox(
                height: 15,
              ),
            if (Platform.isMacOS || Platform.isWindows)
              Row(
                children: [
                  Expanded(
                    child: SizedBox(
                      height: 50,
                      child: TextField(
                        controller: socketTextController,
                        style: TextStyle(
                          fontSize: 16,
                          fontFamily: ThemeConstants().fontName,
                        ),
                        decoration: InputDecoration(
                            contentPadding: const EdgeInsets.all(14.0),
                            hintText: 'Enter your socket url',
                            border: OutlineInputBorder(
                              borderRadius: BorderRadius.circular(10.0),
                            )),
                      ),
                    ),
                  ),
                  Padding(
                    padding: const EdgeInsets.symmetric(horizontal: 10.0),
                    child: ElevatedButton(
                      onPressed: () async {
                        if (socketTextController.text.isNotEmpty) {
                          StorageService()
                              .setSocketUrl(socketTextController.text);
                          Navigator.pushNamed(context, "/home");
                        } else {
                          debugPrint("Encryption key cannot be empty!");
                        }
                      },
                      style: ButtonStyle(
                        backgroundColor: WidgetStateProperty.all<Color>(
                            Color(ThemeConstants().primaryColor)),
                        shape: WidgetStateProperty.all<RoundedRectangleBorder>(
                          RoundedRectangleBorder(
                            borderRadius: BorderRadius.circular(10),
                          ),
                        ),
                      ),
                      child: CustomText(
                        text: 'Update',
                        fontSize: 16,
                        color: Color(ThemeConstants().whiteColor),
                        fontWeight: FontWeight.w500,
                      ),
                    ),
                  )
                ],
              ),
            if (Platform.isMacOS || Platform.isWindows)
              const CustomText(
                text:
                    'Note : After updating the socket URL, please restart the app to continue.',
                fontSize: 10,
              ),
            const SizedBox(
              height: 50,
            ),
          ],
        ),
      ),
    );
  }
}
