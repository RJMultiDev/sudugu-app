import React from 'react';
import {
  View, Text, ScrollView, TouchableOpacity, StyleSheet, Alert,
} from 'react-native';
import { useTheme } from '../context/ThemeContext';
import { useBookshelf } from '../context/BookshelfContext';
import type { NativeStackNavigationProp } from '@react-navigation/native-stack';

interface Props {
  navigation: NativeStackNavigationProp<any>;
}

export function ProfileScreen({ navigation }: Props) {
  const { colors, theme, toggleTheme } = useTheme();
  const { bookshelf } = useBookshelf();

  return (
    <ScrollView style={[styles.container, { backgroundColor: colors.background }]}>
      <View style={[styles.header, { backgroundColor: colors.primary }]}>
        <View style={styles.avatar}>
          <Text style={styles.avatarText}>👤</Text>
        </View>
        <Text style={styles.headerTitle}>速读谷</Text>
        <Text style={styles.headerSub}>谷内无错 可以速读</Text>
      </View>

      <View style={[styles.statsRow, { backgroundColor: colors.surface }]}>
        <View style={styles.statItem}>
          <Text style={[styles.statNum, { color: colors.primary }]}>{bookshelf.length}</Text>
          <Text style={[styles.statLabel, { color: colors.textSecondary }]}>书架</Text>
        </View>
      </View>

      <View style={[styles.menuGroup, { backgroundColor: colors.surface }]}>
        <MenuItem
          label="夜间模式"
          value={theme === 'dark' ? '已开启' : '已关闭'}
          colors={colors}
          onPress={toggleTheme}
        />
        <MenuItem
          label="阅读记录"
          colors={colors}
          onPress={() => navigation.navigate('ReadHistory')}
        />
        <MenuItem
          label="排行榜"
          colors={colors}
          onPress={() => navigation.navigate('Ranking')}
        />
        <MenuItem
          label="关于"
          colors={colors}
          onPress={() => Alert.alert('关于', '速读谷 v1.0.0\n谷内无错 可以速读\n数据来源: sudugu.org')}
          last
        />
      </View>
    </ScrollView>
  );
}

function MenuItem({ label, value, colors, onPress, last }: {
  label: string; value?: string; colors: any; onPress: () => void; last?: boolean;
}) {
  return (
    <TouchableOpacity
      style={[styles.menuItem, !last && { borderBottomColor: colors.border, borderBottomWidth: StyleSheet.hairlineWidth }]}
      onPress={onPress}
      activeOpacity={0.6}>
      <Text style={[styles.menuLabel, { color: colors.text }]}>{label}</Text>
      <View style={styles.menuRight}>
        {value && <Text style={[styles.menuValue, { color: colors.textSecondary }]}>{value}</Text>}
        <Text style={{ color: colors.textSecondary, fontSize: 14 }}>{'>'}</Text>
      </View>
    </TouchableOpacity>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1 },
  header: {
    paddingTop: 60,
    paddingBottom: 30,
    alignItems: 'center',
  },
  avatar: {
    width: 64,
    height: 64,
    borderRadius: 32,
    backgroundColor: 'rgba(255,255,255,0.25)',
    justifyContent: 'center',
    alignItems: 'center',
    marginBottom: 10,
  },
  avatarText: { fontSize: 28 },
  headerTitle: { color: '#FFF', fontSize: 18, fontWeight: '700' },
  headerSub: { color: 'rgba(255,255,255,0.8)', fontSize: 13, marginTop: 4 },
  statsRow: {
    flexDirection: 'row',
    padding: 20,
    marginTop: 12,
    borderRadius: 10,
    marginHorizontal: 12,
  },
  statItem: { flex: 1, alignItems: 'center' },
  statNum: { fontSize: 22, fontWeight: '700' },
  statLabel: { fontSize: 12, marginTop: 4 },
  menuGroup: {
    marginTop: 12,
    marginHorizontal: 12,
    borderRadius: 10,
    overflow: 'hidden',
  },
  menuItem: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    padding: 16,
  },
  menuLabel: { fontSize: 15 },
  menuRight: { flexDirection: 'row', alignItems: 'center', gap: 6 },
  menuValue: { fontSize: 13 },
});
