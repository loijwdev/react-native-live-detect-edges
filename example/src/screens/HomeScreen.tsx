import { useEffect, useState } from 'react';
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
} from 'react-native';
import { LiveDetectEdgesView, takePhoto } from 'react-native-live-detect-edges';
import { useNavigation } from '@react-navigation/native';
import type { NativeStackNavigationProp } from '@react-navigation/native-stack';
import type { RootStackParamList } from '../navigation/types';

type HomeScreenNavigationProp = NativeStackNavigationProp<
  RootStackParamList,
  'Home'
>;

export default function HomeScreen() {
  const navigation = useNavigation<HomeScreenNavigationProp>();
  const [hasPermission, setHasPermission] = useState(false);
  const [openCamera, setOpenCamera] = useState(false);
  const [isProcessing, setIsProcessing] = useState(false);

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
      setIsProcessing(true);
      const result = await takePhoto();
      if (result) {
        console.log('--- Captured --- result', result);
        navigation.navigate('Crop', {
          imageUri: result.originalImage.uri,
          imageWidth: result.originalImage.width,
          imageHeight: result.originalImage.height,
          initialPoints: result.detectedPoints,
        });
        setOpenCamera(false); // Close camera after capture
      }
    } catch (e) {
      console.error('Capture failed', e);
      Alert.alert('Error', 'Capture failed');
    } finally {
      setIsProcessing(false);
    }
  };

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

      {openCamera ? (
        <>
          <LiveDetectEdgesView
            overlayColor="red"
            overlayStrokeWidth={4}
            style={styles.scanner}
          />
          {isProcessing && (
            <View style={styles.overlayIndicator}>
              <ActivityIndicator size="large" color="white" />
            </View>
          )}
          <View style={styles.overlay}>
            <Text style={styles.text}>Align document within frame</Text>
          </View>
        </>
      ) : (
        <View style={styles.placeholderContainer}>
          <Text style={styles.placeholderText}>Camera is stopped</Text>
          <Text style={styles.placeholderSubText}>
            Press Start Camera to begin
          </Text>
        </View>
      )}
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: 'black',
  },
  scanner: {
    flex: 1,
  },
  controls: {
    flexDirection: 'row',
    justifyContent: 'space-around',
    padding: 10,
    backgroundColor: '#00000080',
    zIndex: 100,
  },
  overlayIndicator: {
    ...StyleSheet.absoluteFillObject,
    justifyContent: 'center',
    alignItems: 'center',
    zIndex: 999,
    backgroundColor: 'rgba(0,0,0,0.5)',
  },
  overlay: {
    position: 'absolute',
    bottom: 50,
    left: 0,
    right: 0,
    alignItems: 'center',
    pointerEvents: 'none',
  },
  text: {
    color: 'white',
    fontSize: 16,
    backgroundColor: '#00000080',
    padding: 10,
    borderRadius: 5,
  },
  placeholderContainer: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
    backgroundColor: '#1a1a1a',
  },
  placeholderText: {
    color: 'white',
    fontSize: 20,
    fontWeight: 'bold',
    marginBottom: 10,
  },
  placeholderSubText: {
    color: '#aaa',
    fontSize: 16,
  },
});
