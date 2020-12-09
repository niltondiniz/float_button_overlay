import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

typedef String OnClickListener(String tag);

class FloatButtonOverlay {
  static const MethodChannel _channel =
      const MethodChannel('com.example.float_button_overlay');

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
    print("Retorno do platformChannel: $retorno");
    return retorno;
  }

  static Future<bool> registerClickToOpenApp(
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
    });
    return true;
  }

  static Future<bool> setIcon(String iconPath) async {
    _channel.setMethodCallHandler((MethodCall call) {
      switch (call.method) {
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
    });
    return true;
  }

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
