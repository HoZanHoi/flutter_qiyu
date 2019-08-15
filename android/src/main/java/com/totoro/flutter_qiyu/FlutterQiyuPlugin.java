package com.totoro.flutter_qiyu;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import android.util.Log;

import io.flutter.plugin.common.EventChannel;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry.Registrar;
import com.qiyukf.unicorn.api.*;
import com.qiyukf.unicorn.api.customization.action.AlbumAction;
import com.qiyukf.unicorn.api.customization.action.BaseAction;
import com.qiyukf.unicorn.api.customization.action.CameraAction;
import com.qiyukf.unicorn.api.customization.action.ImageAction;
import com.qiyukf.unicorn.api.customization.input.InputPanelOptions;
import com.qiyukf.unicorn.api.msg.ProductReslectOnclickListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/** FlutterQiyuPlugin */
public class FlutterQiyuPlugin implements MethodCallHandler, EventChannel.StreamHandler {

  private Context context;

  private BroadcastReceiver sendRespReceiver;

  private int unreadCount = 0;

  private ApplicationInfo appInfo;

  static String TAG = "FlutterQiYuPlugin";

  private boolean initialized = false;

  private YSFOptions option;

  /** Plugin registration. */
  public static void registerWith(Registrar registrar) {
    final MethodChannel channel = new MethodChannel(registrar.messenger(), "flutter_qiyu");
    final EventChannel eventChannel = new EventChannel(registrar.messenger(), "flutter_qiyu_event");
    FlutterQiyuPlugin plugin = new FlutterQiyuPlugin(registrar.context());
    channel.setMethodCallHandler(plugin);
    eventChannel.setStreamHandler(plugin);
  }

  FlutterQiyuPlugin(Context context){
    this.context = context;
    this.appInfo = context.getApplicationInfo();
  }

  @Override
  public void onMethodCall(MethodCall call, Result result) {
    Log.d(TAG,"on method called!");
    Log.d(TAG,call.method);
    if (call.method.equals("init")) {
      String appKey = call.argument("appKey");
      if(appKey == null) {
        Log.d("FlutterQiYuPlugin", "QiYu init failed, appKey cannot be null");
        result.error("error", "QiYu init failed, appKey cannot be null", null);
      }else{
        this.initialized = this.initService(appKey);
        result.success(this.initialized);
      }
    } else if(call.method.equals("setInfo")){
      if(!this.initialized)
        result.error("SetInfo Error","QiYu not initialized","");
      else{
        String name = call.argument("name");
        String phone = call.argument("phone");
        String userId = call.argument("id");
        String token = call.argument("token");
        String avatar = call.argument("avatar");
        YSFUserInfo userInfo = new YSFUserInfo();
//        userInfo.userId = id;
//        userInfo.authToken = token;
        userInfo.data = "[" +
                "{\"key\":\"real_name\",\"value\":\""+ name + "\"},"+
                "{\"key\":\"mobile_phone\",\"value\":\""+ phone + "\"},"+
                "{\"key\":\"avatar\",\"value\":\""+ avatar + "\"},"+
                "{\"key\":\"userId\",\"label\":\"userId\",\"value\":\""+ userId + "\"},"+
                "{\"key\":\"token\",\"label\":\"token\",\"value\":\""+ token + "\"},"+
                "]";
        boolean ret = Unicorn.setUserInfo(userInfo);
        result.success(ret);
      }
    } else if(call.method.equals("logout")){
      if(!this.initialized)
        result.error("Logout Error","QiYu not initialized","");
      else{
        Unicorn.logout();
        result.success(true);
      }
    } else if(call.method.equals("clearCache")) {
      if(!this.initialized)
        result.error("ClearCache Error","QiYu not initialized","");
      else{
        Unicorn.clearCache();
        result.success(true);
      }
    } else if(call.method.equals("openService")) {
      if(!this.initialized)
        result.error("OpenService Error","QiYu not initialized","");
      else{
        String uri = call.argument("fromUri");
        String title = call.argument("fromTitle");
        Integer staffId = call.argument("staffId");
        Integer groupId = call.argument("groupId");
        Integer robotId = call.argument("robotId");
        Integer vipLevel = call.argument("vipLevel");

        ConsultSource cs = new ConsultSource(uri, title, "");
        if(staffId != null) cs.staffId = staffId;
        if(groupId != null) cs.groupId = groupId;
        if(robotId != null) cs.robotId = robotId;
        if(vipLevel != null) cs.vipLevel = vipLevel;

        ProductDetail.Builder pdb = makeProductDetail(call);
        if(pdb != null){
          cs.productDetail = pdb.build();
        }
//      cs.robotFirst = true;
        Unicorn.openServiceActivity(this.context,"咨询客服",cs);
        result.success(true);
      }
    }  else if(call.method.equals("getUnreadCount")) {
      if(!this.initialized)
        result.error("GetUnreadCount Error","QiYu not initialized","");
      else{
        unreadCount = Unicorn.getUnreadCount();
        result.success(unreadCount);
      }
    } else if(call.method.equals("uiCustomization")) {
      if(!this.initialized)
        result.error("UICustomization Error","QiYu not initialized","");
      else{
        this.uiCustomization(call);
        result.success(true);
      }
    } else if(call.method.equals("inputPanelCustomization")) {
      if(!this.initialized)
        result.error("InputPanelCustomization Error","QiYu not initialized","");
      else{
        this.inputPanelCustomization(call);
        result.success(true);
      }
    } else if(call.method.equals("titleBarConfig")) {
      if(!this.initialized)
        result.error("TitleBarConfig Error","QiYu not initialized","");
      else{

      }
    } else {
      result.notImplemented();
    }
  }

