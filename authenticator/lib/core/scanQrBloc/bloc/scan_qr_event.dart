abstract class ScanQrEvent {}

class ScanQr extends ScanQrEvent {
  String? encryptionKey;
  ScanQr({this.encryptionKey});
}
