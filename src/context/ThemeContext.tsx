import React, { createContext, useContext, useState, useEffect, useCallback } from 'react';
import { useColorScheme } from 'react-native';
import { getThemeMode, setThemeMode as storeTheme, getFontSize, setFontSize as storeFontSize } from '../services/storage';
import { COLORS, FONT_SIZES } from '../utils/constants';
import type { ThemeMode, ReaderFontSize } from '../types';

interface ThemeContextType {
  theme: ThemeMode;
  colors: typeof COLORS.light;
  fontSize: number;
  readerFontSize: ReaderFontSize;
  toggleTheme: () => void;
  setReaderFontSize: (size: ReaderFontSize) => void;
}

const ThemeContext = createContext<ThemeContextType>({
  theme: 'light',
  colors: COLORS.light,
  fontSize: FONT_SIZES.medium,
  readerFontSize: 'medium',
  toggleTheme: () => {},
  setReaderFontSize: () => {},
});

export function ThemeProvider({ children }: { children: React.ReactNode }) {
  const systemScheme = useColorScheme();
  const [theme, setTheme] = useState<ThemeMode>('light');
  const [readerFontSize, setReaderFontSizeState] = useState<ReaderFontSize>('medium');

  useEffect(() => {
    getThemeMode().then((mode) => setTheme(mode));
    getFontSize().then((size) => setReaderFontSizeState(size));
  }, []);

  const toggleTheme = useCallback(() => {
    setTheme((prev) => {
      const next = prev === 'light' ? 'dark' : 'light';
      storeTheme(next);
      return next;
    });
  }, []);

  const setReaderFontSize = useCallback((size: ReaderFontSize) => {
    setReaderFontSizeState(size);
    storeFontSize(size);
  }, []);

  const colors = COLORS[theme];
  const fontSize = FONT_SIZES[readerFontSize];

  return (
    <ThemeContext.Provider
      value={{ theme, colors, fontSize, readerFontSize, toggleTheme, setReaderFontSize }}>
      {children}
    </ThemeContext.Provider>
  );
}

export const useTheme = () => useContext(ThemeContext);
