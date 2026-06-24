import { Platform } from 'react-native';
import type {
  Novel,
  HomeItem,
  Chapter,
  ChapterContent,
  Category,
  ChapterRef,
} from '../types';

const BASE_URL = 'https://sudugu.org';

// ---- In-memory cache ----
const cache = new Map<string, { data: string; expiry: number }>();
const CACHE_TTL = 2 * 60 * 1000;
const CACHE_MAX = 500;
const inflight = new Map<string, Promise<string>>();

function evictExpired() {
  const now = Date.now();
  for (const [key, entry] of cache) {
    if (entry.expiry <= now) cache.delete(key);
  }
}

async function fetchHTML(url: string): Promise<string> {
  const directUrl = url.startsWith('http') ? url : `${BASE_URL}${url}`;
  const isWeb = Platform.OS === 'web';
  const fetchUrl = isWeb ? `https://corsproxy.io/?${encodeURIComponent(directUrl)}` : directUrl;

  if (cache.size > CACHE_MAX * 0.8) evictExpired();

  const cacheKey = directUrl;
  const cached = cache.get(cacheKey);
  if (cached && cached.expiry > Date.now()) {
    return cached.data;
  }

  const pending = inflight.get(cacheKey);
  if (pending) return pending;

  const promise = fetch(fetchUrl, isWeb ? {} : {
    headers: {
      'User-Agent': 'Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36',
    },
  })
    .then(async (response) => {
      console.log('[fetchHTML]', { url: directUrl, status: response.status, isWeb });
      if (!response.ok) {
        throw new Error(`HTTP ${response.status}: ${response.statusText}`);
      }
      const text = await response.text();

      if (cache.size >= CACHE_MAX) {
        const oldest = cache.keys().next().value;
        if (oldest) cache.delete(oldest);
      }
      cache.set(cacheKey, {
        data: text,
        expiry: Date.now() + CACHE_TTL,
      });
      inflight.delete(cacheKey);
      return text;
    })
    .catch((err) => {
      inflight.delete(cacheKey);
      throw err;
    });

  inflight.set(cacheKey, promise);
  return promise;
}

// ---- Helpers ----

function extractAttribute(html: string, attr: string): string {
  const match = html.match(new RegExp(`${attr}="([^"]*)"`));
  return match ? match[1] : '';
}

function extractText(html: string): string {
  return html.replace(/<[^>]*>/g, '').trim();
}

function normalizeCover(src: string): string {
  if (!src) return '';
  return src.startsWith('http') ? src : src.startsWith('//') ? `https:${src}` : `${BASE_URL}${src}`;
}

function normalizeUrl(href: string): string {
  if (!href) return '';
  return href.startsWith('http') ? href : `${BASE_URL}${href}`;
}

/** Extract all item blocks from HTML */
function extractItemBlocks(html: string): string[] {
  const blocks: string[] = [];
  const regex = /<div class="item">([\s\S]*?)<\/div>\s*<\/div>/g;
  let m;
  while ((m = regex.exec(html)) !== null) {
    blocks.push(m[0]);
  }
  return blocks;
}

