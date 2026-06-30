import 'package:eztoverify_getting_started/constants/image_constants.dart';
import 'package:flutter/material.dart';

class AppBackgroundWidget extends StatelessWidget {
  final Widget child;
  const AppBackgroundWidget({super.key, required this.child});

  @override
  Widget build(BuildContext context) {
    return Container(
      decoration: const BoxDecoration(
        color: Colors.white,
        image: DecorationImage(
          image: AssetImage(
            ImageConstants.homeBackgroundPng,
          ),
          fit: BoxFit.cover,
        ),
      ),
      child: child,
    );
  }
}