  private YSFOptions options() {
    this.option = new YSFOptions();
    this.option.statusBarNotificationConfig = new StatusBarNotificationConfig();
    this.option.statusBarNotificationConfig.contentTitle = "在线客服";
    return this.option;
  }

  private boolean initService(String appKey) {
    boolean ret = Unicorn.init(this.context, appKey, options(), new GlideImageLoader(this.context));

    if(ret){
      Unicorn.addUnreadCountChangeListener(new UnreadCountChangeListener() {
        @Override
        public void onUnreadCountChange(int count) {
          unreadCount = count;
          Intent intent = new Intent();
          intent.setAction("com.totoro.flutterQiYu.QiYuUnreadCount");
          LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(context);
          localBroadcastManager.sendBroadcast(intent);
        }
      }, true);
    }
    return ret;
  }

  private ProductDetail.Builder makeProductDetail(MethodCall call){
    String productTitle = call.argument("productTitle");
    String productDesc = call.argument("productDesc");
    String productNote = call.argument("productNote");
    String productPic = call.argument("productPic");
    String productUrl = call.argument("productUrl");
    Integer productShow = call.argument("productShow");
    Boolean productAlwaysSend = call.argument("productAlwaysSend");
    List<HashMap> productTags = call.argument("productTags");
    Boolean productOpenTemplate = call.argument("productOpenTemplate");
    Boolean productSendByUser = call.argument("productSendByUser");
    String productActionText = call.argument("productActionText");
    Integer productActionTextColor = call.argument("productActionTextColor");
    Boolean productIsOpenReselect = call.argument("productIsOpenReselect");
    String productReselectText = call.argument("productReselectText");
    String productHandlerTag = call.argument("productHandlerTag");
    if(productTitle != null){
      ProductDetail.Builder pdb = new ProductDetail.Builder().setTitle(productTitle);
      if( productDesc != null ) pdb.setDesc(productDesc);
      if( productNote != null ) pdb.setNote(productNote);
      if( productPic != null ) pdb.setPicture(productPic);
      if( productUrl != null ) pdb.setUrl(productUrl);
      if( productShow != null ) pdb.setShow(productShow);
      if( productAlwaysSend != null ) pdb.setAlwaysSend(productAlwaysSend);
      if(productTags != null ){
        List<ProductDetail.Tag> tags = new ArrayList();
        for (HashMap item : productTags) {
          String label = (String)item.get("label");
          String url = (String)item.get("url");
          String focusIframe = (String)item.get("focusIframe");
          String data = (String)item.get("data");
          ProductDetail.Tag tag = new ProductDetail.Tag(label,url,focusIframe,data);
          tags.add(tag);
        }
        pdb.setTags(tags);
      }
      if( productOpenTemplate != null ) pdb.setOpenTemplate(productOpenTemplate);
      if( productSendByUser != null ) pdb.setSendByUser(productSendByUser);
      if( productActionText != null ) pdb.setActionText(productActionText);
      if( productActionTextColor != null ) pdb.setActionTextColor(productActionTextColor);
      if( productIsOpenReselect != null ){
        pdb.setIsOpenReselect(productIsOpenReselect);
        if( productIsOpenReselect ){
          if( productReselectText != null ) pdb.setReselectText(productReselectText);
          if( productHandlerTag != null ) pdb.setHandlerTag(productHandlerTag);
          pdb.setProductReslectOnclickListener(new ProductReslectOnclickListener() {
            @Override
            public void onClick(Context context, String handlerTag) {
              Intent intent = new Intent();
              intent.setAction("com.totoro.flutterQiYu.ProductReselect");
              LocalBroadcastManager manager = LocalBroadcastManager.getInstance(context);
              manager.sendBroadcast(intent);
            }
          });
        }
      }
      return pdb;
    }
    return null;
  }

