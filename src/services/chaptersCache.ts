// Global chapters cache - avoids passing huge arrays through navigation params
let currentChapters: { id: string; title: string }[] = [];
let currentBookId = '';

export function setChapters(bookId: string, chapters: { id: string; title: string }[]) {
  currentBookId = bookId;
  currentChapters = chapters;
}

export function getChapters(bookId: string): { id: string; title: string }[] {
  if (bookId === currentBookId) return currentChapters;
  return [];
}
