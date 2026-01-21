import type { TurboModule } from 'react-native';
import { TurboModuleRegistry } from 'react-native';

export type Point = {
  x: number;
  y: number;
};

export type Quadrilateral = {
  topLeft: Point;
  topRight: Point;
  bottomRight: Point;
  bottomLeft: Point;
};

export type CropImageParams = {
  imageUri: string;
  quad: Quadrilateral;
};

export type CropImageResult = {
  uri: string;
  width: number;
  height: number;
};

export type TakePhotoResult = {
  image: {
    uri: string;
    width: number;
    height: number;
  };
  originalImage: {
    uri: string;
    width: number;
    height: number;
  };
  detectedPoints: Point[];
};

export interface Spec extends TurboModule {
  takePhoto(): Promise<TakePhotoResult>;
  cropImage(params: CropImageParams): Promise<CropImageResult>;
}

export default TurboModuleRegistry.getEnforcing<Spec>('LiveDetectEdgesModule');
