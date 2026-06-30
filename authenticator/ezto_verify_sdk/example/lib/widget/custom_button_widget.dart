import 'package:flutter/material.dart';

class CustomButtonWidget extends StatelessWidget {
  final String buttonName;
  final void Function()? onPressed;
  const CustomButtonWidget(
      {super.key, required this.buttonName, required this.onPressed});

  @override
  Widget build(BuildContext context) {
    return ElevatedButton(
      onPressed: onPressed,
      style: ElevatedButton.styleFrom(
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(12),
        ),
      ),
      child: Text(buttonName),
    );
  }
}
