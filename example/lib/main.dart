import 'package:flutter/material.dart';
import 'dart:async';

import 'package:flutter/services.dart';
import 'package:flutter_qiyu/flutter_qiyu.dart';

void main() => runApp(MyApp());

class MyApp extends StatefulWidget {
  @override
  _MyAppState createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  String _platformVersion = 'Unknown';

  StreamSubscription subScription;

  @override
  void initState() {
    super.initState();
    initPlatformState();
    subScription = FlutterQiyu.pluginResp.listen((Object event){
      print(event);
    }, onError: (Object error){
      print(error);
    }, onDone: (){

    });
  }

  @override
  void dispose() {
    subScription.cancel();
    super.dispose();
  }
  // Platform messages are asynchronous, so we initialize in an async method.
  Future<void> initPlatformState() async {
    String platformVersion;
    // Platform messages may fail, so we use a try/catch PlatformException.
    // try {
    //   platformVersion = await FlutterQiyu.platformVersion;
    // } on PlatformException {
    //   platformVersion = 'Failed to get platform version.';
    // }
    try {
      await FlutterQiyu.init(key: "ba7e10fd6f381e6f3104e4a2687cd87e");
    } on PlatformException {
      print('Init QiYu Error');
    }

    try{
      await FlutterQiyu.setInfo(id: '1',name: 'abc',phone: '12345678901',avatar: 'aaa.png',token: '88ijfidag23234234');
    }on PlatformException {
      print('set QiYu user info Error');
    }    

    try{
      await FlutterQiyu.openService(url: 'abc', title: 'test');
    }on PlatformException {
      print('open QiYu service Error');
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
          title: const Text('Plugin example app'),
        ),
        body: Center(
          child: Text('Running on: $_platformVersion\n'),
        ),
      ),
    );
  }
}
