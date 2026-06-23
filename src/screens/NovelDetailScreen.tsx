import React, { useState, useEffect, useCallback } from 'react';
import {
  View, Text, ScrollView, Image, TouchableOpacity, StyleSheet, RefreshControl, Linking,
} from 'react-native';
import { useTheme } from '../context/ThemeContext';
import { useBookshelf } from '../context/BookshelfContext';
import { LoadingIndicator, ErrorView } from '../components/LoadingIndicator';
import { api } from '../services/api';
import { getReadProgress } from '../services/storage';
import type { NativeStackNavigationProp } from '@react-navigation/native-stack';
import type { RouteProp } from '@react-navigation/native';
import type { Novel, ReadProgress } from '../types';

interface Props {
  navigation: NativeStackNavigationProp<any>;
  route: RouteProp<{ params: { id: string } }, 'params'>;
}

export function NovelDetailScreen({ navigation, route }: Props) {
  const { id } = route.params;
  const { colors } = useTheme();
  const { addBook, removeBook, isBookInShelf } = useBookshelf();
  const [novel, setNovel] = useState<(Novel & { chapters: { id: string; title: string }[]; txtLinks?: { url: string; label: string }[] }) | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [inShelf, setInShelf] = useState(false);
  const [readProgress, setReadProgress] = useState<ReadProgress | null>(null);
  const [refreshing, setRefreshing] = useState(false);

  const fetchNovel = useCallback(async () => {
    try {
      setError(null);
      const result = await api.getNovelDetail(id);
      setNovel(result);
      setInShelf(await isBookInShelf(id));
      setReadProgress(await getReadProgress(id));
    } catch (err: any) {
      setError(err.message || '加载失败');
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  }, [id, isBookInShelf]);

  useEffect(() => { fetchNovel(); }, [fetchNovel]);

  const toggleBookshelf = async () => {
    if (!novel) return;
    if (inShelf) {
      await removeBook(id);
    } else {
      await addBook({
        id: novel.id, title: novel.title, author: novel.author,
        cover: novel.cover, status: novel.status, category: novel.category,
      });
    }
    setInShelf((prev) => !prev);
  };

  const startReading = (targetChapterId?: string) => {
    if (!novel?.chapters?.length) return;
    // targetChapterId is the chapter's original ID from the novel data
    const resolvedId = targetChapterId || readProgress?.chapterId || novel.chapters[0].id;
    const idx = novel.chapters.findIndex((c) => c.id === resolvedId);
    navigation.navigate('Reader', {
      bookId: id,
      bookTitle: novel.title,
      chapterId: idx >= 0 ? idx : 0,
      chapters: novel.chapters,
    });
  };

  if (loading) return <LoadingIndicator text="加载中..." />;
  if (error || !novel) return <ErrorView message={error || '加载失败'} onRetry={fetchNovel} />;

  return (
    <ScrollView
      style={[styles.container, { backgroundColor: colors.background }]}
      refreshControl={<RefreshControl refreshing={refreshing} onRefresh={() => { setRefreshing(true); fetchNovel(); }} tintColor={colors.primary} />}>
      {/* Header */}
      <View style={[styles.header, { backgroundColor: colors.surface }]}>
        {novel.cover && <Image source={{ uri: novel.cover }} style={styles.cover} resizeMode="cover" />}
        <View style={styles.headerInfo}>
          <Text style={[styles.title, { color: colors.text }]} numberOfLines={2}>{novel.title}</Text>
          <Text style={[styles.author, { color: colors.textSecondary }]}>{novel.author}</Text>
          <View style={styles.metaRow}>
            {novel.words && <Text style={[styles.meta, { color: colors.textSecondary }]}>{novel.words}</Text>}
            {novel.status && <Text style={[styles.meta, { color: colors.primary }]}>{novel.status}</Text>}
            {novel.category && <Text style={[styles.meta, { color: colors.textSecondary }]}>{novel.category}</Text>}
          </View>
        </View>
      </View>

      {/* Action buttons */}
      <View style={styles.actions}>
        <TouchableOpacity
          style={[styles.btn, { backgroundColor: inShelf ? colors.border : colors.primary }]}
          onPress={toggleBookshelf}>
          <Text style={styles.btnText}>{inShelf ? '已加入书架' : '加入书架'}</Text>
        </TouchableOpacity>
        <TouchableOpacity style={[styles.btn, { backgroundColor: colors.primary }]} onPress={() => startReading()}>
          <Text style={styles.btnText}>{readProgress ? '继续阅读' : '开始阅读'}</Text>
        </TouchableOpacity>
      </View>

      {readProgress && (
        <TouchableOpacity style={[styles.progressBanner, { backgroundColor: colors.primary + '15' }]} onPress={() => startReading()}>
          <Text style={{ color: colors.primary, fontSize: 13 }}>上次读到: {readProgress.chapterTitle}</Text>
        </TouchableOpacity>
      )}

      {/* Description */}
      {novel.description ? (
        <View style={[styles.section, { backgroundColor: colors.surface }]}>
          <Text style={[styles.sectionTitle, { color: colors.text }]}>简介</Text>
          <Text style={[styles.desc, { color: colors.textSecondary }]}>{novel.description}</Text>
        </View>
      ) : null}

      {/* TXT Download */}
      {novel.txtLinks && novel.txtLinks.length > 0 && (
        <View style={[styles.section, { backgroundColor: colors.surface }]}>
          <Text style={[styles.sectionTitle, { color: colors.text }]}>TXT 下载</Text>
          {novel.txtLinks.map((link, i) => (
            <TouchableOpacity
              key={i}
              style={[styles.txtItem, { borderBottomColor: colors.border }]}
              onPress={() => Linking.openURL(link.url)}>
              <Text style={{ color: colors.primary, fontSize: 14 }}>{link.label}</Text>
              <Text style={{ color: colors.textSecondary, fontSize: 12 }}>{'下载 >'}</Text>
            </TouchableOpacity>
          ))}
        </View>
      )}

      {/* Chapter list */}
      {novel.chapters?.length > 0 && (
        <View style={[styles.section, { backgroundColor: colors.surface }]}>
          <View style={styles.sectionHeader}>
            <Text style={[styles.sectionTitle, { color: colors.text }]}>目录 ({novel.chapters.length}章)</Text>
            <TouchableOpacity onPress={() => navigation.navigate('ChapterList', {
              bookId: id, bookTitle: novel.title, chapters: novel.chapters,
            })}>
              <Text style={{ color: colors.primary, fontSize: 13 }}>全部章节{'>'}</Text>
            </TouchableOpacity>
          </View>
          {novel.chapters.slice(-10).map((ch, i) => (
            <TouchableOpacity
              key={ch.id}
              style={[styles.chapterItem, { borderBottomColor: colors.border }]}
              onPress={() => {
                const idx = novel.chapters.findIndex((c) => c.id === ch.id);
                startReading(ch.id);
              }}>
              <Text style={[styles.chapterTitle, {
                color: readProgress?.chapterId === ch.id ? colors.primary : colors.text,
              }]} numberOfLines={1}>{ch.title}</Text>
            </TouchableOpacity>
          ))}
        </View>
      )}
      <View style={{ height: 30 }} />
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1 },
  header: { flexDirection: 'row', padding: 16 },
  cover: { width: 90, height: 120, borderRadius: 6 },
  headerInfo: { flex: 1, marginLeft: 14, justifyContent: 'center' },
  title: { fontSize: 18, fontWeight: '700', marginBottom: 4 },
  author: { fontSize: 14, marginBottom: 8 },
  metaRow: { flexDirection: 'row', gap: 10 },
  meta: { fontSize: 12 },
  actions: { flexDirection: 'row', padding: 12, gap: 10 },
  btn: { flex: 1, padding: 12, borderRadius: 6, alignItems: 'center' },
  btnText: { color: '#FFF', fontSize: 15, fontWeight: '600' },
  progressBanner: { marginHorizontal: 12, padding: 10, borderRadius: 6 },
  section: { marginTop: 8, padding: 14 },
  sectionHeader: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center' },
  sectionTitle: { fontSize: 16, fontWeight: '700', marginBottom: 8 },
  desc: { fontSize: 14, lineHeight: 22 },
  txtItem: {
    flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center',
    paddingVertical: 10, borderBottomWidth: StyleSheet.hairlineWidth,
  },
  chapterItem: { paddingVertical: 10, borderBottomWidth: StyleSheet.hairlineWidth },
  chapterTitle: { fontSize: 14 },
});
