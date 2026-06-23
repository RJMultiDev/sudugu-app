import { API_BASE_URL } from '../utils/constants';
import type {
  HomeData,
  Category,
  CategoryDetail,
  PaginatedResponse,
  HomeItem,
  Novel,
  ChapterContent,
} from '../types';

async function fetchJSON<T>(url: string): Promise<T> {
  const response = await fetch(url);
  if (!response.ok) {
    throw new Error(`HTTP ${response.status}: ${response.statusText}`);
  }
  return response.json();
}

export const api = {
  getHome(): Promise<HomeData> {
    return fetchJSON<HomeData>(`${API_BASE_URL}/home`);
  },

  getCategories(): Promise<Category[]> {
    return fetchJSON<Category[]>(`${API_BASE_URL}/categories`);
  },

  getCategory(slug: string, page: number = 1): Promise<CategoryDetail> {
    return fetchJSON<CategoryDetail>(`${API_BASE_URL}/category/${slug}?page=${page}`);
  },

  getRanking(page: number = 1): Promise<PaginatedResponse<HomeItem>> {
    return fetchJSON<PaginatedResponse<HomeItem>>(`${API_BASE_URL}/ranking?page=${page}`);
  },

  getCompleted(page: number = 1): Promise<CategoryDetail> {
    return fetchJSON<CategoryDetail>(`${API_BASE_URL}/completed?page=${page}`);
  },

  getLatest(page: number = 1): Promise<PaginatedResponse<HomeItem>> {
    return fetchJSON<PaginatedResponse<HomeItem>>(`${API_BASE_URL}/latest?page=${page}`);
  },

  search(keyword: string): Promise<{ novels: Novel[]; keyword: string }> {
    return fetchJSON<{ novels: Novel[]; keyword: string }>(
      `${API_BASE_URL}/search?keyword=${encodeURIComponent(keyword)}`
    );
  },

  getNovelDetail(id: string): Promise<Novel & { chapters: { id: string; title: string }[] }> {
    return fetchJSON(`${API_BASE_URL}/novel/${id}`);
  },

  getChapter(bookId: string, chapterId: string): Promise<ChapterContent> {
    return fetchJSON<ChapterContent>(`${API_BASE_URL}/chapter/${bookId}/${chapterId}`);
  },

  getAuthorNovels(tag: string): Promise<{ novels: Novel[]; author: string }> {
    return fetchJSON<{ novels: Novel[]; author: string }>(
      `${API_BASE_URL}/author?tag=${encodeURIComponent(tag)}`
    );
  },
};
