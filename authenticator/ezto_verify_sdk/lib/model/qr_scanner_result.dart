enum QrStatus {
  success,
  failed,
  cancelled,
  encryptionKeyError,
}

class QrScannerResult {
  final Map<String, dynamic>? data;
  final QrStatus status;

  QrScannerResult({
    this.data,
    required this.status,
  });
}