  private void inputPanelCustomization(MethodCall call){
    InputPanelOptions ipo = new InputPanelOptions();
    int voiceIconResId = this.getResourceId((String)call.argument("voiceIconRes"),"drawable");
    if(voiceIconResId != 0)
      ipo.voiceIconResId = voiceIconResId;

    int emojiIconResId = this.getResourceId((String)call.argument("emojiIconRes"),"drawable");
    if(emojiIconResId != 0)
      ipo.emojiIconResId = emojiIconResId;

    int photoIconResId = this.getResourceId((String)call.argument("photoIconRes"),"drawable");
    if(photoIconResId != 0)
      ipo.photoIconResId = photoIconResId;

    int moreIconResId = this.getResourceId((String)call.argument("moreIconRes"),"drawable");
    if(moreIconResId != 0)
      ipo.moreIconResId = moreIconResId;

    ipo.showActionPanel = (boolean) call.argument("showActionPanel");

    if(ipo.showActionPanel){
      List<HashMap> actionList = call.argument("actionList");
      if(actionList != null){
        List<BaseAction> list = new ArrayList();
        for(HashMap map : actionList){
          int type  = (int)map.get("type");
          String name = (String)map.get("name");
          String icon = (String)map.get("icon");
          list.add(this.getAction(type, name, icon));
        }
      }
    }

    this.option.inputPanelOptions = ipo;
    Unicorn.updateOptions(this.option);
  }

  private BaseAction getAction(int type, String name, String icon){
    if(type < 0 || name == null || icon == null)
      return null;
    int nameId = this.getResourceId(name, "string");
    int picId = this.getResourceId(icon, "drawable");
    if(nameId <= 0 || picId <= 0)
      return null;
    switch (type){
      case 0:
        return new ImageAction(picId, nameId);
      case 1:
        return new CameraAction(picId, nameId);
      case 2:
        return new AlbumAction(picId, nameId);
    }
    return null;
  }

