import 'package:eztoverify_getting_started/generated/flavor/theme_constants.g.dart';
import 'package:flutter/material.dart';

class CustomText extends StatelessWidget {
  final Color? color;
  final double? fontSize;
  final FontWeight? fontWeight;
  final int? maxLine;
  final String text;
  final TextAlign? alignment;

  const CustomText({
    super.key,
    required this.text,
    this.fontSize,
    this.color,
    this.fontWeight,
    this.alignment,
    this.maxLine,
  });

  @override
  Widget build(BuildContext context) {
    return LayoutBuilder(
        builder: (BuildContext context, BoxConstraints constraints) {
      return Text(
        textScaler: TextScaler.noScaling,
        text,
        textAlign: alignment,
        style: TextStyle(
          fontFamily: ThemeConstants().fontName,
          color: color ?? Color(ThemeConstants().primaryTextColor),
          fontSize: fontSize ?? (constraints.maxWidth > 600 ? 16 : 14),
          fontWeight: fontWeight ?? FontWeight.normal,
        ),
      );
    });
  }
}
