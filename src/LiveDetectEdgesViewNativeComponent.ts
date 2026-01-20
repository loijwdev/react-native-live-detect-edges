import {
  codegenNativeComponent,
  type ColorValue,
  type ViewProps,
  type CodegenTypes,
} from 'react-native';

interface NativeProps extends ViewProps {
  overlayColor?: ColorValue;
  overlayFillColor?: ColorValue;
  overlayStrokeWidth?: CodegenTypes.Float;
}

export default codegenNativeComponent<NativeProps>('LiveDetectEdgesView');