/** Parse a novel item card (used by most list pages) */
function parseNovelItemFromHtml(
  block: string,
  includeChapters: boolean = false
): HomeItem | null {
  // Title + link from .itemtxt heading
  const titleMatch = block.match(
    /<div class="itemtxt">\s*<h[13][^>]*>[\s\S]*?<a href="([^"]*)"[^>]*>([^<]+)<\/a>/
  );
  if (!titleMatch) return null;

  const href = titleMatch[1];
  const title = titleMatch[2].trim();
  const id = href.replace(/\//g, '');
  if (!id || !title) return null;

  const cover = normalizeCover(extractAttribute(block, 'src'));

  // Author from <p><a href="...tag=...">作者：XXX</a></p>
  let author = '';
  const authorMatch = block.match(/作者[：:]\s*([^<]+)/);
  if (authorMatch) author = authorMatch[1].trim();

  // Status and category from <span> inside .itemtxt <p> tags
  const spans = [...block.matchAll(/<span>([^<]*)<\/span>/g)];
  const status = spans.length > 0 ? spans[0][1].trim() : '';
  const category = spans.length > 1 ? spans[1][1].trim() : '';

  // Word count from <i> inside heading
  const wordsMatch = block.match(/<h[13][^>]*>\s*<i>([^<]*)<\/i>/);
  const words = wordsMatch ? wordsMatch[1].trim() : '';

  const result: HomeItem = {
    id,
    title,
    author,
    cover,
    status,
    category,
    words,
  };

  // Last chapter info from <ul><li><i>time</i><a href="...">title</a></li></ul>
  const liMatch = block.match(/<li>\s*<i>([^<]*)<\/i>\s*<a href="([^"]*)"[^>]*>([^<]+)<\/a>/);
  if (liMatch) {
    const chapterId = liMatch[2].match(/\/(\d+)\.html/)?.[1] || '';
    const lastChapter: ChapterRef = {
      id: chapterId,
      title: liMatch[3].trim(),
      time: liMatch[1].trim(),
    };
    result.lastChapter = lastChapter;
  }

  if (includeChapters) {
    const chapters: ChapterRef[] = [];
    const liRegex = /<li>\s*<i>([^<]*)<\/i>\s*<a href="([^"]*)"[^>]*>([^<]+)<\/a>/g;
    let liM;
    while ((liM = liRegex.exec(block)) !== null) {
      const chId = liM[2].match(/\/(\d+)\.html/)?.[1] || '';
      if (chId) {
        chapters.push({
          id: chId,
          title: liM[3].trim(),
          time: liM[1].trim(),
        });
      }
    }
    if (chapters.length > 0) {
      result.chapters = chapters;
      if (!result.lastChapter) result.lastChapter = chapters[0];
    }
  }

  return result;
}

/** Parse ranking list item (.list.top li) */
function parseRankingItem(liHtml: string, rank: number): HomeItem | null {
  const titleMatch = liHtml.match(/<p[^>]*>\s*<a[^>]*>([^<]+)<\/a>/);
  const hrefMatch = liHtml.match(/class="imga"[^>]*href="([^"]*)"/);
  const srcMatch = liHtml.match(/<img[^>]*src="([^"]*)"/);

  if (!titleMatch || !hrefMatch) return null;

  const title = titleMatch[1].trim();
  const id = hrefMatch[1].replace(/\//g, '').split('?')[0];
  const cover = normalizeCover(srcMatch ? srcMatch[1] : '');

  if (!id || !title) return null;

  return { id, title, cover, rank, author: '' };
}

/** Parse completed list item (.list.top li) */
function parseCompletedItem(liHtml: string): Novel | null {
  const titleMatch = liHtml.match(/<p[^>]*>\s*<a[^>]*>([^<]+)<\/a>/);
  const hrefMatch = liHtml.match(/class="imga"[^>]*href="([^"]*)"/);
  const srcMatch = liHtml.match(/<img[^>]*src="([^"]*)"/);

  if (!titleMatch || !hrefMatch) return null;

  const title = titleMatch[1].trim();
  const id = hrefMatch[1].replace(/\//g, '');
  const cover = normalizeCover(srcMatch ? srcMatch[1] : '');

  if (!id || !title) return null;

  return { id, title, cover, author: '' };
}

/** Extract list items from HTML */
function extractListItems(html: string): string[] {
  const items: string[] = [];
  const regex = /<li>([\s\S]*?)<\/li>/g;
  let m;
  while ((m = regex.exec(html)) !== null) {
    items.push(m[0]);
  }
  return items;
}

/** Parse pagination from .page element */
function parsePagination(html: string): number {
  const pageMatch = html.match(/<div class="page">([\s\S]*?)<\/div>/);
  if (!pageMatch) return 1;
  const nums = pageMatch[1].match(/(\d+)/g);
  if (nums && nums.length >= 2) {
    return parseInt(nums[nums.length - 1]);
  }
  return 1;
}

// ---- Homepage ----

export async function scrapeHome() {
  const html = await fetchHTML('/');
  const sections = html.split(/<div class="container">/);

  // Latest updates: first container
  const latestUpdates: HomeItem[] = [];
  if (sections.length > 1) {
    const blocks = extractItemBlocks(sections[1]);
    for (const block of blocks) {
      const item = parseNovelItemFromHtml(block);
      if (item) latestUpdates.push(item);
    }
  }

  // Rankings: second container, .list.top li
  const rankings: HomeItem[] = [];
  if (sections.length > 2) {
    const topListMatch = sections[2].match(/<ul class="list top">([\s\S]*?)<\/ul>/);
    if (topListMatch) {
      const items = extractListItems(topListMatch[1]);
      items.forEach((li, i) => {
        const item = parseRankingItem(li, i + 1);
        if (item) rankings.push(item);
      });
    }
  }

  // Completed novels: third container, .list.top li
  const completedNovels: Novel[] = [];
  if (sections.length > 3) {
    const topListMatch = sections[3].match(/<ul class="list top">([\s\S]*?)<\/ul>/);
    if (topListMatch) {
      const items = extractListItems(topListMatch[1]);
      for (const li of items) {
        const item = parseCompletedItem(li);
        if (item) completedNovels.push(item);
      }
    }
  }

  return { latestUpdates, rankings, completedNovels };
}

