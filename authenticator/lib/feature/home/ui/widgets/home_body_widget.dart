import 'package:eztoverify_getting_started/constants/image_constants.dart';
import 'package:eztoverify_getting_started/core/widgets/custom_text.dart';
import 'package:eztoverify_getting_started/generated/flavor/theme_constants.g.dart';
import 'package:eztoverify_getting_started/generated/l10n.dart';
import 'package:flutter/material.dart';

class HomeBodyWidget extends StatefulWidget {
  const HomeBodyWidget({super.key});

  @override
  State<HomeBodyWidget> createState() => _HomeBodyWidgetState();
}

class _HomeBodyWidgetState extends State<HomeBodyWidget>
    with TickerProviderStateMixin {
  late final AnimationController _controller;
  late final Animation<double> _opacityAnimation;

  @override
  void initState() {
    _controller = AnimationController(
      duration: const Duration(seconds: 2),
      vsync: this,
    );
    final CurvedAnimation curve =
        CurvedAnimation(parent: _controller, curve: Curves.easeOut);
    _opacityAnimation = Tween(
      begin: 1.0,
      end: 24.0,
    ).animate(curve);
    _controller.repeat(reverse: true);
    super.initState();
  }

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return LayoutBuilder(
      builder: (BuildContext context, BoxConstraints constraints) {
        bool isTablet =
            constraints.maxWidth > 600 && constraints.maxWidth < 800;
        return _buildTabletLayout(
            _opacityAnimation, _controller, isTablet, context);
      },
    );
  }
}

Widget _buildTabletLayout(Animation<double> opacityAnimation,
    AnimationController controller, bool isTablet, context) {
  return Column(
    mainAxisAlignment: MainAxisAlignment.start,
    crossAxisAlignment: CrossAxisAlignment.center,
    children: [
      SizedBox(
        height: isTablet
            ? MediaQuery.of(context).size.height * 0.1
            : MediaQuery.of(context).size.height * 0.04,
      ),
      Center(
        child: CustomText(
            text: S.current.home_page_waiting_for_verification,
            fontSize: isTablet ? 24 : 20,
            fontWeight: FontWeight.w500),
      ),
      Expanded(
        child: InkWell(
          borderRadius: BorderRadius.circular(100),
          child: AnimatedBuilder(
            animation: opacityAnimation,
            builder: (context, child) {
              return Ink(
                padding: isTablet
                    ? const EdgeInsets.all(35)
                    : const EdgeInsets.all(20),
                decoration: BoxDecoration(shape: BoxShape.circle, boxShadow: [
                  for (int i = 1; i <= 3; i++)
                    BoxShadow(
                      color: Color(ThemeConstants().rippleColor)
                          // ignore: deprecated_member_use
                          .withOpacity(controller.value / 2),
                      spreadRadius: opacityAnimation.value * i,
                    )
                ]),
                child: Padding(
                  padding: const EdgeInsets.symmetric(horizontal: 5),
                  child: SizedBox(
                    width: isTablet
                        ? MediaQuery.of(context).size.height * 0.125
                        : MediaQuery.of(context).size.height * 0.075,
                    height: isTablet
                        ? MediaQuery.of(context).size.height * 0.125
                        : MediaQuery.of(context).size.height * 0.075,
                    child: Image.asset(
                      ImageConstants.logoIconOnlyPng,
                    ),
                  ),
                ),
              );
            },
          ),
        ),
      ),
    ],
  );
}
