import 'package:flutter/material.dart';

class NfcBottomSheetWidget {
  static nfcBottomSheet(BuildContext context) {
    showModalBottomSheet(
        enableDrag: false,
        isDismissible: false,
        context: context,
        builder: (context) {
          return SizedBox(
            height: MediaQuery.of(context).size.height * 0.3,
            width: MediaQuery.of(context).size.width,
            child: Column(
              children: [
                SizedBox(
                  height: MediaQuery.of(context).size.height * 0.04,
                ),
                const Text(
                  "Ready to Scan",
                  style: TextStyle(color: Colors.grey, fontSize: 23),
                ),
                SizedBox(
                  height: MediaQuery.of(context).size.height * 0.04,
                ),
                Icon(
                  Icons.nfc_rounded,
                  size: 30,
                  color: Colors.blue[900],
                ),
                SizedBox(
                  height: MediaQuery.of(context).size.height * 0.04,
                ),
                const Text(
                  "Hold your device near the NFC tag.",
                  style: TextStyle(fontSize: 15),
                ),
                SizedBox(
                  height: MediaQuery.of(context).size.height * 0.03,
                ),
              ],
            ),
          );
        });
  }
}
