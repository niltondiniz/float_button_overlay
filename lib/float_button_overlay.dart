import 'dart:async';

import 'package:flutter/services.dart';

typedef String OnClickListener(String tag);

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

  static Future<bool> get openOverlay async {
    return await _channel.invokeMethod('openOverlay');
  }

  static Future<String> get closeOverlay async {
    final String retorno = await _channel.invokeMethod('closeOverlay');
    print("PlatformChannel returns: $retorno");
    return retorno;
  }

  ///

  /// Initialize
  /// iconPath: Path of image to set in ImageView of Float Button
  /// packageName: Package Name of the app you want to open on click
  /// activityName: Activity on your app that represents the main activity
  ///
  /// Two callbacks are defined:
  /// callback: Called on click, used to open the app
  /// seticon: Called on show float button, will set the image to ImageView of Float button

  static Future<bool> initialize(
      String iconPath, String packageName, String activityName) async {
    _channel.setMethodCallHandler((MethodCall call) {
      switch (call.method) {
        case "callback":
          {
            final Map<String, dynamic> params = <String, String>{
              'packageName': packageName,
              'activityName': activityName
            };
            print(params);
            return Future.value(params);
          }
          break;
        case "seticon":
          {
            final Map<String, dynamic> params = <String, String>{
              'iconPath': iconPath
            };
            print(params);
            return Future.value(params);
          }
          break;
      }
      return Future.value(true);
    });
    return true;
  }

  //Future...
  /*static Future<bool> registerClickToOpenApp(OnClickListener callBackFunction,
      String packageName, String activityName) async {
    _channel.setMethodCallHandler((MethodCall call) {
      switch (call.method) {
        case "callback":
          callBackFunction("");
          break;
      }
      final Map<String, dynamic> params = <String, String>{
        'packageName': packageName,
        'activityName': activityName
      };

      print(params);
      return Future.value(params);
    });
    return true;
  }*/

}
