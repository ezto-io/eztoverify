import 'package:eztoverify_getting_started/constants/image_constants.dart';
import 'package:eztoverify_getting_started/core/locator/locator.dart';
import 'package:eztoverify_getting_started/core/services/package_info/package_info_service.dart';
import 'package:eztoverify_getting_started/core/widgets/app_background_widget.dart';
import 'package:eztoverify_getting_started/core/widgets/appbar_widget.dart';
import 'package:eztoverify_getting_started/feature/about/ui/widgets/card_widget.dart';
import 'package:eztoverify_getting_started/feature/about/ui/widgets/encryption_bottomsheet.dart';
import 'package:eztoverify_getting_started/feature/about/ui/widgets/policy_links.dart';
import 'package:eztoverify_getting_started/generated/flavor/generated_constants.g.dart';
import 'package:flutter/material.dart';
import '../../../../core/services/device_info/device_info_service.dart';
import '../../ui/widgets/about_footer_widget.dart';

class AboutPage extends StatefulWidget {
  const AboutPage({
    super.key,
  });

  @override
  State<AboutPage> createState() => _AboutPageState();
}

class _AboutPageState extends State<AboutPage> {
  final TextEditingController textController = TextEditingController();
  final TextEditingController socketTextController = TextEditingController();

  @override
  void dispose() {
    textController.dispose();
    socketTextController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return AppBackgroundWidget(
      child: Scaffold(
        backgroundColor: Colors.transparent,
        body: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const AppbarWidget(
              icon: ImageConstants.goBackSvg,
            ),
            Expanded(
              child: Padding(
                padding: const EdgeInsets.fromLTRB(50, 12, 50, 12),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.stretch,
                  children: [
                    SizedBox(
                      height: MediaQuery.of(context).size.height * 0.06,
                    ),
                    CardWidget(
                      title: 'Device Model',
                      content: getIt<DeviceInfoServices>().getDeviceModel(),
                    ),
                    Padding(
                      padding: EdgeInsets.only(
                          top: MediaQuery.of(context).size.height * 0.02),
                      child: CardWidget(
                        title: 'OS Version',
                        content: getIt<DeviceInfoServices>().getDeviceVersion(),
                      ),
                    ),
                    Padding(
                      padding: EdgeInsets.only(
                          top: MediaQuery.of(context).size.height * 0.02),
                      child: buildAppVersionWidget(context),
                    ),
                    Padding(
                      padding: EdgeInsets.only(
                          top: MediaQuery.of(context).size.height * 0.05),
                      child: const PolicyLinksWidget(),
                    ),
                  ],
                ),
              ),
            ),
            const AboutFooterWidget()
          ],
        ),
      ),
    );
  }

  Widget buildAppVersionWidget(BuildContext context) {
    int count = 0;
    return GestureDetector(
      child: FutureBuilder(
          future: getIt<PackageInfoServices>().getAppInfo(),
          builder: (context, snapshot) {
            if (snapshot.hasData) {
              return CardWidget(
                  title: 'App Version',
                  content:
                      '${snapshot.data!.version} (${snapshot.data!.buildNumber})');
            } else {
              return const CardWidget(title: 'App Version', content: '1.0.0');
            }
          }),
      onTap: () {
        count++;
        if (count == 5) {
          count = 0;
          if (!GeneratedConstants().isProd) {
            showEncryptionSheet(context);
          }
        }
      },
    );
  }

  Future<void> showEncryptionSheet(BuildContext context) async {
    await showModalBottomSheet(
      context: context,
      isDismissible: false,
      isScrollControlled: true,
      builder: (BuildContext builderContext) {
        return SingleChildScrollView(
            child: EncryptionBottomsheet(
          textController: textController,
          socketTextController: socketTextController,
        ));
      },
    );
  }
}
