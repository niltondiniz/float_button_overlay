import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:float_button_overlay/float_button_overlay.dart';

void main() {
  const MethodChannel channel = MethodChannel('float_button_overlay');

  TestWidgetsFlutterBinding.ensureInitialized();

  setUp(() {
    channel.setMockMethodCallHandler((MethodCall methodCall) async {
      return '42';
    });
  });

  tearDown(() {
    channel.setMockMethodCallHandler(null);
  });

  test('getPlatformVersion', () async {
    expect(await FloatButtonOverlay.platformVersion, '42');
  });
}
