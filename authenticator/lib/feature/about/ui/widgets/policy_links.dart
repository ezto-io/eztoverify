import 'package:eztoverify_getting_started/generated/flavor/theme_constants.g.dart';
import 'package:eztoverify_getting_started/generated/flavor/url_generated.g.dart';
import 'package:flutter/material.dart';
import 'package:url_launcher/url_launcher.dart';

import '../../../../core/widgets/custom_text.dart';

class PolicyLinksWidget extends StatelessWidget {
  const PolicyLinksWidget({
    super.key,
  });

  @override
  Widget build(BuildContext context) {
    return Wrap(
      alignment: WrapAlignment.center,
      children: <Widget>[
        GestureDetector(
          child: const CustomText(text: 'Privacy Policy'),
          onTap: () async {
            if (!await launchUrl(
              Uri.parse(UrlConstants().privacyPolicy),
              mode: LaunchMode.externalApplication,
            )) {
              throw Exception(
                  'Could not launch ${UrlConstants().privacyPolicy}');
            }
          },
        ),
        CustomText(
          text: ' | ',
          color: Color(ThemeConstants().dividerColor),
        ),
        GestureDetector(
          child: const CustomText(text: 'Terms & Conditions'),
          onTap: () async {
            if (!await launchUrl(
              Uri.parse(UrlConstants().termsAndConditions),
              mode: LaunchMode.externalApplication,
            )) {
              throw Exception(
                  'Could not launch ${UrlConstants().termsAndConditions}');
            }
          },
        ),
      ],
    );
  }
}
