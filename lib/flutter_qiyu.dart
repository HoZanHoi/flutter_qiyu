import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

class FlutterQiyu {
  // static Stream<dynamic> _pluginResp;

  static const MethodChannel _channel = const MethodChannel('flutter_qiyu');
  static const EventChannel _eventChannel =
      const EventChannel('flutter_qiyu_event');

  static Future<bool> init({@required String key}) async{
    final bool ret = await _channel.invokeMethod('init', {"appKey": key});
    return ret;
  }

  static Future<bool> setInfo({@required String id,@required  String name,@required  String phone,@required  String token,@required  String avatar}) async{
    final bool ret = await _channel.invokeMethod('setInfo', {"id": id,"name":name,"phone":phone,"token":token,"avatar":avatar});
    return ret;
  }

  static Future<bool> logout() async{
    final bool ret = await _channel.invokeMethod('logou');
    return ret;
  }

  static Future<bool> clearCache() async{
    final bool ret = await _channel.invokeMethod('clearCache');
    return ret;
  }
  
  static Future<bool> openService({@required String url, @required String title, int staffId, int groupId, int robotId, int vipLevel = 11}) async{
    Map<String,dynamic> params = {
      "fromUri":url, 
      "fromTitle":title,
      "vipLevel":vipLevel
    };
    if(staffId != null ) params['staffId'] = staffId;
    if(groupId != null ) params['groupId'] = groupId;
    if(robotId != null ) params['robotId'] = robotId;

    final bool ret = await _channel.invokeMethod('openService',params);
    return ret;
  }

  static Future<bool> setProductDetailAndOpen() async{
    final bool ret = await _channel.invokeMethod('openService');
    return ret;
  }
  
  static Future<int> getUnreadCount() async{
    final int ret = await _channel.invokeMethod('getUnreadCount');
    return ret;
  }  

  //TODO
  static Future<bool> uiCustomization() async{
    final bool ret = await _channel.invokeMethod('UICustomization');
    return ret;
  }             

  static Stream<dynamic> get pluginResp {
    // if (_pluginResp == null) {
      return _eventChannel.receiveBroadcastStream();
    // }
    // return _pluginResp;
  }
}
