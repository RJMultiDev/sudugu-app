import React from 'react';
import { View, Text, Image, TouchableOpacity, StyleSheet } from 'react-native';
import { useTheme } from '../context/ThemeContext';
import type { Novel } from '../types';

interface Props {
  novel: Novel;
  onPress: () => void;
  variant?: 'horizontal' | 'vertical';
  rank?: number;
}

export function NovelCard({ novel, onPress, variant = 'horizontal', rank }: Props) {
  const { colors } = useTheme();

  if (variant === 'vertical') {
    return (
      <TouchableOpacity style={styles.verticalCard} onPress={onPress} activeOpacity={0.7}>
        {novel.cover ? (
          <Image source={{ uri: novel.cover }} style={styles.verticalCover} resizeMode="cover" />
        ) : (
          <View style={[styles.verticalCover, styles.placeholder]}>
            <Text style={styles.placeholderText}>{novel.title.slice(0, 1)}</Text>
          </View>
        )}
        <Text style={[styles.verticalTitle, { color: colors.text }]} numberOfLines={2}>
          {novel.title}
        </Text>
        {novel.author && (
          <Text style={[styles.verticalAuthor, { color: colors.textSecondary }]} numberOfLines={1}>
            {novel.author}
          </Text>
        )}
      </TouchableOpacity>
    );
  }

  return (
    <TouchableOpacity style={[styles.horizontalCard, { backgroundColor: colors.card }]} onPress={onPress} activeOpacity={0.7}>
      {novel.cover ? (
        <Image source={{ uri: novel.cover }} style={styles.horizontalCover} resizeMode="cover" />
      ) : (
        <View style={[styles.horizontalCover, styles.placeholder]}>
          <Text style={styles.placeholderText}>{novel.title.slice(0, 1)}</Text>
        </View>
      )}
      <View style={styles.horizontalInfo}>
        <View style={styles.titleRow}>
          {rank !== undefined && (
            <View style={[
              styles.rankBadge,
              rank <= 3 ? styles.rankBadgeTop : styles.rankBadgeNormal,
            ]}>
              <Text style={styles.rankText}>{String(rank).padStart(2, '0')}</Text>
            </View>
          )}
          <Text style={[styles.horizontalTitle, { color: colors.text }]} numberOfLines={1}>
            {novel.title}
          </Text>
        </View>
        <Text style={[styles.horizontalAuthor, { color: colors.textSecondary }]} numberOfLines={1}>
          {novel.author}
        </Text>
        <View style={styles.tagRow}>
          {novel.status && (
            <Text style={[styles.tag, { color: colors.primary, borderColor: colors.primary }]}>
              {novel.status}
            </Text>
          )}
          {novel.category && (
            <Text style={[styles.tag, { color: colors.textSecondary, borderColor: colors.border }]}>
              {novel.category}
            </Text>
          )}
        </View>
      </View>
    </TouchableOpacity>
  );
}

const styles = StyleSheet.create({
  horizontalCard: {
    flexDirection: 'row',
    padding: 12,
    borderBottomWidth: StyleSheet.hairlineWidth,
    borderBottomColor: '#E0E0E0',
  },
  horizontalCover: {
    width: 70,
    height: 93,
    borderRadius: 4,
    backgroundColor: '#E8E8E8',
  },
  placeholder: {
    justifyContent: 'center',
    alignItems: 'center',
    backgroundColor: '#DDD',
  },
  placeholderText: { fontSize: 20, color: '#999', fontWeight: '600' },
  horizontalInfo: {
    flex: 1,
    marginLeft: 12,
    justifyContent: 'center',
  },
  titleRow: { flexDirection: 'row', alignItems: 'center', marginBottom: 4 },
  horizontalTitle: { fontSize: 16, fontWeight: '600', flex: 1 },
  horizontalAuthor: { fontSize: 13, marginBottom: 6 },
  tagRow: { flexDirection: 'row', gap: 6 },
  tag: {
    fontSize: 11,
    borderWidth: 1,
    borderRadius: 3,
    paddingHorizontal: 5,
    paddingVertical: 1,
    overflow: 'hidden',
  },
  rankBadge: {
    width: 24,
    height: 24,
    borderRadius: 4,
    justifyContent: 'center',
    alignItems: 'center',
    marginRight: 8,
  },
  rankBadgeTop: { backgroundColor: '#FF6B6B' },
  rankBadgeNormal: { backgroundColor: '#999' },
  rankText: { color: '#FFF', fontSize: 11, fontWeight: '700' },
  verticalCard: { width: 120, marginRight: 12 },
  verticalCover: {
    width: 120,
    height: 160,
    borderRadius: 6,
    backgroundColor: '#E8E8E8',
  },
  verticalTitle: { fontSize: 13, fontWeight: '500', marginTop: 6 },
  verticalAuthor: { fontSize: 12, marginTop: 2 },
});
