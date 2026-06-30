import 'package:eztoverify_getting_started/core/widgets/app_background_widget.dart';
import 'package:eztoverify_getting_started/core/widgets/appbar_widget.dart';
import 'package:eztoverify_getting_started/feature/home/ui/widgets/home_body_widget.dart';
import 'package:eztoverify_getting_started/feature/home/ui/widgets/home_drawer_widget.dart';
import 'package:eztoverify_getting_started/feature/home/ui/widgets/home_footer_widget.dart';
import 'package:flutter/material.dart';

class HomePage extends StatelessWidget {
  const HomePage({super.key});

  @override
  Widget build(BuildContext context) {
    return const AppBackgroundWidget(
      child: Scaffold(
        drawer: HomeDrawerWidget(),
        resizeToAvoidBottomInset: false,
        backgroundColor: Colors.transparent,
        body: SafeArea(
          child: Column(
            children: [
              AppbarWidget(),
              Expanded(child: HomeBodyWidget()),
              HomeFooterWidget()
            ],
          ),
        )
      ),
    );
  }
}
