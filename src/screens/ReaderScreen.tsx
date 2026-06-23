import React, { useState, useEffect, useCallback, useRef } from 'react';
import {
  View, Text, ScrollView, TouchableOpacity, StyleSheet, Dimensions,
  StatusBar, FlatList, Alert, GestureResponderEvent,
} from 'react-native';
import { useTheme } from '../context/ThemeContext';
import { LoadingIndicator, ErrorView } from '../components/LoadingIndicator';
import { api } from '../services/api';
import { saveReadProgress } from '../services/storage';
import { FONT_SIZES } from '../utils/constants';
import type { NativeStackNavigationProp } from '@react-navigation/native-stack';
import type { RouteProp } from '@react-navigation/native';
import type { ChapterContent, ReaderFontSize } from '../types';

interface Props {
  navigation: NativeStackNavigationProp<any>;
  route: RouteProp<{
    params: {
      bookId: string;
      bookTitle: string;
      chapterId: number;   // index in chapters array
      chapters: { id: string; title: string }[];
    };
  }, 'params'>;
}

const { width: SCREEN_WIDTH, height: SCREEN_HEIGHT } = Dimensions.get('window');

export function ReaderScreen({ navigation, route }: Props) {
  const { bookId, bookTitle, chapterId: initialIndex, chapters } = route.params;
  const { theme, colors, fontSize, readerFontSize, setReaderFontSize } = useTheme();
  const [content, setContent] = useState<ChapterContent | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [showToolbar, setShowToolbar] = useState(false);
  const [showSettings, setShowSettings] = useState(false);
  const [pageIndex, setPageIndex] = useState(0);
  const [chapterIndex, setChapterIndex] = useState(initialIndex);
  const [pages, setPages] = useState<string[]>([]);

  const currentChapter = chapters[chapterIndex];

  // Fetch chapter content
  const fetchChapter = useCallback(async (chIdx: number) => {
    try {
      setLoading(true);
      setError(null);
      const ch = chapters[chIdx];
      if (!ch) return;
      const result = await api.getChapter(bookId, ch.id);
      setContent(result);
      setChapterIndex(chIdx);
      setPageIndex(0);
      saveReadProgress(bookId, ch.id, result.chapterTitle, bookTitle);
    } catch (err: any) {
      setError(err.message || '加载失败');
    } finally {
      setLoading(false);
    }
  }, [bookId, bookTitle, chapters]);

  useEffect(() => {
    fetchChapter(initialIndex);
  }, [fetchChapter, initialIndex]);

  // Paginate content whenever content or font size changes
  useEffect(() => {
    if (!content) return;
    const paras = content.content.split('\n').filter((p) => p.trim());
    // First page: chapter title + first N paragraphs
    // Remaining pages: next N paragraphs
    const lineH = fontSize * 1.8;
    const paddingTop = 80; // space for chapter title on first page
    const paddingHorizontal = 40;
    const paddingBottom = 60;
    const usableHeight = SCREEN_HEIGHT - paddingTop - paddingBottom;
    const linesPerPage = Math.max(1, Math.floor(usableHeight / lineH));

    const allPages: string[] = [];
    let currentLines = 0;
    let currentPage: string[] = [];

    for (const para of paras) {
      const paraLines = Math.max(1, Math.ceil(para.length / Math.floor((SCREEN_WIDTH - paddingHorizontal) / fontSize)));
      if (currentLines + paraLines > linesPerPage && currentPage.length > 0) {
        allPages.push(currentPage.join('\n\n'));
        currentPage = [para];
        currentLines = paraLines;
      } else {
        currentPage.push(para);
        currentLines += paraLines;
      }
    }
    if (currentPage.length > 0) {
      allPages.push(currentPage.join('\n\n'));
    }

    setPages(allPages.length > 0 ? allPages : ['（本章暂无内容）']);
  }, [content, fontSize]);

  const goToPage = (delta: number) => {
    const newPage = pageIndex + delta;
    if (newPage >= 0 && newPage < pages.length) {
      setPageIndex(newPage);
    } else if (newPage >= pages.length) {
      // Last page -> next chapter
      if (chapterIndex < chapters.length - 1) {
        fetchChapter(chapterIndex + 1);
      } else {
        Alert.alert('提示', '已是最后一章');
      }
    } else if (newPage < 0) {
      // Before first page -> prev chapter
      if (chapterIndex > 0) {
        fetchChapter(chapterIndex - 1);
      } else {
        Alert.alert('提示', '已是第一章');
      }
    }
  };

  // Tap handling: left 30% = prev, center 40% = menu, right 30% = next
  const handleTap = (e: GestureResponderEvent) => {
    if (showSettings) { setShowSettings(false); return; }
    const x = e.nativeEvent.locationX;
    if (x < SCREEN_WIDTH * 0.3) {
      goToPage(-1);
    } else if (x > SCREEN_WIDTH * 0.7) {
      goToPage(1);
    } else {
      setShowToolbar(!showToolbar);
      if (showToolbar) setShowSettings(false);
    }
  };

  if (loading && !content) return <LoadingIndicator text="加载章节..." />;
  if (error && !content) return <ErrorView message={error} onRetry={() => fetchChapter(chapterIndex)} />;

  const isFirstChapter = chapterIndex <= 0;
  const isLastChapter = chapterIndex >= chapters.length - 1;
  const isFirstPage = pageIndex <= 0;
  const isLastPage = pageIndex >= pages.length - 1;

  return (
    <View style={[styles.container, { backgroundColor: colors.readerBg }]}>
      <StatusBar hidden={!showToolbar} />

      {/* Top bar */}
      {showToolbar && (
        <View style={[styles.topBar, { backgroundColor: colors.surface, borderBottomColor: colors.border }]}>
          <TouchableOpacity onPress={() => navigation.goBack()} style={styles.backBtn}>
            <Text style={{ color: colors.text, fontSize: 16 }}>{'<'}</Text>
          </TouchableOpacity>
          <Text style={[styles.topTitle, { color: colors.text }]} numberOfLines={1}>
            {content?.chapterTitle || ''}
          </Text>
          <View style={{ width: 40 }} />
        </View>
      )}

      {/* Loading overlay */}
      {loading && content && (
        <View style={[styles.loadingOverlay, { backgroundColor: theme === 'dark' ? 'rgba(0,0,0,0.85)' : 'rgba(255,255,255,0.85)' }]}>
          <LoadingIndicator text="加载中..." />
        </View>
      )}

      {/* Page content with tap handling */}
      <TouchableOpacity
        style={styles.contentArea}
        activeOpacity={1}
        onPress={handleTap}>
        <ScrollView
          key={`page-${chapterIndex}-${pageIndex}`}
          style={styles.pageScroll}
          contentContainerStyle={styles.pageContent}
          showsVerticalScrollIndicator={false}>
          {/* Chapter title on first page of each chapter */}
          {pageIndex === 0 && content && (
            <Text style={[styles.chapterTitle, {
              color: colors.readerText,
              fontSize: fontSize + 6,
              lineHeight: (fontSize + 6) * 1.6,
            }]}>
              {content.chapterTitle}
            </Text>
          )}
          {/* Page paragraphs */}
          {(pages[pageIndex] || '').split('\n\n').map((para, i) => (
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
        </ScrollView>
      </TouchableOpacity>

      {/* Page indicator */}
      {showToolbar && (
        <View style={[styles.pageIndicator, { backgroundColor: colors.surface, borderTopColor: colors.border }]}>
          <Text style={{ color: colors.textSecondary, fontSize: 12 }}>
            {pageIndex + 1} / {pages.length}
          </Text>
        </View>
      )}

      {/* Bottom toolbar */}
      {showToolbar && (
        <View style={[styles.bottomToolbar, { backgroundColor: colors.surface, borderTopColor: colors.border }]}>
          <TouchableOpacity
            style={styles.toolbarBtn}
            onPress={() => fetchChapter(chapterIndex - 1)}
            disabled={isFirstChapter}>
            <Text style={{ color: isFirstChapter ? colors.border : colors.text, fontSize: 13 }}>上一章</Text>
          </TouchableOpacity>
          <TouchableOpacity style={styles.toolbarBtn} onPress={() => setShowSettings(!showSettings)}>
            <Text style={{ color: colors.text, fontSize: 13 }}>设置</Text>
          </TouchableOpacity>
          <TouchableOpacity
            style={styles.toolbarBtn}
            onPress={() => navigation.navigate('ChapterList', { bookId, bookTitle, chapters })}>
            <Text style={{ color: colors.text, fontSize: 13 }}>目录</Text>
          </TouchableOpacity>
          <TouchableOpacity
            style={styles.toolbarBtn}
            onPress={() => fetchChapter(chapterIndex + 1)}
            disabled={isLastChapter}>
            <Text style={{ color: isLastChapter ? colors.border : colors.text, fontSize: 13 }}>下一章</Text>
          </TouchableOpacity>
        </View>
      )}

      {/* Settings panel */}
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
          <Text style={[styles.settingsHint, { color: colors.textSecondary }]}>
            左侧: 上一页 | 右侧: 下一页 | 中间: 菜单
          </Text>
        </View>
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1 },
  topBar: {
    flexDirection: 'row', alignItems: 'center',
    paddingTop: 40, paddingBottom: 10, paddingHorizontal: 12,
    borderBottomWidth: StyleSheet.hairlineWidth,
  },
  backBtn: { width: 40, padding: 8 },
  topTitle: { flex: 1, textAlign: 'center', fontSize: 15, fontWeight: '500' },
  contentArea: { flex: 1 },
  pageScroll: { flex: 1 },
  pageContent: { padding: 20, paddingTop: 30, paddingBottom: 60 },
  loadingOverlay: {
    ...StyleSheet.absoluteFill,
    justifyContent: 'center', alignItems: 'center', zIndex: 10,
  },
  chapterTitle: {
    fontWeight: '700', textAlign: 'center', marginBottom: 20, paddingBottom: 16,
    borderBottomWidth: StyleSheet.hairlineWidth, borderBottomColor: '#CCC',
  },
  paragraph: { marginBottom: 8 },
  pageIndicator: {
    paddingVertical: 6, borderTopWidth: StyleSheet.hairlineWidth, alignItems: 'center',
  },
  bottomToolbar: {
    flexDirection: 'row', paddingBottom: 30, paddingTop: 10,
    borderTopWidth: StyleSheet.hairlineWidth,
  },
  toolbarBtn: { flex: 1, alignItems: 'center', paddingVertical: 8 },
  settingsPanel: { padding: 16, paddingBottom: 30, borderTopWidth: StyleSheet.hairlineWidth },
  settingsLabel: { fontSize: 14, fontWeight: '600', marginBottom: 8 },
  fontSizeRow: { flexDirection: 'row', gap: 10 },
  fontSizeBtn: { flex: 1, padding: 10, borderRadius: 6, alignItems: 'center', backgroundColor: '#F0F0F0' },
  settingsHint: { fontSize: 12, marginTop: 8, textAlign: 'center' },
});
