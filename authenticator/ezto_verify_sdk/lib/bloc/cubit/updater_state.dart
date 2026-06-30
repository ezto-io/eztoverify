part of 'updater_cubit.dart';

@immutable
sealed class UpdaterState {}

final class UpdaterInitial extends UpdaterState {}

final class UpdaterUpdateAvailable extends UpdaterState {
  final UpdateType updateType;
  final Upgrader? upgrader;

  UpdaterUpdateAvailable({
    this.upgrader,
    required this.updateType,
  });
}

final class UpdaterUpdateNotAvailable extends UpdaterState {}

enum UpdateType {
  appStore,
  playStore,
  appCast,
}
