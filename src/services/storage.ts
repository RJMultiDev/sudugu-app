import AsyncStorage from '@react-native-async-storage/async-storage';
import type { BookshelfItem, ReadProgress } from '../types';

const KEYS = {
  BOOKSHELF: '@sudugu/bookshelf',
  READ_HISTORY: '@sudugu/read_history',
  THEME: '@sudugu/theme',
  FONT_SIZE: '@sudugu/font_size',
  READING_PROGRESS: '@sudugu/reading_progress',
};

// ---- Bookshelf ----
export async function getBookshelf(): Promise<BookshelfItem[]> {
  const data = await AsyncStorage.getItem(KEYS.BOOKSHELF);
  try {
    return data ? JSON.parse(data) : [];
  } catch {
    return [];
  }
}

export async function addToBookshelf(book: BookshelfItem): Promise<void> {
  const shelf = await getBookshelf();
  if (!shelf.find((b) => b.id === book.id)) {
    shelf.unshift({ ...book, addedAt: Date.now() });
    await AsyncStorage.setItem(KEYS.BOOKSHELF, JSON.stringify(shelf));
  }
}

export async function removeFromBookshelf(bookId: string): Promise<void> {
  const shelf = await getBookshelf();
  await AsyncStorage.setItem(
    KEYS.BOOKSHELF,
    JSON.stringify(shelf.filter((b) => b.id !== bookId))
  );
}

export async function isInBookshelf(bookId: string): Promise<boolean> {
  const shelf = await getBookshelf();
  return shelf.some((b) => b.id === bookId);
}

// ---- Read progress ----
export async function saveReadProgress(
  bookId: string,
  chapterId: string,
  chapterTitle: string,
  bookTitle: string,
  scrollY?: number
): Promise<void> {
  // 读取旧 progress 保留 scrollY
  const existing = await getReadProgress(bookId);
  const progress: ReadProgress = {
    bookId,
    chapterId,
    chapterTitle,
    bookTitle,
    timestamp: Date.now(),
    scrollY: scrollY ?? existing?.scrollY ?? 0,
  };
  await AsyncStorage.setItem(
    `${KEYS.READING_PROGRESS}_${bookId}`,
    JSON.stringify(progress)
  );
  // Also update history
  const history = await getReadHistory();
  const filtered = history.filter((h) => h.bookId !== bookId);
  filtered.unshift(progress);
  await AsyncStorage.setItem(KEYS.READ_HISTORY, JSON.stringify(filtered.slice(0, 50)));
}

export async function getReadProgress(bookId: string): Promise<ReadProgress | null> {
  const data = await AsyncStorage.getItem(`${KEYS.READING_PROGRESS}_${bookId}`);
  try {
    return data ? JSON.parse(data) : null;
  } catch {
    return null;
  }
}

export async function getReadHistory(): Promise<ReadProgress[]> {
  const data = await AsyncStorage.getItem(KEYS.READ_HISTORY);
  try {
    return data ? JSON.parse(data) : [];
  } catch {
    return [];
  }
}

// ---- Theme ----
export async function getThemeMode(): Promise<'light' | 'dark'> {
  const data = await AsyncStorage.getItem(KEYS.THEME);
  return (data as 'light' | 'dark') || 'light';
}

export async function setThemeMode(mode: 'light' | 'dark'): Promise<void> {
  await AsyncStorage.setItem(KEYS.THEME, mode);
}

// ---- Font size ----
export async function getFontSize(): Promise<'small' | 'medium' | 'large' | 'xlarge'> {
  const data = await AsyncStorage.getItem(KEYS.FONT_SIZE);
  return (data as any) || 'medium';
}

export async function setFontSize(size: 'small' | 'medium' | 'large' | 'xlarge'): Promise<void> {
  await AsyncStorage.setItem(KEYS.FONT_SIZE, size);
}
