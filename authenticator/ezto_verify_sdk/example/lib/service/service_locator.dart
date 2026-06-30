import 'package:eztoverify_internal/service/push_factory.dart';
import 'package:eztoverify_internal/service/push_service_interface.dart';
import 'package:get_it/get_it.dart';

final sl = GetIt.instance;
Future<void> initSl() async {
  final push = await PushFactory.getInstance();
  sl.registerSingleton<PushServiceInterface>(push);
}
