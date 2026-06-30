import 'dart:io';

import 'package:eztoverify_getting_started/constants/image_constants.dart';
import 'package:eztoverify_getting_started/generated/flavor/theme_constants.g.dart';
import 'package:flutter/material.dart';
import 'package:flutter_svg/flutter_svg.dart';

class AppbarWidget extends StatelessWidget {
  final int? padding;
  final String? icon;
  final double? fontSize;
  const AppbarWidget({
    super.key,
    this.padding = 20,
    this.icon = ImageConstants.hamburgerSvg,
    this.fontSize = 16,
  });

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: EdgeInsets.only(
        top: (Platform.isAndroid || Platform.isIOS) ? kToolbarHeight : 10,
        left: 10,
      ),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.start,
        crossAxisAlignment: CrossAxisAlignment.center,
        children: [
          Builder(builder: (context) {
            return IconButton(
              onPressed: () {
                if (icon == ImageConstants.hamburgerSvg) {
                  Scaffold.of(context).openDrawer();
                } else {
                  Navigator.pop(context);
                }
              },
              icon: SvgPicture.asset(
                icon ?? ImageConstants.hamburgerSvg,
                height: 45,
                width: 45,
                theme: SvgTheme(
                  currentColor: Color(ThemeConstants().primaryColor),
                ),
              ),
            );
          }),
          Expanded(
            child: Padding(
              padding: const EdgeInsets.only(right: 50),
              child: SvgPicture.asset(
                ImageConstants.logoFullSvg,
                height: 45,
              ),
            ),
          ),
        ],
      ),
    );
  }
}
