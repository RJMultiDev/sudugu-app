import React, { useState, useEffect, useCallback } from 'react';
import {
  View, Text, ScrollView, TouchableOpacity, StyleSheet, RefreshControl,
} from 'react-native';
import { useTheme } from '../context/ThemeContext';
import { LoadingIndicator, ErrorView } from '../components/LoadingIndicator';
import { api } from '../services/api';
import { CATEGORIES } from '../utils/constants';
import type { NativeStackNavigationProp } from '@react-navigation/native-stack';
import type { Category } from '../types';

interface Props {
  navigation: NativeStackNavigationProp<any>;
}

export function CategoryScreen({ navigation }: Props) {
  const { colors } = useTheme();

  return (
    <ScrollView style={[styles.container, { backgroundColor: colors.background }]}>
      <Text style={[styles.header, { color: colors.text }]}>小说分类</Text>
      <View style={styles.grid}>
        {CATEGORIES.map((cat) => (
          <TouchableOpacity
            key={cat.slug}
            style={[styles.item, { backgroundColor: colors.surface }]}
            onPress={() => navigation.navigate('CategoryDetail', { slug: cat.slug, name: cat.name })}
            activeOpacity={0.7}>
            <Text style={styles.itemIcon}>{cat.icon}</Text>
            <Text style={[styles.itemName, { color: colors.text }]}>{cat.name}</Text>
          </TouchableOpacity>
        ))}
      </View>
      <View style={{ height: 30 }} />
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1 },
  header: { fontSize: 22, fontWeight: '700', padding: 16, paddingBottom: 8 },
  grid: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    paddingHorizontal: 10,
  },
  item: {
    width: '30%',
    margin: '1.5%',
    paddingVertical: 20,
    borderRadius: 8,
    alignItems: 'center',
  },
  itemIcon: { fontSize: 28, marginBottom: 6 },
  itemName: { fontSize: 14, fontWeight: '500' },
});
