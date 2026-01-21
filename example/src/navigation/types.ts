export type Point = {
  x: number;
  y: number;
};

export type RootStackParamList = {
  Home: undefined;
  Crop: {
    imageUri: string;
    imageWidth: number;
    imageHeight: number;
    initialPoints: Point[];
  };
  Result: {
    imageUri: string;
    width: number;
    height: number;
  };
};
