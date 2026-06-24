import React from 'react';
import { Text } from 'react-native';
import { NavigationContainer, useNavigation, useRoute } from '@react-navigation/native';
import { createNativeStackNavigator, NativeStackNavigationProp } from '@react-navigation/native-stack';
import { createBottomTabNavigator } from '@react-navigation/bottom-tabs';
import { RouteProp } from '@react-navigation/native';
import { useTheme } from '../context/ThemeContext';

import { HomeScreen } from '../screens/HomeScreen';
import { CategoryScreen } from '../screens/CategoryScreen';
import { CategoryDetailScreen } from '../screens/CategoryDetailScreen';
import { BookshelfScreen } from '../screens/BookshelfScreen';
import { ProfileScreen } from '../screens/ProfileScreen';
import { NovelDetailScreen } from '../screens/NovelDetailScreen';
import { ReaderScreen } from '../screens/ReaderScreen';
import { SearchScreen } from '../screens/SearchScreen';
import { ChapterListScreen } from '../screens/ChapterListScreen';
import { RankingScreen } from '../screens/RankingScreen';
import { ReadHistoryScreen } from '../screens/ReadHistoryScreen';

export type RootStackParamList = {
  Main: undefined;
  Search: undefined;
  NovelDetail: { id: string };
  Reader: {
    bookId: string;
    bookTitle: string;
    chapterId: number;   // index in chapters array
  };
  ChapterList: {
    bookId: string;
    bookTitle: string;
  };
  Ranking: undefined;
  CategoryDetail: { slug: string; name: string };
  ReadHistory: undefined;
};

export type TabParamList = {
  '首页': undefined;
  '分类': undefined;
  '书架': undefined;
  '我的': undefined;
};

export type ScreenNavigationProp<T extends keyof RootStackParamList> =
  NativeStackNavigationProp<RootStackParamList, T>;

export type ScreenRouteProp<T extends keyof RootStackParamList> =
  RouteProp<RootStackParamList, T>;

const Stack = createNativeStackNavigator<RootStackParamList>();
const Tab = createBottomTabNavigator<TabParamList>();

function TabIcon({ label, focused }: { label: string; focused: boolean }) {
  const icons: Record<string, string> = {
    '首页': '📖',
    '分类': '📂',
    '书架': '📚',
    '我的': '👤',
  };
  return (
    <Text style={{ fontSize: focused ? 22 : 20 }}>
      {icons[label] || '📄'}
    </Text>
  );
}

function MainTabs() {
  const { colors } = useTheme();

  return (
    <Tab.Navigator
      screenOptions={({ route }) => ({
        tabBarIcon: ({ focused }) => (
          <TabIcon label={route.name} focused={focused} />
        ),
        tabBarActiveTintColor: colors.primary,
        tabBarInactiveTintColor: colors.textSecondary,
        tabBarStyle: { backgroundColor: colors.tabBar, borderTopColor: colors.border },
        headerStyle: { backgroundColor: colors.surface },
        headerTintColor: colors.text,
        headerTitleStyle: { fontWeight: '700' },
      })}>
      <Tab.Screen name="首页" component={HomeScreen} options={{ headerTitle: '速读谷' }} />
      <Tab.Screen name="分类" component={CategoryScreen} options={{ headerTitle: '小说分类' }} />
      <Tab.Screen name="书架" component={BookshelfScreen} options={{ headerTitle: '我的书架' }} />
      <Tab.Screen name="我的" component={ProfileScreen} options={{ headerShown: false }} />
    </Tab.Navigator>
  );
}

// Wrapper to inject navigation/route as any to avoid strict typing issues
function wrapScreen<P>(Component: React.ComponentType<P>) {
  return (props: any) => <Component {...props} />;
}

export function AppNavigator() {
  const { colors } = useTheme();

  return (
    <NavigationContainer>
      <Stack.Navigator
        screenOptions={{
          headerStyle: { backgroundColor: colors.surface },
          headerTintColor: colors.text,
          headerTitleStyle: { fontWeight: '600' },
          contentStyle: { backgroundColor: colors.background },
        }}>
        <Stack.Screen name="Main" component={MainTabs} options={{ headerShown: false }} />
        <Stack.Screen name="Search" component={wrapScreen(SearchScreen)} options={{ headerTitle: '搜索小说' }} />
        <Stack.Screen name="NovelDetail" component={wrapScreen(NovelDetailScreen)} options={{ headerTitle: '小说详情' }} />
        <Stack.Screen
          name="Reader"
          component={wrapScreen(ReaderScreen)}
          options={{ headerShown: false, animation: 'fade' }}
        />
        <Stack.Screen name="ChapterList" component={wrapScreen(ChapterListScreen)} options={{ headerTitle: '章节目录' }} />
        <Stack.Screen name="Ranking" component={wrapScreen(RankingScreen)} options={{ headerTitle: '排行榜' }} />
        <Stack.Screen
          name="CategoryDetail"
          component={wrapScreen(CategoryDetailScreen)}
          options={({ route }) => ({ headerTitle: route.params?.name || '分类' })}
        />
        <Stack.Screen name="ReadHistory" component={wrapScreen(ReadHistoryScreen)} options={{ headerTitle: '阅读记录' }} />
      </Stack.Navigator>
    </NavigationContainer>
  );
}
