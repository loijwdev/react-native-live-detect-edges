import type {
  DirectEventHandler,
  Double,
  Float,
} from 'react-native/Libraries/Types/CodegenTypes';
// eslint-disable-next-line @react-native/no-deep-imports
import codegenNativeCommands from 'react-native/Libraries/Utilities/codegenNativeCommands';
// eslint-disable-next-line @react-native/no-deep-imports
import codegenNativeComponent from 'react-native/Libraries/Utilities/codegenNativeComponent';
import type { ViewProps, ColorValue, HostComponent } from 'react-native';

export type OnCaptureEventData = Readonly<{
  image: Readonly<{
    uri: string;
    width: Double;
    height: Double;
  }>;
  originalImage: Readonly<{
    uri: string;
    width: Double;
    height: Double;
  }>;
  detectedPoints: string;
}>;

interface NativeProps extends ViewProps {
  overlayColor?: ColorValue;
  overlayFillColor?: ColorValue;
  overlayStrokeWidth?: Float;
  onCapture?: DirectEventHandler<OnCaptureEventData>;
}

interface NativeCommands {
  capture: (viewRef: React.ElementRef<HostComponent<NativeProps>>) => void;
}

export const Commands: NativeCommands = codegenNativeCommands<NativeCommands>({
  supportedCommands: ['capture'],
});

export default codegenNativeComponent<NativeProps>('LiveDetectEdgesView');
