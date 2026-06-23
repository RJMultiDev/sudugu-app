// Server API base URL - change this to your server's address
// For web: use localhost; for physical device: use your machine's LAN IP
import { Platform } from 'react-native';

function getBaseUrl(): string {
  // On web, always use localhost
  if (typeof window !== 'undefined') {
    return 'http://localhost:3001/api';
  }
  // On Android emulator, use 10.0.2.2; on iOS simulator, use localhost
  if (Platform.OS === 'android') {
    return 'http://10.0.2.2:3001/api';
  }
  return 'http://localhost:3001/api';
}

export const API_BASE_URL = getBaseUrl();

export const CATEGORIES = [
  { slug: 'dushi', name: '都市', icon: '🏙️' },
  { slug: 'xuanhuan', name: '玄幻', icon: '🐉' },
  { slug: 'xianxia', name: '仙侠', icon: '⚔️' },
  { slug: 'qing', name: '轻小说', icon: '📚' },
  { slug: 'lishi', name: '历史', icon: '📜' },
  { slug: 'kehuan', name: '科幻', icon: '🚀' },
  { slug: 'yanqing', name: '言情', icon: '💕' },
  { slug: 'xuanyi', name: '悬疑', icon: '🔍' },
  { slug: 'junshi', name: '军事', icon: '🎖️' },
  { slug: 'qihuan', name: '奇幻', icon: '✨' },
  { slug: 'youxi', name: '游戏', icon: '🎮' },
  { slug: 'guanchang', name: '官场', icon: '🏛️' },
  { slug: 'wuxia', name: '武侠', icon: '🗡️' },
  { slug: 'tiyu', name: '体育', icon: '⚽' },
  { slug: 'xianshi', name: '现实', icon: '📖' },
  { slug: 'xiangcun', name: '乡村', icon: '🌾' },
  { slug: 'zhutianwuxian', name: '诸天无限', icon: '🌌' },
] as const;

export const FONT_SIZES = {
  small: 16,
  medium: 18,
  large: 20,
  xlarge: 22,
} as const;

export const COLORS = {
  light: {
    primary: '#4A90D9',
    background: '#F5F5F5',
    surface: '#FFFFFF',
    text: '#333333',
    textSecondary: '#666666',
    border: '#E0E0E0',
    tabBar: '#FFFFFF',
    readerBg: '#F5F0E8',
    readerText: '#333333',
    card: '#FFFFFF',
    error: '#E74C3C',
    success: '#27AE60',
  },
  dark: {
    primary: '#5A9FE8',
    background: '#1A1A1A',
    surface: '#2A2A2A',
    text: '#E0E0E0',
    textSecondary: '#999999',
    border: '#404040',
    tabBar: '#2A2A2A',
    readerBg: '#1A1A1A',
    readerText: '#C0C0C0',
    card: '#2A2A2A',
    error: '#E74C3C',
    success: '#27AE60',
  },
};
