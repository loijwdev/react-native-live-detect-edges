import 'react-native-gesture-handler';
import { NavigationContainer } from '@react-navigation/native';
import { createNativeStackNavigator } from '@react-navigation/native-stack';
import HomeScreen from './screens/HomeScreen';
import CropScreen from './screens/CropScreen';
import ResultScreen from './screens/ResultScreen';
import type { RootStackParamList } from './navigation/types';

const Stack = createNativeStackNavigator<RootStackParamList>();

export default function App() {
  return (
    <NavigationContainer>
      <Stack.Navigator initialRouteName="Home">
        <Stack.Screen
          name="Home"
          component={HomeScreen}
          options={{ title: 'Scanner', headerShown: false }}
        />
        <Stack.Screen
          name="Crop"
          component={CropScreen}
          options={{ title: 'Adjust Crop' }}
        />
        <Stack.Screen
          name="Result"
          component={ResultScreen}
          options={{ title: 'Cropped Result' }}
        />
      </Stack.Navigator>
    </NavigationContainer>
  );
}
