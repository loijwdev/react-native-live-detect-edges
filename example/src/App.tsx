import { useEffect, useRef, useState } from 'react';
import {
  StyleSheet,
  View,
  Text,
  PermissionsAndroid,
  Platform,
  ActivityIndicator,
  Button,
  SafeAreaView,
  Alert,
  Image,
} from 'react-native';
import {
  LiveDetectEdgesView,
  type LiveDetectRef,
  type OnCaptureEventData,
} from 'react-native-live-detect-edges';

export default function App() {
  const [hasPermission, setHasPermission] = useState(false);
  const [openCamera, setOpenCamera] = useState(false);
  const [capturedResult, setCapturedResult] =
    useState<OnCaptureEventData | null>(null);
  const [viewSize, setViewSize] = useState({ width: 0, height: 0 });
  const cameraRef = useRef<LiveDetectRef>(null);

  useEffect(() => {
    const checkPermission = async () => {
      if (Platform.OS === 'android') {
        const granted = await PermissionsAndroid.request(
          PermissionsAndroid.PERMISSIONS.CAMERA,
          {
            title: 'Camera Permission',
            message: 'App needs access to your camera to scan documents.',
            buttonNeutral: 'Ask Me Later',
            buttonNegative: 'Cancel',
            buttonPositive: 'OK',
          }
        );
        setHasPermission(granted === PermissionsAndroid.RESULTS.GRANTED);
      } else {
        setHasPermission(true);
      }
    };

    checkPermission();
  }, []);

  const handleStartCamera = () => {
    setOpenCamera(true);
  };

  const handleStopCamera = () => {
    setOpenCamera(false);
  };

  const handleCapture = async () => {
    try {
      const result = await cameraRef.current?.takePhoto();
      console.log('--- Captured via Promise ---');
      if (result) {
        console.log('Image:', result.image);
        console.log('Original Image:', result.originalImage);
        console.log('Detected Points:', result.detectedPoints);

        setCapturedResult(result);
      }
    } catch (e) {
      console.error('Capture failed', e);
      Alert.alert('Error', 'Capture failed');
    }
  };

  const closePreview = () => {
    setCapturedResult(null);
  };

  const [resizeMode, setResizeMode] = useState<'contain' | 'cover'>('contain');

  const toggleResizeMode = () => {
    setResizeMode((prev) => (prev === 'contain' ? 'cover' : 'contain'));
  };

  if (capturedResult) {
    const { originalImage, detectedPoints } = capturedResult;

    // Flexible logic based on resizeMode
    const scale =
      resizeMode === 'contain'
        ? Math.min(
            viewSize.width / originalImage.width,
            viewSize.height / originalImage.height
          )
        : Math.max(
            viewSize.width / originalImage.width,
            viewSize.height / originalImage.height
          );

    const renderWidth = originalImage.width * scale;
    const renderHeight = originalImage.height * scale;

    const offsetX = (viewSize.width - renderWidth) / 2;
    const offsetY = (viewSize.height - renderHeight) / 2;

    return (
      <SafeAreaView style={styles.container}>
        <View
          style={styles.previewContainer}
          onLayout={(e) => setViewSize(e.nativeEvent.layout)}
        >
          {viewSize.width > 0 && (
            <>
              <Image
                source={{ uri: originalImage.uri }}
                style={StyleSheet.absoluteFill}
                resizeMode={resizeMode}
              />
              {detectedPoints.map((p: { x: number; y: number }, i: number) => (
                <View
                  key={i}
                  style={[
                    styles.point,
                    {
                      left: p.x * scale + offsetX - 5,
                      top: p.y * scale + offsetY - 5,
                    },
                  ]}
                />
              ))}
            </>
          )}
        </View>
        <View style={styles.controls}>
          <Button title={`Mode: ${resizeMode}`} onPress={toggleResizeMode} />
          <Button title="Close" onPress={closePreview} />
        </View>
      </SafeAreaView>
    );
  }

  if (!hasPermission) {
    return (
      <View style={styles.container}>
        <ActivityIndicator size="large" />
        <Text>Requesting Camera Permission...</Text>
      </View>
    );
  }

  return (
    <SafeAreaView style={styles.container}>
      <View style={styles.controls}>
        <Button
          title={openCamera ? 'Stop Camera' : 'Start Camera'}
          onPress={openCamera ? handleStopCamera : handleStartCamera}
        />
        {openCamera && <Button title="Capture" onPress={handleCapture} />}
      </View>

      {openCamera && (
        <>
          <LiveDetectEdgesView
            ref={cameraRef}
            overlayColor="red"
            overlayStrokeWidth={4}
            style={styles.scanner}
          />
          <View style={styles.overlay}>
            <Text style={styles.text}>Align document within frame</Text>
          </View>
        </>
      )}
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  safeArea: {
    flex: 1,
  },
  container: {
    flex: 1,
    backgroundColor: 'black',
  },
  previewContainer: {
    flex: 1,
    backgroundColor: '#333',
  },
  scanner: {
    flex: 1,
  },
  controls: {
    flexDirection: 'row',
    justifyContent: 'space-around',
    padding: 10,
    backgroundColor: '#00000080',
    zIndex: 100, // Ensure controls are above everything
  },
  button: {
    paddingHorizontal: 20,
    paddingVertical: 10,
    borderRadius: 5,
    minWidth: 120,
    alignItems: 'center',
  },
  buttonStart: {
    backgroundColor: '#4CAF50',
  },
  buttonStop: {
    backgroundColor: '#F44336',
  },
  buttonDisabled: {
    backgroundColor: '#666666',
  },
  buttonText: {
    color: 'white',
    fontSize: 16,
    fontWeight: 'bold',
  },
  overlay: {
    position: 'absolute',
    bottom: 50,
    left: 0,
    right: 0,
    alignItems: 'center',
    pointerEvents: 'none', // Allow touches to pass through
  },
  text: {
    color: 'white',
    fontSize: 16,
    backgroundColor: '#00000080',
    padding: 10,
    borderRadius: 5,
  },
  point: {
    position: 'absolute',
    width: 10,
    height: 10,
    borderRadius: 5,
    backgroundColor: 'lime',
    borderWidth: 1,
    borderColor: 'white',
  },
});
