import type { Float } from 'react-native/Libraries/Types/CodegenTypes';
// eslint-disable-next-line @react-native/no-deep-imports
import codegenNativeComponent from 'react-native/Libraries/Utilities/codegenNativeComponent';
import type { ViewProps, ColorValue } from 'react-native';

interface NativeProps extends ViewProps {
  overlayColor?: ColorValue;
  overlayFillColor?: ColorValue;
  overlayStrokeWidth?: Float;
}

export default codegenNativeComponent<NativeProps>('LiveDetectEdgesView');
