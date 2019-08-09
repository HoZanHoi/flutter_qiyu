import 'package:flutter/services.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:flutter_qiyu/flutter_qiyu.dart';

void main() {
  const MethodChannel channel = MethodChannel('flutter_qiyu');

  setUp(() {
    channel.setMockMethodCallHandler((MethodCall methodCall) async {
      return true;
    });
  });

  tearDown(() {
    channel.setMockMethodCallHandler(null);
  });

  test('getPlatformVersion', () async {
    expect(await FlutterQiyu.init(key: 'adfadfsdgasdfsd'), true);
  });
}
