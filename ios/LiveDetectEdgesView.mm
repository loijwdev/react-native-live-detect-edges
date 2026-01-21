#import "LiveDetectEdgesView.h"

#import <React/RCTConversions.h>

#import <react/renderer/components/LiveDetectEdgesViewSpec/ComponentDescriptors.h>
#import <react/renderer/components/LiveDetectEdgesViewSpec/Props.h>
#import <react/renderer/components/LiveDetectEdgesViewSpec/RCTComponentViewHelpers.h>

#import "RCTFabricComponentsPlugins.h"
#import <React/RCTViewComponentView.h> // Ensure this is imported if not already via header

// Protocol definition (usually in generated header, but declaring here just in
// case helpful or to satisfy IDE if strictly checked before build)
@protocol RCTLiveDetectEdgesViewViewProtocol <NSObject>
- (void)capture;
@end

@interface LiveDetectEdgesView () <RCTLiveDetectEdgesViewViewProtocol>
@end

#if __has_include(<LiveDetectEdges/LiveDetectEdges-Swift.h>)
#import <LiveDetectEdges/LiveDetectEdges-Swift.h>
#else
#import "LiveDetectEdges-Swift.h"
#endif

using namespace facebook::react;

@implementation LiveDetectEdgesView {
  UIView *_view;
}

- (void)handleCommand:(const NSString *)commandName args:(const NSArray *)args {
  RCTLiveDetectEdgesViewHandleCommand(self, commandName, args);
}

- (void)capture {
  LiveDetectEdgesScannerWrapper *wrapper =
      (LiveDetectEdgesScannerWrapper *)_view;
  [wrapper captureImageWithCompletion:^(NSDictionary *response) {
    if (!self->_eventEmitter) {
      return;
    }

    auto eventEmitter =
        std::static_pointer_cast<LiveDetectEdgesViewEventEmitter const>(
            self->_eventEmitter);

    NSDictionary *imageDict = response[@"image"];
    NSDictionary *originalImageDict = response[@"originalImage"];
    NSArray *pointsArray = response[@"detectedPoints"];

    LiveDetectEdgesViewEventEmitter::OnCaptureImage image;
    image.uri = std::string([imageDict[@"uri"] UTF8String]);
    image.width = [imageDict[@"width"] doubleValue];
    image.height = [imageDict[@"height"] doubleValue];

    LiveDetectEdgesViewEventEmitter::OnCaptureOriginalImage originalImage;
    originalImage.uri = std::string([originalImageDict[@"uri"] UTF8String]);
    originalImage.width = [originalImageDict[@"width"] doubleValue];
    originalImage.height = [originalImageDict[@"height"] doubleValue];

    NSError *error;
    NSData *jsonData = [NSJSONSerialization dataWithJSONObject:pointsArray
                                                       options:0
                                                         error:&error];
    NSString *jsonString = @"[]";
    if (jsonData && !error) {
      jsonString = [[NSString alloc] initWithData:jsonData
                                         encoding:NSUTF8StringEncoding];
    }

    LiveDetectEdgesViewEventEmitter::OnCapture event = {
        .image = image,
        .originalImage = originalImage,
        .detectedPoints = std::string([jsonString UTF8String])};

    eventEmitter->onCapture(event);
  }];
}

+ (ComponentDescriptorProvider)componentDescriptorProvider {
  return concreteComponentDescriptorProvider<
      LiveDetectEdgesViewComponentDescriptor>();
}

- (instancetype)initWithFrame:(CGRect)frame {
  if (self = [super initWithFrame:frame]) {
    static const auto defaultProps =
        std::make_shared<const LiveDetectEdgesViewProps>();
    _props = defaultProps;

    _view = [[LiveDetectEdgesScannerWrapper alloc] init];

    self.contentView = _view;
  }

  return self;
}

- (void)updateProps:(Props::Shared const &)props
           oldProps:(Props::Shared const &)oldProps {
  const auto &oldViewProps =
      *std::static_pointer_cast<LiveDetectEdgesViewProps const>(_props);
  const auto &newViewProps =
      *std::static_pointer_cast<LiveDetectEdgesViewProps const>(props);

  [super updateProps:props oldProps:oldProps];

  LiveDetectEdgesScannerWrapper *wrapper =
      (LiveDetectEdgesScannerWrapper *)_view;

  if (oldViewProps.overlayColor != newViewProps.overlayColor) {
    wrapper.overlayColor = RCTUIColorFromSharedColor(newViewProps.overlayColor);
  }

  if (oldViewProps.overlayFillColor != newViewProps.overlayFillColor) {
    wrapper.overlayFillColor =
        RCTUIColorFromSharedColor(newViewProps.overlayFillColor);
  }

  if (oldViewProps.overlayStrokeWidth != newViewProps.overlayStrokeWidth) {
    wrapper.overlayStrokeWidth = newViewProps.overlayStrokeWidth;
  }
}
@end
