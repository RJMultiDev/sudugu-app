import React, { useState, useEffect } from 'react';
import {
  View, Text, FlatList, TouchableOpacity, StyleSheet,
} from 'react-native';
import { useTheme } from '../context/ThemeContext';
import { getReadHistory } from '../services/storage';
import type { NativeStackNavigationProp } from '@react-navigation/native-stack';
import type { ReadProgress } from '../types';

interface Props {
  navigation: NativeStackNavigationProp<any>;
}

export function ReadHistoryScreen({ navigation }: Props) {
  const { colors } = useTheme();
  const [history, setHistory] = useState<ReadProgress[]>([]);

  useEffect(() => {
    getReadHistory().then(setHistory);
  }, []);

  if (history.length === 0) {
    return (
      <View style={[styles.empty, { backgroundColor: colors.background }]}>
        <Text style={[styles.emptyText, { color: colors.textSecondary }]}>暂无阅读记录</Text>
      </View>
    );
  }

  return (
    <FlatList
      data={history}
      keyExtractor={(item) => item.bookId}
      style={[styles.container, { backgroundColor: colors.background }]}
      renderItem={({ item }) => (
        <TouchableOpacity
          style={[styles.item, { borderBottomColor: colors.border }]}
          onPress={() => navigation.navigate('NovelDetail', { id: item.bookId })}>
          <View style={styles.info}>
            <Text style={[styles.title, { color: colors.text }]} numberOfLines={1}>{item.bookTitle}</Text>
            <Text style={[styles.chapter, { color: colors.textSecondary }]} numberOfLines={1}>
              读到: {item.chapterTitle}
            </Text>
          </View>
          <Text style={[styles.time, { color: colors.textSecondary }]}>
            {new Date(item.timestamp).toLocaleDateString('zh-CN')}
          </Text>
        </TouchableOpacity>
      )}
    />
  );
}

const styles = StyleSheet.create({
  container: { flex: 1 },
  empty: { flex: 1, justifyContent: 'center', alignItems: 'center' },
  emptyText: { fontSize: 15 },
  item: {
    flexDirection: 'row',
    padding: 14,
    borderBottomWidth: StyleSheet.hairlineWidth,
    alignItems: 'center',
  },
  info: { flex: 1 },
  title: { fontSize: 15, fontWeight: '500', marginBottom: 4 },
  chapter: { fontSize: 13 },
  time: { fontSize: 12 },
});
