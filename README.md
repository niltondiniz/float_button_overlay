# float_button_overlay

A Flutter plugin to keep app service background visible like a Float Action Button

## Example of Float Button Overlay

![example](https://raw.githubusercontent.com/niltondiniz/float_button_overlay/main/example/assets/example_3.gif "example")

## Getting Started

1- import 'package:float_button_overlay/float_button_overlay.dart';

2- call CheckPermissions method

    @override
    void initState() {
      super.initState();
      initPlatformState();
    }

    Future<void> initPlatformState() async {
      try {
        FloatButtonOverlay.checkPermissions;
      } on PlatformException {}
    }

3- On Main.dart main method:

    void main() async {
        WidgetsFlutterBinding.ensureInitialized();          
        runApp(MyApp());
    }

4- Show/Hide Float Button

  The openOverlay method sets: notificationTitle, notificationText, iconPath, packageName and activityName

    -Show: FloatButtonOverlay.openOverlay(
                      activityName: 'MainActivity',
                      iconPath: file.path,
                      notificationText: "Float Button Overlay ☠️",
                      notificationTitle: 'Float Button Overlay ☠️',
                      packageName: packageInfo.packageName);
    -Hide: FloatButtonOverlay.closeOverlay;

This project is a starting point for a Flutter
[plug-in package](https://flutter.dev/developing-packages/),
a specialized package that includes platform-specific implementation code for
Android and/or iOS.

For help getting started with Flutter, view our
[online documentation](https://flutter.dev/docs), which offers tutorials,
samples, guidance on mobile development, and a full API reference.

