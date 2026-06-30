import 'package:eztoverify/controller/ezto_verify.dart';
import 'package:eztoverify_getting_started/core/scanQrBloc/bloc/scan_qr_event.dart';
import 'package:eztoverify_getting_started/core/scanQrBloc/bloc/scan_qr_state.dart';
import 'package:eztoverify_getting_started/core/services/encryption_service/enc_key_service.dart';
import 'package:flutter_bloc/flutter_bloc.dart';

class ScanQrBloc extends Bloc<ScanQrEvent, ScanQrState> {
  ScanQrBloc() : super(ScanQrNotInitialized()) {
    on<ScanQr>(_onScanQr);
  }

  Future<void> _onScanQr(ScanQr event, Emitter<ScanQrState> emit) async {
    if (event.encryptionKey == null || event.encryptionKey!.isEmpty) {
      event.encryptionKey = await EncKeyService.getEncKey();
    }

    EztoVerify(encryptionKey: event.encryptionKey).scanQr();
  }
}
