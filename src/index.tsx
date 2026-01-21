import { type ViewProps, StyleSheet } from 'react-native';
import LiveDetectEdgesViewNativeComponent from './LiveDetectEdgesViewNativeComponent';

export type LiveDetectEdgesViewProps = ViewProps & {
  overlayColor?: string;
  overlayFillColor?: string;
  overlayStrokeWidth?: number;
};

export const LiveDetectEdgesView = (props: LiveDetectEdgesViewProps) => {
  return (
    <LiveDetectEdgesViewNativeComponent
      {...props}
      style={[styles.defaultStyle, props.style]}
    />
  );
};

const styles = StyleSheet.create({
  defaultStyle: {
    flex: 1,
  },
});

export * from './LiveDetectEdgesViewNativeComponent';
export type {
  CropImageParams,
  CropImageResult,
  Quadrilateral,
  TakePhotoResult,
} from './LiveDetectEdgesModule';

// Convenience export for cropImage function
import LiveDetectEdgesModuleDefault from './LiveDetectEdgesModule';
export const cropImage = LiveDetectEdgesModuleDefault.cropImage;
export const takePhoto = LiveDetectEdgesModuleDefault.takePhoto;
