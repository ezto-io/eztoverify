abstract class ScanQrState {}

class ScanQrNotInitialized extends ScanQrState {}

class ScanningQr extends ScanQrState {}

class ScanningQrCompleted extends ScanQrState {}
