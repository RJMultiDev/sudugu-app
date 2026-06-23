import React from 'react';
import {
  View, Text, FlatList, Image, TouchableOpacity, StyleSheet,
} from 'react-native';
import { useTheme } from '../context/ThemeContext';
import { useBookshelf } from '../context/BookshelfContext';
import type { NativeStackNavigationProp } from '@react-navigation/native-stack';
import type { BookshelfItem } from '../types';

interface Props {
  navigation: NativeStackNavigationProp<any>;
}

export function BookshelfScreen({ navigation }: Props) {
  const { colors } = useTheme();
  const { bookshelf } = useBookshelf();

  if (bookshelf.length === 0) {
    return (
      <View style={[styles.emptyContainer, { backgroundColor: colors.background }]}>
        <Text style={[styles.emptyIcon, { color: colors.border }]}>📚</Text>
        <Text style={[styles.emptyText, { color: colors.textSecondary }]}>书架空空如也</Text>
        <TouchableOpacity
          style={[styles.goBtn, { backgroundColor: colors.primary }]}
          onPress={() => navigation.navigate('HomeTab')}>
          <Text style={styles.goBtnText}>去书城逛逛</Text>
        </TouchableOpacity>
      </View>
    );
  }

  return (
    <View style={[styles.container, { backgroundColor: colors.background }]}>
      <Text style={[styles.header, { color: colors.text }]}>我的书架</Text>
      <FlatList
        data={bookshelf}
        keyExtractor={(item) => item.id}
        renderItem={({ item }) => (
          <BookshelfCard
            item={item}
            colors={colors}
            onPress={() => navigation.navigate('NovelDetail', { id: item.id })}
          />
        )}
        contentContainerStyle={styles.list}
      />
    </View>
  );
}

function BookshelfCard({ item, colors, onPress }: {
  item: BookshelfItem; colors: any; onPress: () => void;
}) {
  return (
    <TouchableOpacity style={[styles.card, { backgroundColor: colors.surface }]} onPress={onPress} activeOpacity={0.7}>
      {item.cover ? (
        <Image source={{ uri: item.cover }} style={[styles.cover, { backgroundColor: colors.border }]} resizeMode="cover" />
      ) : (
        <View style={[styles.cover, styles.placeholder]}>
          <Text style={{ fontSize: 20, color: '#999' }}>{item.title.slice(0, 1)}</Text>
        </View>
      )}
      <View style={styles.info}>
        <Text style={[styles.title, { color: colors.text }]} numberOfLines={1}>{item.title}</Text>
        <Text style={[styles.author, { color: colors.textSecondary }]} numberOfLines={1}>{item.author}</Text>
        <View style={styles.tagRow}>
          {item.status && (
            <Text style={[styles.tag, { color: colors.primary, borderColor: colors.primary + '50' }]}>
              {item.status}
            </Text>
          )}
          {item.category && (
            <Text style={[styles.tag, { color: colors.textSecondary, borderColor: colors.border }]}>
              {item.category}
            </Text>
          )}
        </View>
      </View>
    </TouchableOpacity>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1 },
  emptyContainer: { flex: 1, justifyContent: 'center', alignItems: 'center' },
  emptyIcon: { fontSize: 48, marginBottom: 12 },
  emptyText: { fontSize: 15, marginBottom: 16 },
  goBtn: { paddingHorizontal: 24, paddingVertical: 10, borderRadius: 20 },
  goBtnText: { color: '#FFF', fontSize: 14, fontWeight: '600' },
  header: { fontSize: 22, fontWeight: '700', padding: 16, paddingBottom: 8 },
  list: { paddingHorizontal: 12 },
  card: {
    flexDirection: 'row',
    padding: 12,
    marginBottom: 8,
    borderRadius: 8,
  },
  cover: {
    width: 65,
    height: 87,
    borderRadius: 4,
    backgroundColor: '#E8E8E8',
  },
  placeholder: {
    justifyContent: 'center',
    alignItems: 'center',
    backgroundColor: '#DDD',
  },
  info: { flex: 1, marginLeft: 12, justifyContent: 'center' },
  title: { fontSize: 16, fontWeight: '600', marginBottom: 4 },
  author: { fontSize: 13, marginBottom: 6 },
  tagRow: { flexDirection: 'row', gap: 6 },
  tag: {
    fontSize: 11,
    borderWidth: 1,
    borderRadius: 3,
    paddingHorizontal: 5,
    paddingVertical: 1,
    overflow: 'hidden',
  },
});
