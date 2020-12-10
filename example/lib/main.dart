import 'dart:io';

import 'package:float_button_overlay/float_button_overlay.dart';
import 'package:flutter/material.dart';
import 'dart:async';

import 'package:flutter/services.dart';
import 'package:package_info/package_info.dart';
import 'package:path_provider/path_provider.dart';

Future<File> getImageFileFromAssets(String path) async {
  final byteData = await rootBundle.load('assets/$path');

  final file = File('${(await getTemporaryDirectory()).path}/$path');
  await file.writeAsBytes(byteData.buffer
      .asUint8List(byteData.offsetInBytes, byteData.lengthInBytes));

  return file;
}

Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();

  PackageInfo packageInfo = await PackageInfo.fromPlatform();

  var file = await getImageFileFromAssets('ic_floatbutton.jpg');
  FloatButtonOverlay.initialize(
      file.path, packageInfo.packageName, "MainActivity");

  runApp(MyApp());
}

class MyApp extends StatefulWidget {
  @override
  _MyAppState createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  String _platformVersion = 'Unknown';

  @override
  void initState() {
    super.initState();
    initPlatformState();
  }

  // Platform messages are asynchronous, so we initialize in an async method.
  Future<void> initPlatformState() async {
    String platformVersion;
    // Platform messages may fail, so we use a try/catch PlatformException.
    try {
      FloatButtonOverlay.checkPermissions;
      platformVersion = await FloatButtonOverlay.platformVersion;
    } on PlatformException {
      platformVersion = 'Failed to get platform version.';
    }

    // If the widget was removed from the tree while the asynchronous platform
    // message was in flight, we want to discard the reply rather than calling
    // setState to update our non-existent appearance.
    if (!mounted) return;

    setState(() {
      _platformVersion = platformVersion;
    });
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(
          title: const Text('Float Button Overlay - Example'),
        ),
        body: Center(
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            crossAxisAlignment: CrossAxisAlignment.center,
            children: [
              Text('Running on: $_platformVersion\n'),
              FlatButton(
                onPressed: () {
                  debugPrint("Vai abrir o overlay");
                  FloatButtonOverlay.openOverlay;
                },
                child: Container(
                  color: Colors.green,
                  height: 40,
                  width: 100,
                  child: Center(
                    child: Text(
                      "Show Button",
                    ),
                  ),
                ),
              ),
              FlatButton(
                onPressed: () {
                  debugPrint("Vai fechar o overlay");
                  FloatButtonOverlay.closeOverlay;
                },
                child: Container(
                  color: Colors.black54,
                  height: 40,
                  width: 100,
                  child: Center(
                    child: Text(
                      "Close Button",
                    ),
                  ),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
