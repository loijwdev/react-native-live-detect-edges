import {
  codegenNativeComponent,
  type ColorValue,
  type ViewProps,
  type CodegenTypes,
} from 'react-native';

interface NativeProps extends ViewProps {
  color?: ColorValue;
  overlayColor?: ColorValue;
  overlayStrokeWidth?: CodegenTypes.Float;
}

export default codegenNativeComponent<NativeProps>('LiveDetectEdgesView');
