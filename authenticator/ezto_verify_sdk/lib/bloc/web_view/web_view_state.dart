part of 'web_view_cubit.dart';

@immutable
abstract class WebViewStates {}

class WebViewInitial extends WebViewStates {}

class WebViewLoading extends WebViewStates {}

class WebViewLoaded extends WebViewStates {}

class WebViewSuccess extends WebViewStates {}

class WebViewFailed extends WebViewStates {
  final ErrorCode errorCode;

  WebViewFailed({required this.errorCode});
}
