import 'dart:io';

import 'package:eztoverify/model/force_update_model.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter_bloc/flutter_bloc.dart';
import 'package:in_app_update/in_app_update.dart';
import 'package:upgrader/upgrader.dart';
import 'package:url_launcher/url_launcher_string.dart';

part 'updater_state.dart';

class UpdaterCubit extends Cubit<UpdaterState> {
  UpdaterCubit() : super(UpdaterInitial());

  Future<void> doUpdate() async {
    if (state is UpdaterUpdateAvailable) {
      final updaterState = state as UpdaterUpdateAvailable;
      switch (updaterState.updateType) {
        case UpdateType.appStore:
          updaterState.upgrader?.sendUserToAppStore();
          break;
        case UpdateType.playStore:
          await InAppUpdate.checkForUpdate();
          InAppUpdate.performImmediateUpdate();
          break;
        case UpdateType.appCast:
          final url = updaterState.upgrader?.versionInfo?.appStoreListingURL;
          if (url != null) {
            await launchUrlString(url);
          }
      }
    }
  }

  Future<void> checkForUpdates({
    required ForceUpdate forceUpdate,
  }) async {
    emit(UpdaterInitial());

    if (kIsWeb) {
      return;
    }

    if (Platform.isAndroid && !forceUpdate.android.shouldForceUpdate) {
      emit(UpdaterUpdateNotAvailable());
      return;
    } else if (Platform.isIOS && !forceUpdate.ios.shouldForceUpdate) {
      emit(UpdaterUpdateNotAvailable());
      return;
    } else if (Platform.isMacOS) {
      if (!forceUpdate.mac.shouldForceUpdate || forceUpdate.mac.url == null) {
        emit(UpdaterUpdateNotAvailable());
        return;
      }
    } else if (Platform.isWindows) {
      if (!forceUpdate.windows.shouldForceUpdate ||
          forceUpdate.windows.url == null) {
        emit(UpdaterUpdateNotAvailable());
        return;
      }
    }

    if (Platform.isAndroid && forceUpdate.android.url == null) {
      try {
        final updateInfo = await InAppUpdate.checkForUpdate();
        if (updateInfo.updateAvailability ==
            UpdateAvailability.updateAvailable) {
          emit(UpdaterUpdateAvailable(
            updateType: UpdateType.playStore,
          ));
        } else {
          emit(UpdaterUpdateNotAvailable());
        }
      } catch (e) {
        emit(UpdaterUpdateNotAvailable());
      }
    } else {
      if (kDebugMode) {
        await Upgrader.clearSavedSettings();
      }
      final upgrader = Upgrader(
        debugLogging: kDebugMode,
        storeController: UpgraderStoreController(
          oniOS: () => UpgraderAppStore(),
          onMacOS: () => UpgraderAppcastStore(
            appcastURL: forceUpdate.mac.url!,
          ),
          onWindows: () => UpgraderAppcastStore(
            appcastURL: forceUpdate.windows.url!,
          ),
        ),
      );
      await upgrader.initialize();

      final isUpdateAvailable = upgrader.isUpdateAvailable();
      if (isUpdateAvailable) {
        emit(UpdaterUpdateAvailable(
          upgrader: upgrader,
          updateType: Platform.isIOS ? UpdateType.appStore : UpdateType.appCast,
        ));
      } else {
        emit(UpdaterUpdateNotAvailable());
      }
    }
  }
}
