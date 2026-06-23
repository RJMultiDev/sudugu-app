import React, { useState, useEffect, useCallback } from 'react';
import {
  View, Text, FlatList, TouchableOpacity, StyleSheet, TextInput,
} from 'react-native';
import { useTheme } from '../context/ThemeContext';
import { NovelCard } from '../components/NovelCard';
import { LoadingIndicator } from '../components/LoadingIndicator';
import { api } from '../services/api';
import type { NativeStackNavigationProp } from '@react-navigation/native-stack';
import type { Novel } from '../types';

interface Props {
  navigation: NativeStackNavigationProp<any>;
}

export function SearchScreen({ navigation }: Props) {
  const { colors } = useTheme();
  const [keyword, setKeyword] = useState('');
  const [results, setResults] = useState<Novel[]>([]);
  const [searched, setSearched] = useState('');
  const [loading, setLoading] = useState(false);
  const [hasSearched, setHasSearched] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const doSearch = async () => {
    const trimmed = keyword.trim();
    if (!trimmed) return;
    setLoading(true);
    setSearched(trimmed);
    setHasSearched(true);
    setError(null);
    try {
      const result = await api.search(trimmed);
      setResults(result.novels);
    } catch {
      setResults([]);
      setError('搜索失败，请检查网络');
    } finally {
      setLoading(false);
    }
  };

  return (
    <View style={[styles.container, { backgroundColor: colors.background }]}>
      {/* Search input */}
      <View style={[styles.searchBar, { backgroundColor: colors.surface, borderColor: colors.border }]}>
        <TextInput
          style={[styles.input, { color: colors.text }]}
          value={keyword}
          onChangeText={setKeyword}
          placeholder="搜索小说、作者"
          placeholderTextColor={colors.textSecondary}
          onSubmitEditing={doSearch}
          returnKeyType="search"
          maxLength={30}
          autoFocus
        />
        <TouchableOpacity style={[styles.searchBtn, { backgroundColor: colors.primary }]} onPress={doSearch}>
          <Text style={styles.searchBtnText}>搜索</Text>
        </TouchableOpacity>
      </View>

      {loading ? (
        <LoadingIndicator text="搜索中..." />
      ) : error ? (
        <View style={styles.emptyContainer}>
          <Text style={[styles.emptyText, { color: '#E53935' }]}>{error}</Text>
        </View>
      ) : hasSearched ? (
        results.length > 0 ? (
          <FlatList
            data={results}
            keyExtractor={(item) => item.id}
            renderItem={({ item }) => (
              <NovelCard novel={item} onPress={() => navigation.navigate('NovelDetail', { id: item.id })} />
            )}
            ListHeaderComponent={
              <Text style={[styles.resultCount, { color: colors.textSecondary }]}>
                搜索"{searched}"，找到 {results.length} 本
              </Text>
            }
          />
        ) : (
          <View style={styles.emptyContainer}>
            <Text style={[styles.emptyText, { color: colors.textSecondary }]}>
              未找到"{searched}"相关小说
            </Text>
          </View>
        )
      ) : null}
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1 },
  searchBar: {
    flexDirection: 'row',
    margin: 12,
    borderRadius: 8,
    borderWidth: 1,
    overflow: 'hidden',
  },
  input: { flex: 1, padding: 12, fontSize: 15 },
  searchBtn: {
    paddingHorizontal: 20,
    justifyContent: 'center',
  },
  searchBtnText: { color: '#FFF', fontSize: 14, fontWeight: '600' },
  resultCount: { paddingHorizontal: 14, paddingVertical: 8, fontSize: 13 },
  emptyContainer: { flex: 1, justifyContent: 'center', alignItems: 'center' },
  emptyText: { fontSize: 15 },
});
