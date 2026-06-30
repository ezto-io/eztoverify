class ThemeService {
  int convertHexToInteger(String hexColor) {
    int r = int.parse(hexColor.substring(1, 3), radix: 16);
    int g = int.parse(hexColor.substring(3, 5), radix: 16);
    int b = int.parse(hexColor.substring(5, 7), radix: 16);

    int rgbInt = (r << 16) | (g << 8) | b;
    return rgbInt;
  }
}
