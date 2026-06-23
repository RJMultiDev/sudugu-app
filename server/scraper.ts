import axios from 'axios';
import * as cheerio from 'cheerio';
import type { AnyNode } from 'domhandler';

const BASE_URL = 'https://sudugu.org';

// Simple in-memory LRU cache
const cache = new Map<string, { data: any; expiry: number }>();
const CACHE_TTL = 2 * 60 * 1000; // 2 minutes
const CACHE_MAX = 500;
const inflight = new Map<string, Promise<string>>();

function evictExpired() {
  const now = Date.now();
  for (const [key, entry] of cache) {
    if (entry.expiry <= now) cache.delete(key);
  }
}

async function fetchHTML(url: string): Promise<string> {
  const fullUrl = url.startsWith('http') ? url : `${BASE_URL}${url}`;

  // Evict expired entries opportunistically
  if (cache.size > CACHE_MAX * 0.8) evictExpired();

  const cacheKey = fullUrl;
  const cached = cache.get(cacheKey);
  if (cached && cached.expiry > Date.now()) {
    return cached.data;
  }

  // Deduplicate concurrent requests for the same URL
  const pending = inflight.get(cacheKey);
  if (pending) return pending;

  const promise = axios.get(fullUrl, {
    headers: {
      'User-Agent': 'Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36',
    },
    timeout: 10000,
  }).then((response) => {
    // LRU eviction: drop oldest entries when at capacity
    if (cache.size >= CACHE_MAX) {
      const oldest = cache.keys().next().value;
      if (oldest) cache.delete(oldest);
    }
    cache.set(cacheKey, { data: response.data, expiry: Date.now() + CACHE_TTL });
    inflight.delete(cacheKey);
    return response.data;
  }).catch((err) => {
    inflight.delete(cacheKey);
    throw err;
  });

  inflight.set(cacheKey, promise);
  return promise;
}

// ---- Type definitions (mirrored in frontend) ----
export interface Novel {
  id: string;
  title: string;
  author: string;
  cover?: string;
  status?: string;
  category?: string;
  words?: string;
  description?: string;
}

export interface HomeItem extends Novel {
  lastChapter?: { id: string; title: string; time: string };
  chapters?: { id: string; title: string; time: string }[];
  rank?: number;
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
  prevChapter?: { id: string; title: string };
  nextChapter?: { id: string; title: string };
}

export interface Category {
  slug: string;
  name: string;
  count: number;
}

