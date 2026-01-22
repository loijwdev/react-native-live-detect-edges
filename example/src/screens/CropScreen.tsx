import { useState } from 'react';
import {
  StyleSheet,
  View,
  Image,
  Button,
  Alert,
  type LayoutChangeEvent,
} from 'react-native';
import {
  useRoute,
  useNavigation,
  type RouteProp,
} from '@react-navigation/native';
import type { NativeStackNavigationProp } from '@react-navigation/native-stack';
import {
  GestureDetector,
  Gesture,
  GestureHandlerRootView,
} from 'react-native-gesture-handler';
import Animated, {
  useSharedValue,
  useAnimatedStyle,
  useAnimatedProps,
  type SharedValue,
} from 'react-native-reanimated';
import Svg, { Path } from 'react-native-svg';
import { cropImage } from 'react-native-live-detect-edges';
import type { RootStackParamList } from '../navigation/types';

type CropScreenRouteProp = RouteProp<RootStackParamList, 'Crop'>;

const POINT_SIZE = 40;
const HALF_POINT_SIZE = POINT_SIZE / 2;

const AnimatedPath = Animated.createAnimatedComponent(Path);

export default function CropScreen() {
  const navigation =
    useNavigation<NativeStackNavigationProp<RootStackParamList>>();
  const route = useRoute<CropScreenRouteProp>();
  const { imageUri, imageWidth, imageHeight, initialPoints } = route.params;

  const [containerLayout, setContainerLayout] = useState<{
    width: number;
    height: number;
    scale: number;
    offsetX: number;
    offsetY: number;
  } | null>(null);

  // Shared values for each point (in screen coordinates relative to image container)
  const tl = useSharedValue({ x: 0, y: 0 });
  const tr = useSharedValue({ x: 0, y: 0 });
  const br = useSharedValue({ x: 0, y: 0 });
  const bl = useSharedValue({ x: 0, y: 0 });

  const onLayout = (event: LayoutChangeEvent) => {
    const { width, height } = event.nativeEvent.layout;
    if (width === 0 || height === 0) return;

    // Calculate scale to fit image in container (contain mode)
    const scale = Math.min(width / imageWidth, height / imageHeight);

    const renderWidth = imageWidth * scale;
    const renderHeight = imageHeight * scale;

    const offsetX = (width - renderWidth) / 2;
    const offsetY = (height - renderHeight) / 2;

    setContainerLayout({ width, height, scale, offsetX, offsetY });

    // Initialize points
    if (initialPoints && initialPoints.length === 4) {
      tl.value = {
        x: initialPoints[0]!.x * scale + offsetX,
        y: initialPoints[0]!.y * scale + offsetY,
      };
      tr.value = {
        x: initialPoints[1]!.x * scale + offsetX,
        y: initialPoints[1]!.y * scale + offsetY,
      };
      br.value = {
        x: initialPoints[2]!.x * scale + offsetX,
        y: initialPoints[2]!.y * scale + offsetY,
      };
      bl.value = {
        x: initialPoints[3]!.x * scale + offsetX,
        y: initialPoints[3]!.y * scale + offsetY,
      };
    } else {
      // Default points if none provided
      tl.value = { x: offsetX, y: offsetY };
      tr.value = { x: offsetX + renderWidth, y: offsetY };
      br.value = { x: offsetX + renderWidth, y: offsetY + renderHeight };
      bl.value = { x: offsetX, y: offsetY + renderHeight };
    }
  };

  const handleCrop = async () => {
    if (!containerLayout) return;

    const { scale, offsetX, offsetY } = containerLayout;

    // Convert screen coords back to image coords
    const toImageCoord = (point: { x: number; y: number }) => ({
      x: (point.x - offsetX) / scale,
      y: (point.y - offsetY) / scale,
    });

    try {
      const result = await cropImage({
        imageUri: imageUri,
        quad: {
          topLeft: toImageCoord(tl.value),
          topRight: toImageCoord(tr.value),
          bottomRight: toImageCoord(br.value),
          bottomLeft: toImageCoord(bl.value),
        },
      });

      console.log('Cropped URI:', result.uri);

      navigation.navigate('Result', {
        imageUri: result.uri,
        width: result.width,
        height: result.height,
      });

      // Alert.alert(
      //   'Success',
      //   `Cropped image saved. Size: ${result.width}x${result.height}`
      // );
    } catch (e: any) {
      console.error(e);
      Alert.alert('Error', 'Failed to crop image: ' + e.message);
    }
  };

  const pathProps = useAnimatedProps(() => {
    const d = `M ${tl.value.x} ${tl.value.y} L ${tr.value.x} ${tr.value.y} L ${br.value.x} ${br.value.y} L ${bl.value.x} ${bl.value.y} Z`;

    return {
      d,
    };
  });

  return (
    <GestureHandlerRootView style={styles.container}>
      <View style={styles.imageContainer} onLayout={onLayout}>
        {containerLayout && (
          <>
            <Image
              source={{ uri: imageUri }}
              style={[
                styles.image,
                {
                  width: imageWidth * containerLayout.scale,
                  height: imageHeight * containerLayout.scale,
                  left: containerLayout.offsetX,
                  top: containerLayout.offsetY,
                },
              ]}
            />

            <Svg style={StyleSheet.absoluteFill} pointerEvents="none">
              <AnimatedPath
                animatedProps={pathProps}
                fill="rgba(0, 255, 0, 0.3)"
                stroke="lime"
                strokeWidth="2"
              />
            </Svg>

            <ControlPoint sharedValue={tl} color="red" />
            <ControlPoint sharedValue={tr} color="blue" />
            <ControlPoint sharedValue={br} color="green" />
            <ControlPoint sharedValue={bl} color="yellow" />
          </>
        )}
      </View>

      <View style={styles.controls}>
        <Button title="Crop Image" onPress={handleCrop} />
      </View>
    </GestureHandlerRootView>
  );
}

interface ControlPointProps {
  sharedValue: SharedValue<{ x: number; y: number }>;
  color: string;
}

const ControlPoint = ({ sharedValue, color }: ControlPointProps) => {
  const gesture = Gesture.Pan().onChange((e) => {
    sharedValue.value = {
      x: sharedValue.value.x + e.changeX,
      y: sharedValue.value.y + e.changeY,
    };
  });

  const style = useAnimatedStyle(() => ({
    transform: [
      { translateX: sharedValue.value.x - HALF_POINT_SIZE },
      { translateY: sharedValue.value.y - HALF_POINT_SIZE },
    ],
  }));

  return (
    <GestureDetector gesture={gesture}>
      <Animated.View
        style={[styles.point, style, { backgroundColor: color }] as any}
      />
    </GestureDetector>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#000',
  },
  imageContainer: {
    flex: 1,
    overflow: 'hidden',
  },
  image: {
    position: 'absolute',
  },
  point: {
    position: 'absolute',
    width: POINT_SIZE,
    height: POINT_SIZE,
    borderRadius: HALF_POINT_SIZE,
    borderWidth: 2,
    borderColor: 'white',
    opacity: 0.8,
  },
  controls: {
    padding: 20,
    backgroundColor: '#333',
    alignItems: 'center',
    paddingBottom: 40,
  },
});