// ---- Categories ----

export async function scrapeCategories(): Promise<Category[]> {
  const html = await fetchHTML('/fenlei/');
  const categories: Category[] = [];

  const regex = /<div class="fenlei">([\s\S]*?)<\/div>/g;
  const fenleiMatch = regex.exec(html);
  if (fenleiMatch) {
    const linkRegex = /<a href="([^"]*)"[^>]*>([^<]+)<\/a>/g;
    let m;
    while ((m = linkRegex.exec(fenleiMatch[1])) !== null) {
      const text = m[2].trim();
      const match = text.match(/^(.+)\((\d+)部\)$/);
      if (match) {
        categories.push({
          slug: m[1].replace(/\//g, ''),
          name: match[1],
          count: parseInt(match[2]),
        });
      }
    }
  }

  return categories;
}

// ---- Category novels ----

export async function scrapeCategory(category: string, page: number = 1) {
  const url = page > 1 ? `/${category}/${page}.html` : `/${category}/`;
  const html = await fetchHTML(url);

  const novels: Novel[] = [];
  const blocks = extractItemBlocks(html);
  for (const block of blocks) {
    const item = parseNovelItemFromHtml(block, true);
    if (item) novels.push(item);
  }

  const totalPages = parsePagination(html);

  return { novels, page, totalPages };
}

// ---- Ranking ----

export async function scrapeRanking(page: number = 1) {
  const url = page > 1 ? `/paihang/${page}.html` : '/paihang/';
  const html = await fetchHTML(url);

  const novels: HomeItem[] = [];
  const blocks = extractItemBlocks(html);
  blocks.forEach((block, i) => {
    const item = parseNovelItemFromHtml(block);
    if (item) {
      item.rank = i + 1 + (page - 1) * 10;
      novels.push(item);
    }
  });

  const totalPages = parsePagination(html);

  return { novels, page, totalPages };
}

// ---- Completed novels ----

export async function scrapeCompleted(page: number = 1) {
  const url = page > 1 ? `/wanjie/${page}.html` : '/wanjie/';
  const html = await fetchHTML(url);

  const novels: Novel[] = [];
  const blocks = extractItemBlocks(html);
  for (const block of blocks) {
    const item = parseNovelItemFromHtml(block, true);
    if (item) novels.push(item);
  }

  const totalPages = parsePagination(html);

  return { novels, page, totalPages };
}

// ---- Latest updates ----

export async function scrapeLatest(page: number = 1) {
  const url = page > 1 ? `/zuixin/${page}.html` : '/zuixin/';
  const html = await fetchHTML(url);

  const novels: HomeItem[] = [];
  const blocks = extractItemBlocks(html);
  for (const block of blocks) {
    const item = parseNovelItemFromHtml(block);
    if (item) novels.push(item);
  }

  const totalPages = parsePagination(html);

  return { novels, page, totalPages };
}

// ---- Search ----

export async function scrapeSearch(keyword: string) {
  const encoded = encodeURIComponent(keyword);
  const url = `/i/sor.aspx?key=${encoded}`;
  const html = await fetchHTML(url);

  const novels: Novel[] = [];
  const blocks = extractItemBlocks(html);
  for (const block of blocks) {
    const item = parseNovelItemFromHtml(block, true);
    if (item) novels.push(item);
  }

  return { novels, keyword };
}

// ---- Novel detail ----

