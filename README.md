# float_button_overlay

A Flutter plugin to keep app service background visible like a Float Action Button

##Example of Float Button Overlay

![example](https://raw.githubusercontent.com/niltondiniz/float_button_overlay/main/example/assets/example.gif "example")

## Getting Started

1- import 'package:float_button_overlay/float_button_overlay.dart';

2- On Main.dart main method:

    void main() async {
        WidgetsFlutterBinding.ensureInitialized();  
        FloatButtonOverlay.initialize("/path/to/icon", packageInfo.packageName, "MainActivity");
        runApp(MyApp());
    }

3- Show/Hide Float Button

    -Show: FloatButtonOverlay.openOverlay;
    -Hide: FloatButtonOverlay.closeOverlay;

This project is a starting point for a Flutter
[plug-in package](https://flutter.dev/developing-packages/),
a specialized package that includes platform-specific implementation code for
Android and/or iOS.

For help getting started with Flutter, view our
[online documentation](https://flutter.dev/docs), which offers tutorials,
samples, guidance on mobile development, and a full API reference.

