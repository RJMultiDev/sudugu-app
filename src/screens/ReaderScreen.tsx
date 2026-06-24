import React, { useState, useEffect, useCallback, useRef } from 'react';
import {
  View, Text, ScrollView, StyleSheet, TouchableOpacity, Dimensions,
  StatusBar, Alert,
} from 'react-native';
import { useTheme } from '../context/ThemeContext';
import { LoadingIndicator, ErrorView } from '../components/LoadingIndicator';
import { api } from '../services/api';
import { saveReadProgress, getReadProgress } from '../services/storage';
import { FONT_SIZES } from '../utils/constants';
import { getChapters } from '../services/chaptersCache';
import type { NativeStackNavigationProp } from '@react-navigation/native-stack';
import type { RouteProp } from '@react-navigation/native';
import type { ChapterContent, ReaderFontSize } from '../types';

interface Props {
  navigation: NativeStackNavigationProp<any>;
  route: RouteProp<{
    params: {
      bookId: string;
      bookTitle: string;
      chapterId: number;
    };
  }, 'params'>;
}

const { width: SCREEN_WIDTH } = Dimensions.get('window');

export function ReaderScreen({ navigation, route }: Props) {
  const { bookId, bookTitle, chapterId: initialIndex } = route.params;
  const chapters = getChapters(bookId);
  const { theme, colors, fontSize, readerFontSize, setReaderFontSize } = useTheme();
  const [content, setContent] = useState<ChapterContent | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [showSettings, setShowSettings] = useState(false);
  const [chapterIndex, setChapterIndex] = useState(initialIndex);
  const scrollRef = useRef<ScrollView>(null);
  const restoredRef = useRef(false);

  const currentChapter = chapters[chapterIndex];

  // Fetch chapter content
  const fetchChapter = useCallback(async (chIdx: number) => {
    try {
      setLoading(true);
      setError(null);
      const ch = chapters[chIdx];
      if (!ch) {
        setError('章节不存在');
        return;
      }
      const result = await api.getChapter(bookId, ch.id);
      setContent(result);
      setChapterIndex(chIdx);
      saveReadProgress(bookId, ch.id, result.chapterTitle, bookTitle);
      restoredRef.current = false;
      // 切换章节时滚到顶部
      setTimeout(() => scrollRef.current?.scrollTo({ y: 0, animated: false }), 50);
    } catch (err: any) {
      setError(err.message || '加载失败');
    } finally {
      setLoading(false);
    }
  }, [bookId, bookTitle, chapters]);

  useEffect(() => {
    fetchChapter(initialIndex);
  }, [fetchChapter, initialIndex]);

  // 加载完内容后，恢复阅读位置
  useEffect(() => {
    if (!content || loading || restoredRef.current) return;
    restoredRef.current = true;
    const restoreScroll = async () => {
      const progress = await getReadProgress(bookId);
      if (progress && progress.chapterId === content.chapterId && progress.scrollY) {
        // 延迟一帧确保内容已渲染
        setTimeout(() => {
          scrollRef.current?.scrollTo({ y: progress.scrollY!, animated: false });
        }, 100);
      }
    };
    restoreScroll();
  }, [content, loading, bookId]);

  // 滚动时记录位置（节流：每 500ms 一次）
  const lastSaveRef = useRef(0);
  const onScroll = useCallback((e: any) => {
    if (!content) return;
    const y = e.nativeEvent.contentOffset.y;
    const now = Date.now();
    if (now - lastSaveRef.current > 500) {
      lastSaveRef.current = now;
      saveReadProgress(bookId, content.chapterId, content.chapterTitle, bookTitle, y);
    }
  }, [bookId, bookTitle, content]);

  const goToChapter = (delta: number) => {
    const newIdx = chapterIndex + delta;
    if (newIdx >= 0 && newIdx < chapters.length) {
      fetchChapter(newIdx);
    } else if (newIdx >= chapters.length) {
      Alert.alert('提示', '已是最后一章');
    } else {
      Alert.alert('提示', '已是第一章');
    }
  };

  if (loading && !content) return <LoadingIndicator text="加载章节..." />;
  if (error && !content) return <ErrorView message={error} onRetry={() => fetchChapter(chapterIndex)} />;

  const isFirstChapter = chapterIndex <= 0;
  const isLastChapter = chapterIndex >= chapters.length - 1;
  const paragraphs = content?.content.split('\n').filter((p) => p.trim()) || [];

  return (
    <View style={[styles.container, { backgroundColor: colors.readerBg }]}>
      <StatusBar barStyle={theme === 'dark' ? 'light-content' : 'dark-content'} />

      {/* 始终显示的顶部工具栏 */}
      <View style={[styles.topBar, { backgroundColor: colors.surface, borderBottomColor: colors.border }]}>
        <TouchableOpacity onPress={() => navigation.goBack()} style={styles.backBtn}>
          <Text style={{ color: colors.text, fontSize: 18 }}>{'<'}</Text>
        </TouchableOpacity>
        <View style={styles.topTitleWrap}>
          <Text style={[styles.topTitle, { color: colors.text }]} numberOfLines={1}>
            {content?.chapterTitle || ''}
          </Text>
          <Text style={[styles.topSub, { color: colors.textSecondary }]} numberOfLines={1}>
            {bookTitle} · 第 {chapterIndex + 1}/{chapters.length} 章
          </Text>
        </View>
        <TouchableOpacity onPress={() => setShowSettings(!showSettings)} style={styles.settingBtn}>
          <Text style={{ color: colors.text, fontSize: 16 }}>Aa</Text>
        </TouchableOpacity>
      </View>

      {/* 内容区（可滚动） */}
      <ScrollView
        ref={scrollRef}
        style={styles.content}
        contentContainerStyle={styles.contentInner}
        onScroll={onScroll}
        scrollEventThrottle={16}>
        {/* 章节标题 */}
        {content && (
          <Text style={[styles.chapterTitle, {
            color: colors.readerText,
            fontSize: fontSize + 6,
            lineHeight: (fontSize + 6) * 1.6,
          }]}>
            {content.chapterTitle}
          </Text>
        )}
        {/* 段落 */}
        {paragraphs.map((para, i) => (
          <Text
            key={i}
            style={[styles.paragraph, {
              color: colors.readerText,
              fontSize,
              lineHeight: fontSize * 1.8,
            }]}>
            {para}
          </Text>
        ))}
        {/* 章节末：上一章/下一章 */}
        <View style={[styles.chapterNav, { borderTopColor: colors.border }]}>
          <TouchableOpacity
            style={styles.chapterNavBtn}
            onPress={() => goToChapter(-1)}
            disabled={isFirstChapter}>
            <Text style={{
              color: isFirstChapter ? colors.border : colors.primary,
              fontSize: 14, fontWeight: '500',
            }}>上一章</Text>
          </TouchableOpacity>
          <TouchableOpacity
            style={styles.chapterNavBtn}
            onPress={() => navigation.navigate('ChapterList', { bookId, bookTitle })}>
            <Text style={{ color: colors.text, fontSize: 14 }}>目录</Text>
          </TouchableOpacity>
          <TouchableOpacity
            style={styles.chapterNavBtn}
            onPress={() => goToChapter(1)}
            disabled={isLastChapter}>
            <Text style={{
              color: isLastChapter ? colors.border : colors.primary,
              fontSize: 14, fontWeight: '500',
            }}>下一章</Text>
          </TouchableOpacity>
        </View>
        <View style={{ height: 40 }} />
      </ScrollView>

      {/* 字体设置面板（顶部栏右侧 Aa 触发） */}
      {showSettings && (
        <View style={[styles.settingsPanel, { backgroundColor: colors.surface, borderTopColor: colors.border }]}>
          <Text style={[styles.settingsLabel, { color: colors.text }]}>字体大小</Text>
          <View style={styles.fontSizeRow}>
            {(['small', 'medium', 'large', 'xlarge'] as ReaderFontSize[]).map((size) => (
              <TouchableOpacity
                key={size}
                style={[styles.fontSizeBtn, readerFontSize === size && { backgroundColor: colors.primary }]}
                onPress={() => setReaderFontSize(size)}>
                <Text style={{ color: readerFontSize === size ? '#FFF' : colors.text, fontSize: FONT_SIZES[size] }}>
                  {size === 'small' ? '小' : size === 'medium' ? '中' : size === 'large' ? '大' : '特大'}
                </Text>
              </TouchableOpacity>
            ))}
          </View>
        </View>
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1 },
  topBar: {
    flexDirection: 'row',
    alignItems: 'center',
    paddingTop: 40,
    paddingBottom: 8,
    paddingHorizontal: 12,
    borderBottomWidth: StyleSheet.hairlineWidth,
  },
  backBtn: { width: 36, paddingVertical: 8 },
  topTitleWrap: { flex: 1, alignItems: 'center' },
  topTitle: { fontSize: 15, fontWeight: '600' },
  topSub: { fontSize: 11, marginTop: 2 },
  settingBtn: { width: 36, paddingVertical: 8, alignItems: 'flex-end' },
  content: { flex: 1 },
  contentInner: { padding: 20, paddingTop: 16, paddingBottom: 20 },
  chapterTitle: {
    fontWeight: '700',
    textAlign: 'center',
    marginBottom: 20,
    paddingBottom: 16,
    borderBottomWidth: StyleSheet.hairlineWidth,
    borderBottomColor: 'rgba(128,128,128,0.3)',
  },
  paragraph: { marginBottom: 8 },
  chapterNav: {
    flexDirection: 'row',
    marginTop: 32,
    paddingTop: 20,
    borderTopWidth: StyleSheet.hairlineWidth,
  },
  chapterNavBtn: { flex: 1, alignItems: 'center', paddingVertical: 12 },
  settingsPanel: { padding: 16, paddingBottom: 24, borderTopWidth: StyleSheet.hairlineWidth },
  settingsLabel: { fontSize: 14, fontWeight: '600', marginBottom: 8 },
  fontSizeRow: { flexDirection: 'row', gap: 10 },
  fontSizeBtn: { flex: 1, padding: 10, borderRadius: 6, alignItems: 'center', backgroundColor: '#F0F0F0' },
});
