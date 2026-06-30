class EztoVerifyResult {}

class EztoVerifyCompleted implements EztoVerifyResult {}

class EztoVerifyFailed implements EztoVerifyResult {
  final String reason;
  EztoVerifyFailed({
    required this.reason,
  });
}

class EztoVerifyCancel implements EztoVerifyResult {
  CancelReason cancelReason;

  EztoVerifyCancel({required this.cancelReason});
}

enum CancelReason { byUser, newTransaction, other }
