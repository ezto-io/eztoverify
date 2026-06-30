part of 'qr_scanner_cubit.dart';

@immutable
abstract class QrScannerState {}

final class QrScannerInitial extends QrScannerState {}

final class QrScannerSuccess extends QrScannerState {}

final class QrScannerFailure extends QrScannerState {}
