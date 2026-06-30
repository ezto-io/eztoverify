abstract interface class SharedPreferencesService {
  Future<void> initialize();

  Future<void> setString(String key, String value);

  String getString(String key, {String defaultValue = ''});

  Future<void> setInt(String key, int value);

  int getInt(String key, {int defaultValue = 0});
}
