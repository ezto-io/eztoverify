import 'package:eztoverify_getting_started/core/widgets/custom_text.dart';
import 'package:eztoverify_getting_started/generated/flavor/theme_constants.g.dart';
import 'package:flutter/material.dart';

class CustomButton extends StatelessWidget {
  final String text;
  final Color? buttonColor;
  final Function()? onPress;
  final Color? buttonTextColor;
  final Color? borderColor;
  final double? borderWidth;
  final bool isDisabled;
  final double? width;
  final Widget? image;
  final FontWeight? textFontWeight;
  final double? fontSize;

  const CustomButton({
    super.key,
    required this.text,
    this.buttonColor,
    required this.onPress,
    this.buttonTextColor,
    this.borderColor,
    this.borderWidth,
    this.isDisabled = false,
    this.width,
    this.textFontWeight,
    this.image,
    this.fontSize,
  });

  @override
  Widget build(BuildContext context) {
    return LayoutBuilder(
        builder: (BuildContext context, BoxConstraints constraints) {
      return Container(
        width: constraints.maxWidth > 600
            ? MediaQuery.of(context).size.width / 3
            : MediaQuery.of(context).size.width / 2,
        height: MediaQuery.of(context).size.height * 0.054,
        margin: EdgeInsets.symmetric(
            horizontal: constraints.maxWidth > 600 ? 65 : 45),
        child: ElevatedButton.icon(
          style: ElevatedButton.styleFrom(
              elevation: 5,
              backgroundColor: Color(ThemeConstants().primaryColor),
              shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(14))),
          onPressed: isDisabled ? null : onPress,
          icon: image!,
          label: CustomText(
            text: text,
            fontSize: fontSize ?? 16,
            maxLine: 1,
            fontWeight: FontWeight.w500,
            color: buttonColor ??
                Color(
                  ThemeConstants().whiteColor,
                ),
          ),
        ),
      );
    });
  }
}
