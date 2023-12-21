import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';

import 'easebuzz_platform_interface.dart';

/// An implementation of [EasebuzzPlatform] that uses method channels.
class MethodChannelEasebuzz extends EasebuzzPlatform {
  /// The method channel used to interact with the native platform.
  @visibleForTesting
  final methodChannel = const MethodChannel('easebuzz');

  @override
  Future<String?> getPlatformVersion() async {
    final version =
        await methodChannel.invokeMethod<String>('getPlatformVersion');
    return version;
  }

  @override
  Future<Map<String, Object>?> openPaymentGateway(
      {required Map<String, dynamic> arguments}) async {
    Map<String, Object>? paymentResponse = await methodChannel
        .invokeMapMethod<String, Object>("easebuzz", arguments);

    return paymentResponse;
  }
}
