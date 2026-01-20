#import "LiveDetectEdgesView.h"

#import <React/RCTConversions.h>

#import <react/renderer/components/LiveDetectEdgesViewSpec/ComponentDescriptors.h>
#import <react/renderer/components/LiveDetectEdgesViewSpec/Props.h>
#import <react/renderer/components/LiveDetectEdgesViewSpec/RCTComponentViewHelpers.h>

#import "RCTFabricComponentsPlugins.h"

#if __has_include(<LiveDetectEdges/LiveDetectEdges-Swift.h>)
#import <LiveDetectEdges/LiveDetectEdges-Swift.h>
#else
#import "LiveDetectEdges-Swift.h"
#endif

using namespace facebook::react;

@implementation LiveDetectEdgesView {
  UIView *_view;
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
