import React from 'react';
import { View, ActivityIndicator, Text, StyleSheet } from 'react-native';
import { useTheme } from '../context/ThemeContext';

export function LoadingIndicator({ text }: { text?: string }) {
  const { colors } = useTheme();
  return (
    <View style={styles.container}>
      <ActivityIndicator size="large" color={colors.primary} />
      {text && <Text style={[styles.text, { color: colors.textSecondary }]}>{text}</Text>}
    </View>
  );
}

export function ErrorView({ message, onRetry }: { message: string; onRetry?: () => void }) {
  const { colors } = useTheme();
  return (
    <View style={styles.container}>
      <Text style={[styles.errorText, { color: colors.error }]}>{message}</Text>
      {onRetry && (
        <Text style={[styles.retryText, { color: colors.primary }]} onPress={onRetry}>
          点击重试
        </Text>
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, justifyContent: 'center', alignItems: 'center', padding: 20 },
  text: { marginTop: 12, fontSize: 14 },
  errorText: { fontSize: 15, textAlign: 'center', marginBottom: 8 },
  retryText: { fontSize: 15, marginTop: 4, textDecorationLine: 'underline' },
});