  private void uiCustomization(MethodCall call){
    UICustomization ui = new UICustomization();
    ui.msgBackgroundUri = call.argument("msgBackgroundUri");
    ui.msgBackgroundColor = (int) call.argument("msgBackgroundColor");
    ui.msgListViewDividerHeight = (int) call.argument("msgListViewDividerHeight");
    ui.hideLeftAvatar = (boolean) call.argument("hideLeftAvatar");
    ui.avatarShape = (int) call.argument("avatarShape");
    ui.leftAvatar = call.argument("leftAvatar");
    ui.rightAvatar = call.argument("rightAvatar");
    ui.tipsTextColor = (int) call.argument("tipsTextColor");
    ui.tipsTextSize = (float) call.argument("tipsTextSize");
    int msgItemBackgroundLeftId = this.getResourceId((String) call.argument("msgItemBackgroundLeft"),"drawable");
    if(msgItemBackgroundLeftId != 0)
      ui.msgItemBackgroundLeft = msgItemBackgroundLeftId;
    int msgItemBackgroundRightId = this.getResourceId((String) call.argument("msgItemBackgroundRight"),"drawable");
    if(msgItemBackgroundRightId != 0)
      ui.msgItemBackgroundRight = msgItemBackgroundRightId;
    int audioMsgAnimationLeftId = this.getResourceId((String) call.argument("audioMsgAnimationLeft"),"drawable");
    if(audioMsgAnimationLeftId != 0)
      ui.audioMsgAnimationLeft = audioMsgAnimationLeftId;
    int audioMsgAnimationRightId = this.getResourceId((String) call.argument("audioMsgAnimationRight"),"drawable");
    if(audioMsgAnimationRightId != 0)
      ui.audioMsgAnimationRight = audioMsgAnimationRightId;
    ui.textMsgColorLeft = (int) call.argument("textMsgColorLeft");
    ui.textMsgColorRight = (int) call.argument("textMsgColorRight");
    ui.hyperLinkColorLeft = (int) call.argument("hyperLinkColorLeft");
    ui.hyperLinkColorRight = (int) call.argument("hyperLinkColorRight");
    ui.textMsgSize = (float) call.argument("textMsgSize");
    ui.inputTextColor = (int) call.argument("inputTextColor");
    ui.inputTextSize = (float) call.argument("inputTextSize");
    ui.topTipBarBackgroundColor = (int) call.argument("topTipBarBackgroundColor");
    ui.topTipBarTextColor = (int) call.argument("topTipBarTextColor");
    ui.topTipBarTextSize = (float) call.argument("topTipBarTextSize");
    int titleBackgroundResId = this.getResourceId((String) call.argument("titleBackgroundRes"),"drawable");
    if( titleBackgroundResId != 0 )
      ui.titleBackgroundResId = titleBackgroundResId;
    ui.titleBackgroundColor = (int) call.argument("titleBackgroundColor");
    ui.titleBarStyle = (int) call.argument("titleBarStyle");
    ui.titleCenter = (boolean) call.argument("titleCenter");
    ui.buttonBackgroundColorList = (int) call.argument("buttonBackgroundColorList");
    ui.buttonTextColor = (int) call.argument("buttonTextColor");
    ui.hideAudio = (boolean) call.argument("hideAudio");
    ui.hideAudioWithRobot = (boolean) call.argument("hideAudioWithRobot");
    ui.hideEmoji = (boolean) call.argument("hideEmoji");
    ui.screenOrientation = (int) call.argument("screenOrientation");
    ui.hideKeyboardOnEnterConsult = (boolean) call.argument("hideKeyboardOnEnterConsult");
    int robotBtnBackId = this.getResourceId((String) call.argument("robotBtnBack"),"drawable");
    if(robotBtnBackId != 0)
      ui.robotBtnBack = robotBtnBackId;
    ui.robotBtnTextColor = (int) call.argument("robotBtnTextColor");
    int inputUpBtnBackId = this.getResourceId((String) call.argument("inputUpBtnBack"),"drawable");
    if(inputUpBtnBackId != 0)
      ui.inputUpBtnBack = inputUpBtnBackId;
    ui.inputUpBtnTextColor = (int) call.argument("inputUpBtnTextColor");
    this.option.uiCustomization = ui;
    Unicorn.updateOptions(this.option);
  }

  @Override
  public void onListen(Object arguments, final EventChannel.EventSink events){
    sendRespReceiver = new BroadcastReceiver() {
      @Override
      public void onReceive(Context context, Intent intent) {
        HashMap<String,String> data = new HashMap();
        String action = intent.getAction();
        if(action.equals("com.totoro.flutterQiYu.QiYuUnreadCount")) {
          data.put("unreadCount", String.valueOf(unreadCount));
        }else if(action.equals("com.totoro.flutterQiYu.ProductReselect")) {
          data.put("productReselect", "");
        }
        events.success(data);
      }
    };

    IntentFilter intentFilter = new IntentFilter();
    intentFilter.addAction("com.totoro.flutterQiYu.QiYuUnreadCount");
    LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(this.context);
    localBroadcastManager.registerReceiver(sendRespReceiver, intentFilter);
  }

  @Override
  public void onCancel(Object arguments) {
    LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(this.context);
    localBroadcastManager.unregisterReceiver(sendRespReceiver);
    sendRespReceiver = null;
  }

  public int getResourceId(String name, String type){
    if(name == null && name.isEmpty())
      return 0;
    return this.context.getResources().getIdentifier(name, type, appInfo.packageName);
  }
}
