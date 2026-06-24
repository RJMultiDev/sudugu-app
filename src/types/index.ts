// ---- API response types ----
export interface Novel {
  id: string;
  title: string;
  author: string;
  cover?: string;
  status?: string;
  category?: string;
  words?: string;
  description?: string;
  txtLinks?: { url: string; label: string }[];
  chapters?: { id: string; title: string }[];
}

export interface HomeItem extends Novel {
  lastChapter?: ChapterRef;
  chapters?: ChapterRef[];
  rank?: number;
}

export interface ChapterRef {
  id: string;
  title: string;
  time?: string;
}

export interface Chapter {
  id: string;
  title: string;
  bookId: string;
}

export interface ChapterContent {
  bookId: string;
  bookTitle: string;
  chapterId: string;
  chapterTitle: string;
  content: string;
  prevChapter?: ChapterRef;
  nextChapter?: ChapterRef;
}

export interface Category {
  slug: string;
  name: string;
  count: number;
}

export interface CategoryDetail {
  novels: Novel[];
  page: number;
  totalPages: number;
}

export interface PaginatedResponse<T> {
  novels: T[];
  page: number;
  totalPages: number;
}

export interface HomeData {
  latestUpdates: HomeItem[];
  rankings: HomeItem[];
  completedNovels: Novel[];
}

// ---- Local storage types ----
export interface BookshelfItem {
  id: string;
  title: string;
  author: string;
  cover?: string;
  status?: string;
  category?: string;
  addedAt?: number;
}

export interface ReadProgress {
  bookId: string;
  chapterId: string;
  chapterTitle: string;
  bookTitle: string;
  timestamp: number;
  scrollY?: number;
}

// ---- Theme ----
export type ThemeMode = 'light' | 'dark';
export type ReaderFontSize = 'small' | 'medium' | 'large' | 'xlarge';
