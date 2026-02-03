import { type ViewProps, StyleSheet, processColor } from 'react-native';
import LiveDetectEdgesViewNativeComponent from './LiveDetectEdgesViewNativeComponent';

export type LiveDetectEdgesViewProps = ViewProps & {
  overlayColor?: string;
  overlayFillColor?: string;
  overlayStrokeWidth?: number;
};

export const LiveDetectEdgesView = ({
  overlayColor = 'rgba(0, 255, 0, 0.5)',
  overlayFillColor,
  overlayStrokeWidth = 4,
  ...props
}: LiveDetectEdgesViewProps) => {
  return (
    <LiveDetectEdgesViewNativeComponent
      overlayColor={processColor(overlayColor)}
      overlayFillColor={processColor(overlayFillColor)}
      overlayStrokeWidth={overlayStrokeWidth}
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
