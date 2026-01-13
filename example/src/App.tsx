import { useEffect, useState } from 'react';
import {
  StyleSheet,
  View,
  Text,
  PermissionsAndroid,
  Platform,
  ActivityIndicator,
} from 'react-native';
import { LiveDetectEdgesView } from 'react-native-live-detect-edges';

export default function App() {
  const [hasPermission, setHasPermission] = useState(false);

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

  if (!hasPermission) {
    return (
      <View style={styles.container}>
        <ActivityIndicator size="large" />
        <Text>Requesting Camera Permission...</Text>
      </View>
    );
  }

  return (
    <View style={styles.container}>
      {/* <View style={styles.controls}>
        <TouchableOpacity
          style={[
            styles.button,
            isCameraRunning ? styles.buttonDisabled : styles.buttonStart,
          ]}
          onPress={handleStartCamera}
          disabled={isCameraRunning}
        >
          <Text style={styles.buttonText}>Start Camera</Text>
        </TouchableOpacity>
        <TouchableOpacity
          style={[
            styles.button,
            !isCameraRunning ? styles.buttonDisabled : styles.buttonStop,
          ]}
          onPress={handleStopCamera}
          disabled={!isCameraRunning}
        >
          <Text style={styles.buttonText}>Stop Camera</Text>
        </TouchableOpacity>
      </View> */}
      <LiveDetectEdgesView
        style={styles.scanner}
        overlayColor="red"
        overlayStrokeWidth={8}
      />
      <View style={styles.overlay}>
        <Text style={styles.text}>Align document within frame</Text>
      </View>
    </View>
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
  scanner: {
    flex: 1,
  },
  controls: {
    flexDirection: 'row',
    justifyContent: 'space-around',
    padding: 10,
    backgroundColor: '#00000080',
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
  },
  text: {
    color: 'white',
    fontSize: 16,
    backgroundColor: '#00000080',
    padding: 10,
    borderRadius: 5,
  },
});
