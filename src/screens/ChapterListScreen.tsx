import React, { useState, useEffect } from 'react';
import {
  View, Text, FlatList, TouchableOpacity, StyleSheet,
} from 'react-native';
import { useTheme } from '../context/ThemeContext';
import { getReadProgress } from '../services/storage';
import type { NativeStackNavigationProp } from '@react-navigation/native-stack';
import type { RouteProp } from '@react-navigation/native';
import type { ReadProgress } from '../types';
import { getChapters } from '../services/chaptersCache';

interface Props {
  navigation: NativeStackNavigationProp<any>;
  route: RouteProp<{
    params: {
      bookId: string;
      bookTitle: string;
    };
  }, 'params'>;
}

export function ChapterListScreen({ navigation, route }: Props) {
  const { bookId, bookTitle } = route.params;
  const chapters = getChapters(bookId);
  const { colors } = useTheme();
  const [currentChapterId, setCurrentChapterId] = useState<string | null>(null);

  useEffect(() => {
    getReadProgress(bookId).then((p) => {
      if (p) setCurrentChapterId(p.chapterId);
    });
  }, [bookId]);

  const goToChapter = (targetId: string) => {
    const idx = chapters.findIndex((c) => c.id === targetId);
    navigation.navigate('Reader', {
      bookId,
      bookTitle,
      chapterId: idx >= 0 ? idx : 0,
    });
  };

  return (
    <View style={[styles.container, { backgroundColor: colors.background }]}>
      <Text style={[styles.header, { color: colors.textSecondary }]}>
        {bookTitle} · 共{chapters.length}章
      </Text>
      <FlatList
        data={chapters}
        keyExtractor={(item) => item.id}
        renderItem={({ item, index }) => {
          const isCurrent = item.id === currentChapterId;
          return (
            <TouchableOpacity
              style={[
                styles.chapterItem,
                { borderBottomColor: colors.border },
                isCurrent && { backgroundColor: colors.primary + '10' },
              ]}
              onPress={() => goToChapter(item.id)}>
              <Text
                style={[
                  styles.chapterTitle,
                  { color: isCurrent ? colors.primary : colors.text },
                ]}
                numberOfLines={1}>
                {item.title}
              </Text>
            </TouchableOpacity>
          );
        }}
        ListHeaderComponent={null}
        initialScrollIndex = {(() => {
          const idx = chapters.findIndex((c) => c.id === currentChapterId);
          return idx >= 0 ? idx : undefined;
        })()}
        getItemLayout={(_, index) => ({
          length: 44,
          offset: 44 * index,
          index,
        })}
      />
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1 },
  header: { fontSize: 13, padding: 12, paddingBottom: 6, textAlign: 'center' },
  chapterItem: {
    paddingHorizontal: 16,
    paddingVertical: 12,
    borderBottomWidth: StyleSheet.hairlineWidth,
  },
  chapterTitle: { fontSize: 14 },
});