export async function scrapeNovelDetail(bookId: string) {
  const html = await fetchHTML(`/${bookId}/`);

  // Title from .itemtxt heading
  const titleMatch = html.match(
    /<div class="itemtxt">\s*<h[13][^>]*>[\s\S]*?<a[^>]*>([^<]+)<\/a>/
  );
  const title = titleMatch ? titleMatch[1].trim() : '';

  // Cover image from .item
  const coverMatch = html.match(/<div class="item">[\s\S]*?<img[^>]*src="([^"]*)"/);
  const cover = normalizeCover(coverMatch ? coverMatch[1] : '');

  // Status and category from spans
  const spans = [...html.matchAll(/<span>([^<]*)<\/span>/g)];
  const status = spans.length > 0 ? spans[0][1].trim() : '';
  const category = spans.length > 1 ? spans[1][1].trim() : '';

  // Author
  const authorMatch = html.match(/作者[：:]\s*([^<]+)/);
  const author = authorMatch ? authorMatch[1].trim() : '';

  // Word count from <i> inside heading
  const wordsMatch = html.match(/<h[13][^>]*>\s*<i>([^<]*)<\/i>/);
  const words = wordsMatch ? wordsMatch[1].trim() : '';

  // Description from .des.bb
  const descMatch = html.match(/<div class="des bb">([\s\S]*?)<\/div>/);
  const description = descMatch ? extractText(descMatch[1]) : '';

  // Chapters from #list - 使用更宽松的正则
  const chapters: Chapter[] = [];
  const listMatch = html.match(/<div\s+id="list"[^>]*>([\s\S]*?)<\/div>/i);
  console.log('[scrapeNovelDetail]', { bookId, htmlLen: html.length, hasList: !!listMatch, listLen: listMatch?.[1]?.length });
  if (listMatch) {
    // 匹配 /bookId/NUMBER.html 格式的章节链接
    const chapterRegex = new RegExp(`<a[^>]*href=["']/\\d+/(\\d+)\\.html["'][^>]*>([^<]+)</a>`, 'g');
    let m;
    while ((m = chapterRegex.exec(listMatch[1])) !== null) {
      const chapterId = m[1];
      const chapterTitle = m[2].trim();
      if (chapterId && chapterTitle) {
        chapters.push({ id: chapterId, title: chapterTitle, bookId });
      }
    }
  }
  console.log('[scrapeNovelDetail] chapters found:', chapters.length);

  // TXT downloads
  let txtLinks: { url: string; label: string }[] = [];
  try {
    const txtHtml = await fetchHTML(`/${bookId}/txt.html`);
    const txtListMatch = txtHtml.match(/<div id="list">([\s\S]*?)<\/div>/);
    if (txtListMatch) {
      const linkRegex = /<a href="([^"]*)"[^>]*>([^<]+)<\/a>/g;
      let m;
      while ((m = linkRegex.exec(txtListMatch[1])) !== null) {
        const href = m[1];
        const label = m[2].trim();
        if (href && label) {
          txtLinks.push({
            url: normalizeUrl(href),
            label,
          });
        }
      }
    }
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

