import React, { useState, useEffect, useCallback } from 'react';
import {
  View, Text, ScrollView, TouchableOpacity, StyleSheet, FlatList, RefreshControl,
} from 'react-native';
import { useTheme } from '../context/ThemeContext';
import { NovelCard } from '../components/NovelCard';
import { LoadingIndicator, ErrorView } from '../components/LoadingIndicator';
import { api } from '../services/api';
import { CATEGORIES } from '../utils/constants';
import type { HomeData, HomeItem, Novel } from '../types';
import type { NativeStackNavigationProp } from '@react-navigation/native-stack';

interface Props {
  navigation: NativeStackNavigationProp<any>;
}

export function HomeScreen({ navigation }: Props) {
  const { colors } = useTheme();
  const [data, setData] = useState<HomeData | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [refreshing, setRefreshing] = useState(false);

  const fetchData = useCallback(async () => {
    try {
      setError(null);
      const result = await api.getHome();
      setData(result);
    } catch (err: any) {
      setError(err.message || '加载失败');
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  }, []);

  useEffect(() => { fetchData(); }, [fetchData]);

  const onRefresh = () => {
    setRefreshing(true);
    fetchData();
  };

  if (loading) return <LoadingIndicator text="加载中..." />;
  if (error || !data) return <ErrorView message={error || '加载失败'} onRetry={fetchData} />;

  return (
    <ScrollView
      style={[styles.container, { backgroundColor: colors.background }]}
      refreshControl={<RefreshControl refreshing={refreshing} onRefresh={onRefresh} tintColor={colors.primary} />}>
      {/* Search bar */}
      <TouchableOpacity
        style={[styles.searchBar, { backgroundColor: colors.surface, borderColor: colors.border }]}
        onPress={() => navigation.navigate('Search')}
        activeOpacity={0.8}>
        <Text style={[styles.searchPlaceholder, { color: colors.textSecondary }]}>
          搜索小说、作者
        </Text>
      </TouchableOpacity>

      {/* Quick Categories */}
      <View style={styles.categoriesContainer}>
        <ScrollView horizontal showsHorizontalScrollIndicator={false}>
          {CATEGORIES.map((cat) => (
            <TouchableOpacity
              key={cat.slug}
              style={styles.categoryItem}
              onPress={() => navigation.navigate('CategoryDetail', { slug: cat.slug, name: cat.name })}>
              <View style={[styles.categoryIcon, { backgroundColor: colors.primary + '15' }]}>
                <Text style={styles.categoryEmoji}>{cat.icon}</Text>
              </View>
              <Text style={[styles.categoryName, { color: colors.text }]} numberOfLines={1}>
                {cat.name}
              </Text>
            </TouchableOpacity>
          ))}
        </ScrollView>
      </View>

      {/* Rankings section */}
      {data.rankings.length > 0 && (
        <Section
          title="阅读排行"
          colors={colors}
          onMore={() => navigation.navigate('Ranking')}>
          {data.rankings.slice(0, 6).map((item, i) => (
            <NovelCard
              key={`${item.id}-${i}`}
              novel={item}
              rank={i + 1}
              onPress={() => navigation.navigate('NovelDetail', { id: item.id })}
            />
          ))}
        </Section>
      )}

      {/* Latest updates */}
      {data.latestUpdates.length > 0 && (
        <Section title="最新更新" colors={colors}
          onMore={() => navigation.navigate('Ranking')}>
          {data.latestUpdates.map((item, i) => (
            <NovelCard
              key={item.id + '-latest-' + i}
              novel={item}
              onPress={() => navigation.navigate('NovelDetail', { id: item.id })}
            />
          ))}
        </Section>
      )}

      {/* Completed novels */}
      {data.completedNovels.length > 0 && (
        <Section title="完结精品" colors={colors}>
          <FlatList
            horizontal
            data={data.completedNovels}
            keyExtractor={(item) => item.id}
            showsHorizontalScrollIndicator={false}
            renderItem={({ item }) => (
              <NovelCard
                novel={item}
                variant="vertical"
                onPress={() => navigation.navigate('NovelDetail', { id: item.id })}
              />
            )}
            contentContainerStyle={styles.horizontalList}
          />
        </Section>
      )}

      <View style={{ height: 20 }} />
    </ScrollView>
  );
}

function Section({ title, colors, children, onMore }: {
  title: string; colors: any; children: React.ReactNode; onMore?: () => void;
}) {
  return (
    <View style={[styles.section, { backgroundColor: colors.surface }]}>
      <View style={styles.sectionHeader}>
        <Text style={[styles.sectionTitle, { color: colors.text }]}>{title}</Text>
        {onMore && (
          <TouchableOpacity onPress={onMore}>
            <Text style={[styles.sectionMore, { color: colors.primary }]}>更多{'>'}</Text>
          </TouchableOpacity>
        )}
      </View>
      {children}
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1 },
  searchBar: {
    margin: 12,
    padding: 12,
    borderRadius: 20,
    borderWidth: 1,
  },
  searchPlaceholder: { fontSize: 14 },
  categoriesContainer: {
    paddingHorizontal: 12,
    paddingBottom: 8,
  },
  categoryItem: {
    alignItems: 'center',
    width: 60,
    marginRight: 8,
  },
  categoryIcon: {
    width: 44,
    height: 44,
    borderRadius: 22,
    justifyContent: 'center',
    alignItems: 'center',
    marginBottom: 4,
  },
  categoryEmoji: { fontSize: 20 },
  categoryName: { fontSize: 11 },
  section: {
    marginTop: 10,
    paddingVertical: 8,
  },
  sectionHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    paddingHorizontal: 14,
    marginBottom: 4,
  },
  sectionTitle: { fontSize: 17, fontWeight: '700' },
  sectionMore: { fontSize: 13 },
  horizontalList: { paddingHorizontal: 12, paddingTop: 4 },
});
