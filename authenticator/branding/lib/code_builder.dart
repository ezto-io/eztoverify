// ignore_for_file: depend_on_referenced_packages

import 'package:branding/models/flavor_model.dart';
import 'package:branding/service/file_service.dart';

// ignore: implementation_imports
import 'package:built_collection/src/list.dart';
import 'package:code_builder/code_builder.dart';

class CodeBuilder {
  final FlavorModel flavor;
  final FileService fileService;

  const CodeBuilder({
    required this.flavor,
    required this.fileService,
  });

  Future<bool> buildUrlConstants() async {
    print("**********");
    print("Generating Url Constants code");
    final privacyPolicy = Field(
      (f) => f
        ..name = "privacyPolicy"
        ..modifier = FieldModifier.final$
        ..assignment = literalString(flavor.appInfo.urls.privacy).code,
    );
    final termsAndConditions = Field(
      (f) => f
        ..name = "termsAndConditions"
        ..modifier = FieldModifier.final$
        ..assignment = literalString(flavor.appInfo.urls.terms).code,
    );
    final windowsUpdate = Field(
      (f) => f
        ..name = "windowsUpdate"
        ..modifier = FieldModifier.final$
        ..assignment = literalString(flavor.appInfo.urls.windowsUpdate).code,
    );
    final macUpdate = Field(
      (f) => f
        ..name = "macUpdate"
        ..modifier = FieldModifier.final$
        ..assignment = literalString(flavor.appInfo.urls.macUpdate).code,
    );
    final appConstants = Class((b) => b
      ..name = "UrlConstants"
      ..fields = ListBuilder<Field>([
        privacyPolicy,
        termsAndConditions,
        windowsUpdate,
        macUpdate,
      ]));
    final emitter = DartEmitter(
      useNullSafetySyntax: true,
    );
    final content = appConstants.accept(emitter).toString();
    await fileService.saveToFile(
      "./../lib/generated/flavor/url_generated.g.dart",
      content,
    );
    return true;
  }

  Future<bool> buildThemeConstants() async {
    print("**********");
    print("Generating Theme Constants code");
    final primaryColor = Field(
      (f) => f
        ..name = "primaryColor"
        ..modifier = FieldModifier.final$
        ..assignment = literal(flavor.theme.primaryColor).code,
    );
    final whiteColor = Field(
      (f) => f
        ..name = "whiteColor"
        ..modifier = FieldModifier.final$
        ..assignment = Code(flavor.theme.whiteColor.toString()),
    );

    final primaryTextColor = Field(
      (f) => f
        ..name = "primaryTextColor"
        ..modifier = FieldModifier.final$
        ..assignment = literalNum(flavor.theme.primaryTextColor).code,
    );

    final cardBgColor = Field(
      (f) => f
        ..name = "cardBgColor"
        ..modifier = FieldModifier.final$
        ..assignment = literalNum(flavor.theme.cardBgColor).code,
    );

    final cardStrokeColor = Field(
      (f) => f
        ..name = "cardStrokeColor"
        ..modifier = FieldModifier.final$
        ..assignment = literalNum(flavor.theme.cardStrokeColor).code,
    );

    final rippleColor = Field(
      (f) => f
        ..name = "rippleColor"
        ..modifier = FieldModifier.final$
        ..assignment = literalNum(flavor.theme.rippleColor).code,
    );

    final dividerColor = Field(
      (f) => f
        ..name = "dividerColor"
        ..modifier = FieldModifier.final$
        ..assignment = literalNum(flavor.theme.dividerColor).code,
    );

    final fontName = Field(
      (f) => f
        ..name = "fontName"
        ..modifier = FieldModifier.final$
        ..assignment = literalString(flavor.theme.fontName).code,
    );

    final appConstants = Class((b) => b
      ..name = "ThemeConstants"
      ..fields = ListBuilder<Field>(
        [
          primaryColor,
          whiteColor,
          primaryTextColor,
          cardBgColor,
          cardStrokeColor,
          rippleColor,
          dividerColor,
          fontName,
        ],
      ));
    final emitter = DartEmitter(
      useNullSafetySyntax: true,
    );
    final content = appConstants.accept(emitter).toString();
    await fileService.saveToFile(
      "./../lib/generated/flavor/theme_constants.g.dart",
      content,
    );
    return true;
  }

  Future<bool> buildAppConstants() async {
    print("**********");
    print("Generating App Constants code");
    final mobileEnc = Field(
      (f) => f
        ..name = "mobileEnc"
        ..modifier = FieldModifier.final$
        ..assignment = literalString(flavor.appInfo.encryption.mobileKey).code,
    );
    final desktopEnc = Field(
      (f) => f
        ..name = "desktopEnc"
        ..modifier = FieldModifier.final$
        ..assignment = literalString(flavor.appInfo.encryption.desktopKey).code,
    );
    final isProd = Field(
      (f) => f
        ..name = "isProd"
        ..modifier = FieldModifier.final$
        ..assignment = literalBool(flavor.appInfo.isProd).code,
    );
    final isCopyAuthCode = Field(
      (f) => f
        ..name = "isCopyAuthCode"
        ..modifier = FieldModifier.final$
        ..assignment = literalBool(flavor.appInfo.isCopyAuthCode).code,
    );
    final isAutoTest = Field(
      (f) => f
        ..name = "isAutoTest"
        ..modifier = FieldModifier.final$
        ..assignment = literalBool(flavor.appInfo.isAutoTest).code,
    );
    final appConstants = Class((b) => b
      ..name = "GeneratedConstants"
      ..fields = ListBuilder<Field>(
        [
          mobileEnc,
          desktopEnc,
          isProd,
          isCopyAuthCode,
          isAutoTest,
        ],
      ));
    final emitter = DartEmitter(
      useNullSafetySyntax: true,
    );
    final content = appConstants.accept(emitter).toString();
    await fileService.saveToFile(
      "./../lib/generated/flavor/generated_constants.g.dart",
      content,
    );
    return true;
  }
}
