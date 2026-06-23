import React, { useState, useEffect, useCallback } from 'react';
import {
  View, Text, FlatList, StyleSheet, RefreshControl,
} from 'react-native';
import { useTheme } from '../context/ThemeContext';
import { NovelCard } from '../components/NovelCard';
import { LoadingIndicator, ErrorView } from '../components/LoadingIndicator';
import { api } from '../services/api';
import type { NativeStackNavigationProp } from '@react-navigation/native-stack';
import type { HomeItem } from '../types';

interface Props {
  navigation: NativeStackNavigationProp<any>;
}

export function RankingScreen({ navigation }: Props) {
  const { colors } = useTheme();
  const [novels, setNovels] = useState<HomeItem[]>([]);
  const [page, setPage] = useState(1);
  const [totalPages, setTotalPages] = useState(1);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [refreshing, setRefreshing] = useState(false);

  const fetchData = useCallback(async (p: number = 1) => {
    try {
      setError(null);
      const result = await api.getRanking(p);
      setNovels(p === 1 ? result.novels : (prev) => [...prev, ...result.novels]);
      setTotalPages(result.totalPages);
      setPage(p);
    } catch (err: any) {
      setError(err.message || '加载失败');
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  }, []);

  useEffect(() => { fetchData(); }, [fetchData]);

  if (loading) return <LoadingIndicator text="加载中..." />;
  if (error && novels.length === 0) return <ErrorView message={error} onRetry={() => fetchData()} />;

  return (
    <FlatList
      data={novels}
      keyExtractor={(item) => item.id + '-' + (item.rank || '')}
      style={[styles.container, { backgroundColor: colors.background }]}
      refreshControl={<RefreshControl refreshing={refreshing} onRefresh={() => { setRefreshing(true); fetchData(); }} tintColor={colors.primary} />}
      renderItem={({ item }) => (
        <NovelCard
          novel={item}
          rank={item.rank}
          onPress={() => navigation.navigate('NovelDetail', { id: item.id })}
        />
      )}
      ListHeaderComponent={
        <Text style={[styles.listHeader, { color: colors.textSecondary }]}>小说排行榜</Text>
      }
      ListFooterComponent={
        page < totalPages ? (
          <Text
            style={[styles.loadMore, { color: colors.primary }]}
            onPress={() => fetchData(page + 1)}>
            加载更多
          </Text>
        ) : (
          <Text style={[styles.endText, { color: colors.textSecondary }]}>— 没有更多了 —</Text>
        )
      }
    />
  );
}

const styles = StyleSheet.create({
  container: { flex: 1 },
  listHeader: { padding: 14, paddingBottom: 4, fontSize: 13 },
  loadMore: { textAlign: 'center', padding: 16, fontSize: 14 },
  endText: { textAlign: 'center', padding: 20, fontSize: 13 },
});
