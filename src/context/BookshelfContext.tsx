import React, { createContext, useContext, useState, useEffect, useCallback } from 'react';
import type { BookshelfItem } from '../types';
import { getBookshelf, addToBookshelf, removeFromBookshelf, isInBookshelf } from '../services/storage';

interface BookshelfContextType {
  bookshelf: BookshelfItem[];
  addBook: (book: BookshelfItem) => Promise<void>;
  removeBook: (bookId: string) => Promise<void>;
  isBookInShelf: (bookId: string) => Promise<boolean>;
  refresh: () => Promise<void>;
}

const BookshelfContext = createContext<BookshelfContextType>({
  bookshelf: [],
  addBook: async () => {},
  removeBook: async () => {},
  isBookInShelf: async () => false,
  refresh: async () => {},
});

let shelfLock = Promise.resolve();

export function BookshelfProvider({ children }: { children: React.ReactNode }) {
  const [bookshelf, setBookshelf] = useState<BookshelfItem[]>([]);

  const refresh = useCallback(async () => {
    const data = await getBookshelf();
    setBookshelf(data);
  }, []);

  useEffect(() => {
    refresh();
  }, [refresh]);

  const addBook = useCallback(async (book: BookshelfItem) => {
    shelfLock = shelfLock.then(async () => {
      await addToBookshelf(book);
      await refresh();
    });
    return shelfLock;
  }, [refresh]);

  const removeBook = useCallback(async (bookId: string) => {
    shelfLock = shelfLock.then(async () => {
      await removeFromBookshelf(bookId);
      await refresh();
    });
    return shelfLock;
  }, [refresh]);

  return (
    <BookshelfContext.Provider value={{ bookshelf, addBook, removeBook, isBookInShelf: isInBookshelf, refresh }}>
      {children}
    </BookshelfContext.Provider>
  );
}

export const useBookshelf = () => useContext(BookshelfContext);
