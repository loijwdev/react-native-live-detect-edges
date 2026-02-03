# import <React/RCTViewManager.h>
# import "LiveDetectEdges-Swift.h" // Import Swift header

@interface LiveDetectEdgesViewManager : RCTViewManager
@end

@implementation LiveDetectEdgesViewManager

RCT_EXPORT_MODULE(LiveDetectEdgesView)

- (UIView *)view
{
  return [[LiveDetectEdgesScannerWrapper alloc] init];
}

RCT_EXPORT_VIEW_PROPERTY(overlayColor, UIColor)
RCT_EXPORT_VIEW_PROPERTY(overlayFillColor, UIColor)
RCT_EXPORT_VIEW_PROPERTY(overlayStrokeWidth, CGFloat)

@end