// ---- Homepage ----
export async function scrapeHome() {
  const html = await fetchHTML('/');
  const $ = cheerio.load(html);

  const latestUpdates: HomeItem[] = [];
  const rankings: HomeItem[] = [];
  const completedNovels: Novel[] = [];

  // Latest updates
  $('.container').first().find('.item').each((_, el) => {
    const item = parseNovelItem($(el), $);
    if (item) latestUpdates.push(item);
  });

  // Reading rankings
  $('.container').eq(1).find('.list.top li').each((i, el) => {
    const $el = $(el);
    const title = $el.find('p a').text().trim();
    const href = $el.find('.imga').attr('href') || '';
    const id = href.replace(/\//g, '').split('?')[0];
    const cover = $el.find('img').attr('src') || '';

    if (id && title) {
      rankings.push({ id, title, cover, rank: i + 1, author: '' });
    }
  });

  // Completed novels
  $('.container').eq(2).find('.list.top li').each((_, el) => {
    const $el = $(el);
    const title = $el.find('p a').text().trim();
    const href = $el.find('.imga').attr('href') || '';
    const id = href.replace(/\//g, '');
    const cover = $el.find('img').attr('src') || '';
    if (id && title) {
      completedNovels.push({ id, title, cover, author: '' });
    }
  });

  return { latestUpdates, rankings, completedNovels };
}

// ---- Categories ----
export async function scrapeCategories(): Promise<Category[]> {
  const html = await fetchHTML('/fenlei/');
  const $ = cheerio.load(html);
  const categories: Category[] = [];

  $('.fenlei li a').each((_, el) => {
    const $el = $(el);
    const text = $el.text().trim();
    const href = $el.attr('href') || '';

    const match = text.match(/^(.+)\((\d+)部\)$/);
    if (match) {
      categories.push({
        slug: href.replace(/\//g, ''),
        name: match[1],
        count: parseInt(match[2]),
      });
    }
  });

  return categories;
}

// ---- Category novels ----
export async function scrapeCategory(category: string, page: number = 1) {
  const url = page > 1 ? `/${category}/${page}.html` : `/${category}/`;
  const html = await fetchHTML(url);
  const $ = cheerio.load(html);

  const novels: Novel[] = [];
  $('.item').each((_, el) => {
    const item = parseNovelItem($(el), $, true);
    if (item) novels.push(item);
  });

  // Pagination
  const pageText = $('.page').text().trim();
  const totalPages = parsePagination(pageText);

  return { novels, page, totalPages };
}

// ---- Ranking ----
export async function scrapeRanking(page: number = 1) {
  const url = page > 1 ? `/paihang/${page}.html` : '/paihang/';
  const html = await fetchHTML(url);
  const $ = cheerio.load(html);

  const novels: HomeItem[] = [];
  $('.item').each((i, el) => {
    const item = parseNovelItem($(el), $);
    if (item) {
      item.rank = i + 1 + (page - 1) * 10;
      novels.push(item);
    }
  });

  const pageText = $('.page').text().trim();
  const totalPages = parsePagination(pageText);

  return { novels, page, totalPages };
}

// ---- Completed novels ----
export async function scrapeCompleted(page: number = 1) {
  const url = page > 1 ? `/wanjie/${page}.html` : '/wanjie/';
  const html = await fetchHTML(url);
  const $ = cheerio.load(html);

  const novels: Novel[] = [];
  $('.item').each((_, el) => {
    const item = parseNovelItem($(el), $, true);
    if (item) novels.push(item);
  });

  const pageText = $('.page').text().trim();
  const totalPages = parsePagination(pageText);

  return { novels, page, totalPages };
}

// ---- Latest updates ----
export async function scrapeLatest(page: number = 1) {
  const url = page > 1 ? `/zuixin/${page}.html` : '/zuixin/';
  const html = await fetchHTML(url);
  const $ = cheerio.load(html);

  const novels: HomeItem[] = [];
  $('.item').each((_, el) => {
    const item = parseNovelItem($(el), $);
    if (item) novels.push(item);
  });

  const pageText = $('.page').text().trim();
  const totalPages = parsePagination(pageText);

  return { novels, page, totalPages };
}

// ---- Search ----
export async function scrapeSearch(keyword: string) {
  const encoded = encodeURIComponent(keyword);
  const url = `/i/sor.aspx?key=${encoded}`;
  const html = await fetchHTML(url);
  const $ = cheerio.load(html);

  const novels: Novel[] = [];
  $('.item').each((_, el) => {
    const item = parseNovelItem($(el), $, true);
    if (item) novels.push(item);
  });

  return { novels, keyword };
}

// ---- Novel detail ----
export async function scrapeNovelDetail(bookId: string) {
  const html = await fetchHTML(`/${bookId}/`);
  const $ = cheerio.load(html);

  const title = $('.itemtxt h1 a, .itemtxt h3 a').first().text().trim();
  const cover = $('.item img').first().attr('src') || '';
  const status = $('.itemtxt p span').first().text().trim();
  const category = $('.itemtxt p span').eq(1).text().trim();
  const author = $('.itemtxt p a[href*="tag"]').text().trim().replace('作者：', '');
  const words = $('.itemtxt h1 i, .itemtxt h3 i').text().trim();
  const description = $('.des.bb').first().text().trim();

  // Chapters from #list (only chapter links ending in .html)
  const chapters: Chapter[] = [];
  $('#list a').each((_, el) => {
    const $el = $(el);
    const href = $el.attr('href') || '';
    const chapterId = href.match(/\/(\d+)\.html/)?.[1] || '';
    const chapterTitle = $el.text().trim();
    if (chapterId && chapterTitle) {
      chapters.push({ id: chapterId, title: chapterTitle, bookId });
    }
  });

  // TXT downloads from the txt page
  let txtLinks: { url: string; label: string }[] = [];
  try {
    const txtHtml = await fetchHTML(`/${bookId}/txt.html`);
    const $txt = cheerio.load(txtHtml);
    $txt('#list a').each((_, el) => {
      const href = $txt(el).attr('href') || '';
      const label = $txt(el).text().trim();
      if (href && label) {
        txtLinks.push({ url: href.startsWith('http') ? href : `${BASE_URL}${href}`, label });
      }
    });
  } catch {}

  return {
    id: bookId,
    title,
    cover,
    status,
    category,
    author,
    words,
    description,
    chapters,
    txtLinks,
  };
}

// ---- Chapter content ----
export async function scrapeChapter(bookId: string, chapterId: string): Promise<ChapterContent> {
  const html = await fetchHTML(`/${bookId}/${chapterId}.html`);
  const $ = cheerio.load(html);

  const bookTitle = $('.submenu h1 a').text().trim();
  const chapterTitle = $('.submenu h1').text().trim().replace(bookTitle + ' > ', '');

  // Gather content paragraphs
  const paragraphs: string[] = [];
  $('.con p').each((_, el) => {
    const text = $(el).text().trim();
    if (text) paragraphs.push(text);
  });

  const content = paragraphs.join('\n\n');

  // Prev/Next
  const prevHref = $('.prenext span').first().find('a').attr('href') || '';
  const nextHref = $('.prenext span').last().find('a').attr('href') || '';

  const prevChapter = prevHref ? {
    id: prevHref.match(/\/(\d+)\.html/)?.[1] || '',
    title: $('.prenext span').first().find('a').text().trim(),
  } : undefined;

  const nextChapter = nextHref ? {
    id: nextHref.match(/\/(\d+)\.html/)?.[1] || '',
    title: $('.prenext span').last().find('a').text().trim(),
  } : undefined;

  return {
    bookId,
    bookTitle,
    chapterId,
    chapterTitle,
    content,
    prevChapter,
    nextChapter,
  };
}

// ---- Author novels ----
export async function scrapeAuthorNovels(author: string) {
  const encoded = encodeURIComponent(author);
  const html = await fetchHTML(`/zuozhe/?tag=${encoded}`);
  const $ = cheerio.load(html);

  const novels: Novel[] = [];
  $('.item').each((_, el) => {
    const item = parseNovelItem($(el), $, true);
    if (item) novels.push(item);
  });

  return { novels, author };
}

// ---- Helper: Parse novel item from list ----
function parseNovelItem($el: cheerio.Cheerio<AnyNode>, $: cheerio.CheerioAPI, includeChapters: boolean = false): HomeItem | null {
  const $link = $el.find('.itemtxt h1 a, .itemtxt h3 a').first();
  const title = $link.text().trim();
  const href = $link.attr('href') || '';
  const id = href.replace(/\//g, '');
  const cover = $el.find('img').attr('src') || '';

  if (!id || !title) return null;

  // Author is in one of the <p> tags with "作者：" text
  let author = '';
  const pEls = $el.find('.itemtxt p').toArray();
  for (const pEl of pEls) {
    const pText = $(pEl).text().trim();
    const m = pText.match(/作者[：:]\s*(\S+)/);
    if (m) { author = m[1]; break; }
  }
  const status = $el.find('.itemtxt p span').first().text().trim();
  const category = $el.find('.itemtxt p span').eq(1).text().trim();
  const words = $el.find('.itemtxt h1 i, .itemtxt h3 i').text().trim();

  const result: HomeItem = {
    id, title, author,
    cover: cover ? (cover.startsWith('http') ? cover : `https:${cover}`) : '',
    status, category, words,
  };

  if (includeChapters) {
    const chapters: { id: string; title: string; time: string }[] = [];
    $el.find('.itemtxt ul li').each((_: number, li: AnyNode) => {
      const $li = $(li);
      const time = $li.find('i').text().trim();
      const $a = $li.find('a');
      const chHref = $a.attr('href') || '';
      const chId = chHref.match(/\/(\d+)\.html/)?.[1] || '';
      const chTitle = $a.text().trim();
      if (chId && chTitle) {
        chapters.push({ id: chId, title: chTitle, time });
      }
    });
    if (chapters.length > 0) {
      result.lastChapter = chapters[0];
      result.chapters = chapters;
    }
  }

  return result;
}

// ---- Helper: Parse pagination ----
function parsePagination(text: string): number {
  const match = text.match(/(\d+)/g);
  if (match && match.length >= 2) {
    return parseInt(match[match.length - 1]);
  }
  // Try "共N页" or "/N" patterns from page links
  return 1;
}
