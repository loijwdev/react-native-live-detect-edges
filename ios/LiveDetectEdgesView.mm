#import "LiveDetectEdgesView.h"

#import <React/RCTConversions.h>

#import <react/renderer/components/LiveDetectEdgesViewSpec/ComponentDescriptors.h>
#import <react/renderer/components/LiveDetectEdgesViewSpec/Props.h>
#import <react/renderer/components/LiveDetectEdgesViewSpec/RCTComponentViewHelpers.h>

#import "RCTFabricComponentsPlugins.h"

using namespace facebook::react;

@implementation LiveDetectEdgesView {
    UIView * _view;
}

+ (ComponentDescriptorProvider)componentDescriptorProvider
{
    return concreteComponentDescriptorProvider<LiveDetectEdgesViewComponentDescriptor>();
}

- (instancetype)initWithFrame:(CGRect)frame
{
  if (self = [super initWithFrame:frame]) {
    static const auto defaultProps = std::make_shared<const LiveDetectEdgesViewProps>();
    _props = defaultProps;

    _view = [[UIView alloc] init];

    self.contentView = _view;
  }

  return self;
}

- (void)updateProps:(Props::Shared const &)props oldProps:(Props::Shared const &)oldProps
{
    const auto &oldViewProps = *std::static_pointer_cast<LiveDetectEdgesViewProps const>(_props);
    const auto &newViewProps = *std::static_pointer_cast<LiveDetectEdgesViewProps const>(props);

    if (oldViewProps.color != newViewProps.color) {
        [_view setBackgroundColor: RCTUIColorFromSharedColor(newViewProps.color)];
    }

    [super updateProps:props oldProps:oldProps];
}

@end