export async function scrapeChapter(
  bookId: string,
  chapterId: string
): Promise<ChapterContent> {
  const html = await fetchHTML(`/${bookId}/${chapterId}.html`);

  // Book title and chapter title from submenu
  const submenuMatch = html.match(/<div class="submenu">([\s\S]*?)<\/div>/);
  let bookTitle = '';
  let chapterTitle = '';

  if (submenuMatch) {
    const h1Match = submenuMatch[1].match(/<h1>([\s\S]*?)<\/h1>/);
    if (h1Match) {
      const h1Content = h1Match[1];
      const linkMatch = h1Content.match(/<a[^>]*>([^<]+)<\/a>/);
      bookTitle = linkMatch ? linkMatch[1].trim() : '';

      // Chapter title is the h1 text minus the book title prefix
      const fullH1 = extractText(h1Content);
      chapterTitle = fullH1
        .replace(new RegExp(`^${bookTitle.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')}\\s*>\\s*`), '')
        .trim();
    }
  }

  // 抓取所有分页内容（章节可能分多页：-2.html, -3.html）
  // 分页链接规律：/bookId/chapterId-N.html，且"下一页"按钮的 href 包含当前 chapterId
  const seenUrls = new Set<string>();
  const allParagraphs: string[] = [];
  let currentHtml = html;
  const baseUrl = `/${bookId}/${chapterId}`;

  // 最多抓 5 页
  for (let page = 1; page <= 5; page++) {
    const pageHash = currentHtml.length + currentHtml.slice(0, 100);
    if (seenUrls.has(pageHash)) break;
    seenUrls.add(pageHash);

    const pageConMatch = currentHtml.match(/<div\s+class="con"[^>]*>([\s\S]*?)<\/div>\s*<div\s+class="prenext"/i)
      || currentHtml.match(/<div\s+class="con"[^>]*>([\s\S]*?)<\/div>/i);

    let pageParaCount = 0;
    if (pageConMatch) {
      const inner = pageConMatch[1];
      console.log('[scrapeChapter] page', page, 'con raw length:', inner.length);
      if (page === 1) {
        // 检查 con 后面还有没有更多内容
        const conEndIdx = currentHtml.indexOf('</div>', currentHtml.indexOf('class="con"'));
        const afterCon = currentHtml.slice(conEndIdx + 6, conEndIdx + 100);
        console.log('[scrapeChapter] after con:', afterCon);
      }
      const pRegex = /<p(?:\s[^>]*)?>([\s\S]*?)<\/p>/gi;
      let m;
      while ((m = pRegex.exec(inner)) !== null) {
        const text = extractText(m[1]);
        if (text) { allParagraphs.push(text); pageParaCount++; }
      }
      if (pageParaCount === 0) {
        const splitHtml = inner.replace(/<br\s*\/?>/gi, '\n');
        const text = extractText(splitHtml);
        text.split(/\n+/).forEach((line) => {
          const t = line.trim();
          if (t) { allParagraphs.push(t); pageParaCount++; }
        });
      }
    }
    console.log('[scrapeChapter] page', page, 'paragraphs:', pageParaCount, 'total so far:', allParagraphs.length);

    // 查找 prenext 中所有链接
    const prenextMatch = currentHtml.match(/<div\s+class="prenext"[^>]*>([\s\S]*?)<\/div>/i);
    let nextPageHref: string | null = null;
    if (prenextMatch) {
      const allLinks = [...prenextMatch[1].matchAll(/<a[^>]*href="([^"]+)"[^>]*>/g)];
      console.log('[scrapeChapter] page', page, 'prenext links:', allLinks.map(l => l[1]));
      for (const linkMatch of allLinks) {
        const href = linkMatch[1];
        // 分页下一页的 href 形如 /bookId/chapterId-N.html
        // 取最后一个匹配的（第一个是"上一页"，最后一个是"下一页"）
        if (href.includes(`${chapterId}-`) && href.endsWith('.html')) {
          nextPageHref = href; // 覆盖直到最后一个
        }
      }
    }

    if (nextPageHref) {
      const nextPath = nextPageHref.startsWith('http') ? nextPageHref.replace(BASE_URL, '') : nextPageHref;
      try {
        currentHtml = await fetchHTML(nextPath);
      } catch {
        break;
      }
    } else {
      break;
    }
  }

  console.log('[scrapeChapter] total paragraphs:', allParagraphs.length, 'total chars:', allParagraphs.join('').length);
  const content = allParagraphs.join('\n\n');

  // Prev/Next navigation
  const prenextMatch = html.match(/<div class="prenext">([\s\S]*?)<\/div>/);
  let prevChapter: ChapterRef | undefined;
  let nextChapter: ChapterRef | undefined;

  if (prenextMatch) {
    const spans = [...prenextMatch[1].matchAll(/<span>([\s\S]*?)<\/span>/g)];

    // First span = prev
    if (spans.length > 0) {
      const prevLinkMatch = spans[0][1].match(
        /<a href="([^"]*)"[^>]*>([^<]+)<\/a>/
      );
      if (prevLinkMatch) {
        prevChapter = {
          id: prevLinkMatch[1].match(/\/(\d+)\.html/)?.[1] || '',
          title: prevLinkMatch[2].trim(),
        };
      }
    }

    // Last span = next
    if (spans.length > 1) {
      const lastSpan = spans[spans.length - 1][1];
      const nextLinkMatch = lastSpan.match(
        /<a href="([^"]*)"[^>]*>([^<]+)<\/a>/
      );
      if (nextLinkMatch) {
        nextChapter = {
          id: nextLinkMatch[1].match(/\/(\d+)\.html/)?.[1] || '',
          title: nextLinkMatch[2].trim(),
        };
      }
    }
  }

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

  const novels: Novel[] = [];
  const blocks = extractItemBlocks(html);
  for (const block of blocks) {
    const item = parseNovelItemFromHtml(block, true);
    if (item) novels.push(item);
  }

  return { novels, author };
}
