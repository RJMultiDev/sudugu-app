import { Router, Request, Response } from 'express';
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

const router = Router();

// Helper: wrap async handler
const asyncHandler = (fn: (req: Request, res: Response) => Promise<any>) =>
  (req: Request, res: Response) => {
    fn(req, res).catch((err: any) => {
      console.error('API Error:', err.message);
      res.status(500).json({ error: err.message || 'Internal server error' });
    });
  };

router.get('/home', asyncHandler(async (_, res) => {
  const data = await scrapeHome();
  res.json(data);
}));

router.get('/categories', asyncHandler(async (_, res) => {
  const data = await scrapeCategories();
  res.json(data);
}));

router.get('/category/:slug', asyncHandler(async (req, res) => {
  const page = parseInt(String(req.query.page || '1')) || 1;
  const slug = String(req.params.slug || '');
  const data = await scrapeCategory(slug, page);
  res.json(data);
}));

router.get('/ranking', asyncHandler(async (req, res) => {
  const page = parseInt(String(req.query.page || '1')) || 1;
  const data = await scrapeRanking(page);
  res.json(data);
}));

router.get('/completed', asyncHandler(async (req, res) => {
  const page = parseInt(String(req.query.page || '1')) || 1;
  const data = await scrapeCompleted(page);
  res.json(data);
}));

router.get('/latest', asyncHandler(async (req, res) => {
  const page = parseInt(String(req.query.page || '1')) || 1;
  const data = await scrapeLatest(page);
  res.json(data);
}));

router.get('/search', asyncHandler(async (req, res) => {
  const keyword = String(req.query.keyword || '').trim();
  if (!keyword) {
    return res.json({ novels: [], keyword: '' });
  }
  const data = await scrapeSearch(keyword);
  res.json(data);
}));

router.get('/novel/:id', asyncHandler(async (req, res) => {
  const data = await scrapeNovelDetail(String(req.params.id));
  res.json(data);
}));

router.get('/chapter/:bookId/:chapterId', asyncHandler(async (req, res) => {
  const bookId = String(req.params.bookId);
  const chapterId = String(req.params.chapterId);
  const data = await scrapeChapter(bookId, chapterId);
  res.json(data);
}));

router.get('/author', asyncHandler(async (req, res) => {
  const tag = String(req.query.tag || '').trim();
  if (!tag) {
    return res.json({ novels: [], author: '' });
  }
  const data = await scrapeAuthorNovels(tag);
  res.json(data);
}));

export default router;
