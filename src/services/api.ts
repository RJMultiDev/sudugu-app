import {
  scrapeHome,
  scrapeCategories,
  scrapeCategory,
  scrapeRanking,
  scrapeCompleted,
  scrapeLatest,
  scrapeSearch,
  scrapeNovelDetail,
  scrapeChapter,
  scrapeAuthorNovels,
} from './scraper';
import type {
  HomeData,
  Category,
  CategoryDetail,
  PaginatedResponse,
  HomeItem,
  Novel,
  ChapterContent,
} from '../types';

export const api = {
  getHome(): Promise<HomeData> {
    return scrapeHome();
  },

  getCategories(): Promise<Category[]> {
    return scrapeCategories();
  },

  getCategory(slug: string, page: number = 1): Promise<CategoryDetail> {
    return scrapeCategory(slug, page) as Promise<CategoryDetail>;
  },

  getRanking(page: number = 1): Promise<PaginatedResponse<HomeItem>> {
    return scrapeRanking(page);
  },

  getCompleted(page: number = 1): Promise<CategoryDetail> {
    return scrapeCompleted(page) as Promise<CategoryDetail>;
  },

  getLatest(page: number = 1): Promise<PaginatedResponse<HomeItem>> {
    return scrapeLatest(page);
  },

  search(keyword: string): Promise<{ novels: Novel[]; keyword: string }> {
    return scrapeSearch(keyword);
  },

  getNovelDetail(id: string): Promise<Novel & { chapters: { id: string; title: string }[] }> {
    return scrapeNovelDetail(id) as Promise<Novel & { chapters: { id: string; title: string }[] }>;
  },

  getChapter(bookId: string, chapterId: string): Promise<ChapterContent> {
    return scrapeChapter(bookId, chapterId);
  },

  getAuthorNovels(tag: string): Promise<{ novels: Novel[]; author: string }> {
    return scrapeAuthorNovels(tag);
  },
};
