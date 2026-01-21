import React, { forwardRef, useImperativeHandle, useRef } from 'react';
import { type ViewProps, StyleSheet } from 'react-native';
import LiveDetectEdgesViewNativeComponent, {
  Commands,
} from './LiveDetectEdgesViewNativeComponent';

export type LiveDetectEdgesViewProps = ViewProps & {
  overlayColor?: string;
  overlayFillColor?: string;
  overlayStrokeWidth?: number;
};

// Redefine the event data for the public API to use parsed Object
export type Point = { x: number; y: number };

export type OnCaptureEventData = {
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

export type LiveDetectRef = {
  takePhoto: () => Promise<OnCaptureEventData>;
};

export const LiveDetectEdgesView = forwardRef<
  LiveDetectRef,
  LiveDetectEdgesViewProps
>((props, ref) => {
  const nativeRef =
    useRef<React.ElementRef<typeof LiveDetectEdgesViewNativeComponent>>(null);
  // Queue to store pending promises
  const pendingPromises = useRef<Array<(data: OnCaptureEventData) => void>>([]);

  useImperativeHandle(ref, () => ({
    takePhoto: () => {
      return new Promise<OnCaptureEventData>((resolve) => {
        if (nativeRef.current) {
          pendingPromises.current.push(resolve);
          Commands.capture(nativeRef.current);
        }
      });
    },
  }));

  const onCaptureRaw = (event: any) => {
    const { nativeEvent } = event;

    // Parse detectedPoints from JSON string to Array of Objects
    let parsedPoints: Point[] = [];
    if (typeof nativeEvent.detectedPoints === 'string') {
      try {
        const parsed = JSON.parse(nativeEvent.detectedPoints);
        if (Array.isArray(parsed)) {
          parsedPoints = parsed;
        }
      } catch (e) {
        console.error('Failed to parse points', e);
      }
    } else if (Array.isArray(nativeEvent.detectedPoints)) {
      // Fallback in case native logic changes to return array directly
      parsedPoints = nativeEvent.detectedPoints;
    }

    const transformedEvent: OnCaptureEventData = {
      image: nativeEvent.image,
      originalImage: nativeEvent.originalImage,
      detectedPoints: parsedPoints,
    };

    // Resolve the oldest pending promise
    const resolve = pendingPromises.current.shift();
    if (resolve) {
      resolve(transformedEvent);
    }
  };

  return (
    <LiveDetectEdgesViewNativeComponent
      ref={nativeRef}
      {...props}
      onCapture={onCaptureRaw}
      style={[styles.defaultStyle, props.style]}
    />
  );
});

const styles = StyleSheet.create({
  defaultStyle: {
    flex: 1,
  },
});

export * from './LiveDetectEdgesViewNativeComponent';
