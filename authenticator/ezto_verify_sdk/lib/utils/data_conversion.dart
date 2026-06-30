import 'dart:typed_data';
import 'package:convert/convert.dart';

class DataConversion {
  static Uint8List converHexToUint8List(String args) {
    List<int> intList = hex.decode(args);
    Uint8List result = Uint8List.fromList(intList);
    return result;
  }

  static String convertUint8ListToHex(Uint8List args) {
    Uint8List uint8List = Uint8List.fromList(args);
    String result = hex.encode(uint8List);
    return result;
  }
}
