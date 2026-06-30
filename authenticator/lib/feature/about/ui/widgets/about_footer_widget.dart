import 'package:flutter/material.dart';
import 'package:flutter/widgets.dart';

import '../../../../core/widgets/custom_text.dart';

class AboutFooterWidget extends StatelessWidget {
  const AboutFooterWidget({super.key});

  @override
  Widget build(BuildContext context) {
    return Align(
      alignment: Alignment.bottomCenter,
      child: Container(
        padding: const EdgeInsets.all(30.0),
        alignment: Alignment.center,
        child: Column(
          children: [
            SizedBox(
              height: MediaQuery.of(context).size.height * 0.03,
            ),
            const Center(
                child: CustomText(
              alignment: TextAlign.center,
              text: '© 2024 ezto, Trademark Registered. All Rights Reserved.',
            )),
          ],
        ),
      ),
    );
  }
}
