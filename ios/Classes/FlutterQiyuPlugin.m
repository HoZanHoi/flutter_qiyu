#import "FlutterQiyuPlugin.h"
#import <QYSDK.h>

@interface FlutterQiyuPlugin ()<FlutterStreamHandler, QYConversationManagerDelegate>
@property (nonatomic, strong, readonly) NSObject<FlutterPluginRegistrar>* registrar;
@property (nonatomic, strong) UINavigationController* navController;
@property (nonatomic, copy) FlutterEventSink eventSink;
@end

@implementation FlutterQiyuPlugin
+ (void)registerWithRegistrar:(NSObject<FlutterPluginRegistrar>*)registrar {
    FlutterMethodChannel* channel = [FlutterMethodChannel
                                     methodChannelWithName:@"flutter_qiyu"
                                     binaryMessenger:[registrar messenger]];
    FlutterEventChannel* eventChannel = [FlutterEventChannel eventChannelWithName:@"flutter_qiyu_event" binaryMessenger:[registrar messenger]];
    FlutterQiyuPlugin* instance = [[FlutterQiyuPlugin alloc] init];
    [registrar addMethodCallDelegate:instance channel:channel];
    [eventChannel setStreamHandler:instance];
}

- (instancetype)initWithRegistrar:(NSObject<FlutterPluginRegistrar>*) registrar{
    self = [super init];
    if (self) {
        _registrar = registrar;
    }
    return self;
}

- (void)handleMethodCall:(FlutterMethodCall*)call result:(FlutterResult)result {
    NSLog(@"FlutterQiYu: on method called!");
    NSLog(@"FlutterQiYu: %@",call.method);
    NSDictionary* arguments = call.arguments;
    
    if ([@"init" isEqualToString:call.method]) {
        NSString* appKey = arguments[@"appKey"];
        NSString* appName = arguments[@"appName"];
        NSAssert(appKey, @"appKey can not be null");
        [[QYSDK sharedSDK] registerAppId:appKey appName:appName];
        result([NSNumber numberWithBool:YES]);
    } else if ([@"setInfo" isEqualToString:call.method]) {
        NSString* name = arguments[@"name"];
        NSString* phone = arguments[@"phone"];
        NSString* userId = arguments[@"id"];
        NSString* token = arguments[@"token"];
        NSString* avatar = arguments[@"avatar"];
        
        QYUserInfo* userInfo = [[QYUserInfo alloc] init];
        userInfo.userId = userId;
        userInfo.data = [NSString stringWithFormat:@"[ {\"key\":\"real_name\",\"value\":\"%@\"},{\"key\":\"mobile_phone\",\"value\":\"%@\"},{\"key\":\"avatar\",\"value\":\"%@\"},{\"key\":\"userId\",\"value\":\"%@\",\"label\":\"token\"},{\"key\":\"token\",\"value\":\"%@\",\"label\":\"token\"}, ]",name,phone,avatar,userId,token];
        [[QYSDK sharedSDK] setUserInfo:userInfo];
        result([NSNumber numberWithBool:YES]);
    } else if([@"logout" isEqualToString:call.method]) {
        [[QYSDK sharedSDK] logout:^(BOOL success){
            result([NSNumber numberWithBool:success]);
        }];
    } else if([@"clearCache" isEqualToString:call.method]) {
        [[QYSDK sharedSDK] cleanResourceCacheWithBlock:^(NSError *error){
            if(error){
                result([FlutterError errorWithCode:[NSString stringWithFormat:@"%ld",(long)error.code]
                                           message:error.localizedDescription
                                           details:error.userInfo]);
            }else
                result([NSNumber numberWithBool:true]);
        }];
    } else if([@"openService" isEqualToString:call.method]) {
        NSString* uri = arguments[@"fromUri"];
        NSString* title = arguments[@"fromTitle"];
        NSInteger staffId = [arguments[@"staffId"] integerValue];
        NSInteger groupId = [arguments[@"groupId"] integerValue];
        NSInteger robotId = [arguments[@"robotId"] integerValue];
        NSInteger vipLevel = [arguments[@"vipLevel"] integerValue];
        
        QYSource* source = [[QYSource alloc] init];
        source.urlString = uri ? : @"";
        source.title = title ? : @"";
        
        QYSessionViewController* sessionViewController = [[QYSDK sharedSDK] sessionViewController];
        sessionViewController.sessionTitle = @"咨询客服";
        sessionViewController.groupId = groupId;
        sessionViewController.staffId = staffId;
        sessionViewController.robotId = robotId;
        sessionViewController.vipLevel = vipLevel;
        sessionViewController.source = source;
        sessionViewController.commodityInfo = [self makeCommondityInfo:arguments];
        
        UINavigationController* nav = [[UINavigationController alloc] initWithRootViewController:sessionViewController];
        self.navController = nav;
        sessionViewController.navigationItem.leftBarButtonItem = [[UIBarButtonItem alloc] initWithTitle:@"返回" style:UIBarButtonItemStylePlain target:self action:@selector(onBack:)];
        [UIApplication.sharedApplication.delegate.window.rootViewController presentViewController:nav animated:YES completion:^{
            result([NSNumber numberWithBool:true]);
        }];
    } else if ([@"getUnreadCount" isEqualToString:call.method]) {
        NSInteger count = [[[QYSDK sharedSDK] conversationManager] allUnreadCount];
        result([NSNumber numberWithInteger:count]);
    } else if ([@"uiCustomization" isEqualToString:call.method]) {
        
        result([NSNumber numberWithBool:true]);
    } else if ([@"inputPanelCustomization" isEqualToString:call.method]) {
        result([NSNumber numberWithBool:true]);
    } else if ([@"titleBarConfig" isEqualToString:call.method]) {
        result([NSNumber numberWithBool:true]);
    } else {
        result(FlutterMethodNotImplemented);
    }
}

