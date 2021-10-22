import 'dart:async';

import 'package:flutter/services.dart';

typedef OnClickListener(String tag);

class FloatButtonOverlay {
  static const MethodChannel _channel =
      const MethodChannel('br.ndz.float_button_overlay');

  static Future<String> get platformVersion async {
    final String version = await _channel.invokeMethod('getPlatformVersion');
    return version;
  }

  static void get checkPermissions {
    _channel.invokeMethod('checkPermissions');
  }

  static Future<bool> openOverlay({
    String? iconPath,
    String? packageName,
    String? activityName,
    String? notificationText,
    String? notificationTitle,
    bool showTransparentCircle = true,
    int iconWidth = 150,
    int iconHeight = 150,
    int transpCircleWidth = 200,
    int transpCircleHeight = 200,
    String? wsRoom,
    String? wsUrl,
    String? driverId,
    String? recipientId,
    String? driverImageProfileUrl,
    String? driverName,
    String? acceptUrl,
    String? driverPositionUrl,
    String? driverPlate,
    String? driverCarModel,
  }) async {
    final Map<String, dynamic> params = <String, dynamic>{
      'packageName': packageName,
      'activityName': activityName,
      'iconPath': iconPath,
      'notificationTitle': notificationTitle,
      'notificationText': notificationText,
      'showTransparentCircle': showTransparentCircle,
      'iconWidth': iconWidth,
      'iconHeight': iconHeight,
      'transpCircleWidth': transpCircleWidth,
      'transpCircleHeight': transpCircleHeight,
      'wsRoom': wsRoom,
      'wsUrl': wsUrl,
      'driverId': driverId,
      'recipientId': recipientId,
      'driverImageProfileUrl': driverImageProfileUrl,
      'driverName': driverName,
      'acceptUrl': acceptUrl,
      'driverPositionUrl': driverPositionUrl,
      'driverPlate': driverPlate,
      'driverCarModel': driverCarModel,
    };
    return await _channel.invokeMethod('openOverlay', params);
  }

  static Future<String> get closeOverlay async {
    final String retorno = await _channel.invokeMethod('closeOverlay');
    print("PlatformChannel returns: $retorno");
    return retorno;
  }

  static Future<bool> registerCallback(OnClickListener callBackFunction,
      [OnClickListener? onClickCallback]) async {
    _channel.setMethodCallHandler(
      (MethodCall? call) async {
        switch (call!.method) {
          case "callback":
            callBackFunction('openOverlayCallback');
            break;
          case "onClickCallback":
            onClickCallback!("onClickCallback");
            break;
        }
      },
    );
    return true;
  }

  static Future<bool> openAppByPackage(String packageName) async {
    final Map<String, String> params = <String, String>{
      'packageName': packageName,
    };
    return await _channel.invokeMethod('openAppByPackage', params);
  }
}
