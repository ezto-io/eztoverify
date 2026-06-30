import 'dart:io';
import 'package:eztoverify_getting_started/constants/image_constants.dart';
import 'package:eztoverify_getting_started/core/scanQrBloc/bloc/scan_qr_bloc.dart';
import 'package:eztoverify_getting_started/core/scanQrBloc/bloc/scan_qr_event.dart';
import 'package:eztoverify_getting_started/core/widgets/app_background_widget.dart';
import 'package:eztoverify_getting_started/core/widgets/appbar_widget.dart';
import 'package:eztoverify_getting_started/core/widgets/custom_text.dart';
import 'package:eztoverify_getting_started/generated/flavor/theme_constants.g.dart';
import 'package:flutter/material.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:flutter_svg/svg.dart';

class HomeDrawerWidget extends StatelessWidget {
  const HomeDrawerWidget({super.key});

  @override
  Widget build(BuildContext context) {
    return AppBackgroundWidget(
      child: Container(
        decoration: BoxDecoration(
          color: Color(ThemeConstants().whiteColor),
          image: const DecorationImage(
              image: AssetImage(ImageConstants.homeBackgroundPng),
              fit: BoxFit.cover),
        ),
        child: Drawer(
          child: Column(
            children: [
              const AppbarWidget(
                icon: ImageConstants.closeSvg,
                fontSize: 15,
                padding: 10,
              ),
              SizedBox(
                height: MediaQuery.of(context).size.height * 0.03,
              ),
              Expanded(
                child: ListView(
                  padding: const EdgeInsets.only(left: 20),
                  children: <Widget>[
                    SizedBox(
                      height: MediaQuery.of(context).size.height * 0.03,
                    ),
                    if (Platform.isAndroid || Platform.isIOS)
                      menuItem(
                        'Scan',
                        SvgPicture.asset(
                          ImageConstants.scanMenuSvg,
                          height: 22,
                          width: 22,
                        ),
                        () {
                          Navigator.pop(context);
                          context.read<ScanQrBloc>().add(ScanQr());
                        },
                      ),
                    menuItem(
                      'About',
                      null,
                      () {
                        Navigator.pop(context);
                        Navigator.pushNamed(context, "/about");
                      },
                    ),
                  ],
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }

  Widget menuItem(String text, Widget? icon, Function() onTap) {
    return ListTile(
      leading: icon ?? const Icon(Icons.info_outline_rounded),
      title: CustomText(
        text: text,
        fontSize: 20,
        fontWeight: FontWeight.normal,
      ),
      onTap: onTap,
    );
  }
}