- (QYCommodityInfo*)makeCommondityInfo:(NSDictionary*)arguments {
    NSString* productTitle = arguments[@"productTitle"];
    if(productTitle && [productTitle isKindOfClass:[NSString class]]){
        NSString* productDesc = arguments[@"productDesc"];
        NSString* productNote = arguments[@"productNote"];
        NSString* productPic = arguments[@"productPic"];
        NSString* productUrl = arguments[@"productUrl"];
        BOOL productShow = [arguments[@"productShow"] boolValue];
        NSArray<NSDictionary*>* productTags = arguments[@"productTags"];
        BOOL productSendByUser = [arguments[@"productSendByUser"] boolValue];
        NSString* productActionText = arguments[@"productTitle"];
        NSInteger productActionTextColor = [arguments[@"productTitle"] integerValue];
        QYCommodityInfo* info = [[QYCommodityInfo alloc] init];
        [info setTitle:productTitle];
        if(productDesc) [info setDesc:productDesc];
        if(productNote) [info setNote:productNote];
        if(productPic) [info setPictureUrlString:productPic];
        if(productUrl) [info setUrlString:productUrl];
        if(productShow) [info setShow:productShow];
        if(productTags && productTags.count > 0){
            NSMutableArray<QYCommodityTag*>* tagArr = [NSMutableArray arrayWithCapacity:productTags.count];
            [productTags enumerateObjectsUsingBlock:^(NSDictionary * _Nonnull obj, NSUInteger idx, BOOL * _Nonnull stop) {
                QYCommodityTag* tag = [[QYCommodityTag alloc] init];
                [tag setLabel:obj[@"label"] ? : @""];
                [tag setUrl:obj[@"url"] ? : @""];
                [tag setFocusIframe:obj[@"focusIframe"] ? : @""];
                [tag setData:obj[@"data"] ? : @""];
                [tagArr addObject:tag];
            }];
            [info setTagsArray:tagArr];
        }
        if(productSendByUser) [info setSendByUser:productSendByUser];
        if(productActionText) [info setActionText:productActionText];
        if(productActionTextColor) [info setActionTextColor:[self UIColorFromHex:productActionTextColor]];

        return info;
    }
    
    return nil;
}

- (FlutterError * _Nullable)onCancelWithArguments:(id _Nullable)arguments {
    return nil;
}

- (FlutterError * _Nullable)onListenWithArguments:(id _Nullable)arguments eventSink:(nonnull FlutterEventSink)events {
    self.eventSink = [events copy];
    return nil;
}

- (void) onBack:(id) sender {
    [self.navController dismissViewControllerAnimated:YES completion:^{
        self.navController = nil;
    }];
}

- (UIColor*) UIColorFromHex:(NSInteger)colorInHex {
    // colorInHex should be value like 0xFFFFFF
    return [UIColor colorWithRed:((float) ((colorInHex & 0xFF0000) >> 16)) / 0xFF
                           green:((float) ((colorInHex & 0xFF00) >> 8)) / 0xFF
                            blue:((float) (colorInHex & 0xFF)) / 0xFF
                           alpha:1.0];
}

#pragma mark - QYConversationManagerDelegate

- (void) onUnreadCountChanged:(NSInteger)count{
    if(self.eventSink){
        NSDictionary* data = [NSDictionary dictionary];
        [data setValue:[NSNumber numberWithInteger:count] forKey:@"unreadCount"];
        self.eventSink(data);
    }
}

@end
