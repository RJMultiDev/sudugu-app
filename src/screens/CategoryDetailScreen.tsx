import React, { useState, useEffect, useCallback } from 'react';
import {
  View, Text, FlatList, TouchableOpacity, StyleSheet, RefreshControl,
} from 'react-native';
import { useTheme } from '../context/ThemeContext';
import { NovelCard } from '../components/NovelCard';
import { LoadingIndicator, ErrorView } from '../components/LoadingIndicator';
import { api } from '../services/api';
import type { NativeStackNavigationProp } from '@react-navigation/native-stack';
import type { RouteProp } from '@react-navigation/native';
import type { Novel } from '../types';

interface Props {
  navigation: NativeStackNavigationProp<any>;
  route: RouteProp<{ params: { slug: string; name: string } }, 'params'>;
}

export function CategoryDetailScreen({ navigation, route }: Props) {
  const { slug, name } = route.params;
  const { colors } = useTheme();
  const [novels, setNovels] = useState<Novel[]>([]);
  const [page, setPage] = useState(1);
  const [totalPages, setTotalPages] = useState(1);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [refreshing, setRefreshing] = useState(false);

  const fetchData = useCallback(async (p: number = 1) => {
    try {
      setError(null);
      const result = await api.getCategory(slug, p);
      setNovels(p === 1 ? result.novels : (prev) => [...prev, ...result.novels]);
      setTotalPages(result.totalPages);
      setPage(p);
    } catch (err: any) {
      setError(err.message || '加载失败');
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  }, [slug]);

  useEffect(() => { fetchData(); }, [fetchData]);

  if (loading) return <LoadingIndicator text="加载中..." />;
  if (error && novels.length === 0) return <ErrorView message={error} onRetry={() => fetchData()} />;

  return (
    <FlatList
      data={novels}
      keyExtractor={(item) => item.id}
      style={[styles.container, { backgroundColor: colors.background }]}
      refreshControl={<RefreshControl refreshing={refreshing} onRefresh={() => { setRefreshing(true); fetchData(); }} tintColor={colors.primary} />}
      renderItem={({ item }) => (
        <NovelCard novel={item} onPress={() => navigation.navigate('NovelDetail', { id: item.id })} />
      )}
      ListFooterComponent={
        page < totalPages ? (
          <TouchableOpacity style={[styles.loadMore, { borderColor: colors.primary }]} onPress={() => fetchData(page + 1)}>
            <Text style={[styles.loadMoreText, { color: colors.primary }]}>加载更多</Text>
          </TouchableOpacity>
        ) : (
          <Text style={[styles.endText, { color: colors.textSecondary }]}>— 没有更多了 —</Text>
        )
      }
    />
  );
}

const styles = StyleSheet.create({
  container: { flex: 1 },
  loadMore: {
    margin: 16,
    padding: 12,
    borderWidth: 1,
    borderRadius: 6,
    alignItems: 'center',
  },
  loadMoreText: { fontSize: 14, fontWeight: '500' },
  endText: { textAlign: 'center', padding: 20, fontSize: 13 },
});
