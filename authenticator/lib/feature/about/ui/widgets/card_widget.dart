import 'package:eztoverify_getting_started/generated/flavor/theme_constants.g.dart';
import 'package:flutter/material.dart';
import '../../../../core/widgets/custom_text.dart';

class CardWidget extends StatelessWidget {
  final String title;
  final String content;
  const CardWidget({super.key, required this.title, required this.content});

  @override
  Widget build(BuildContext context) {
    return LayoutBuilder(
      builder: (BuildContext context, BoxConstraints constraints) {
        bool isTablet = constraints.maxWidth > 600;
        return Card(
          elevation: 2.0,
          color: Color(ThemeConstants().cardBgColor),
          shape: RoundedRectangleBorder(
            side: BorderSide(
              color: Color(ThemeConstants().cardStrokeColor),
              width: 1.0,
            ),
            borderRadius: const BorderRadius.all(Radius.circular(8)),
          ),
          child: ClipPath(
            clipper: ShapeBorderClipper(
                shape: RoundedRectangleBorder(
                    borderRadius: BorderRadius.circular(8))),
            child: Container(
              decoration: const BoxDecoration(
                border: Border(
                  left: BorderSide(
                    color: Colors.black,
                    width: 6,
                  ),
                ),
              ),
              child: Padding(
                padding: const EdgeInsets.all(16.0),
                child: Column(
                  mainAxisAlignment: MainAxisAlignment.start,
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    CustomText(
                      text: title,
                      fontSize: isTablet ? 14 : 12,
                    ),
                    CustomText(
                      text: content,
                      fontWeight: FontWeight.bold,
                      fontSize: isTablet ? 16 : 14,
                    ),
                  ],
                ),
              ),
            ),
          ),
        );
      },
    );
  }
}
