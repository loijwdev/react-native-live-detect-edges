#import <React/RCTBridgeModule.h>

@interface RCT_EXTERN_MODULE (LiveDetectEdgesModule, NSObject)

RCT_EXTERN_METHOD(cropImage
                  : (NSDictionary *)params resolve
                  : (RCTPromiseResolveBlock)resolve reject
                  : (RCTPromiseRejectBlock)reject)

RCT_EXTERN_METHOD(takePhoto
                  : (RCTPromiseResolveBlock)resolve reject
                  : (RCTPromiseRejectBlock)reject)

@end
