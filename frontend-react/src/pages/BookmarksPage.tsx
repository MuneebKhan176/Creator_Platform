import { useEffect, useState } from "react";
import { fetchBookmarks, unbookmarkPost } from "../api/bookmarks";
import type { BookmarkedPost } from "../api/bookmarks";
import PostCard from "../components/PostCard";

export default function BookmarksPage() {
  const [bookmarks, setBookmarks] = useState<BookmarkedPost[]>([]);
  const [page, setPage] = useState(0);
  const [hasMore, setHasMore] = useState(false);
  const [loading, setLoading] = useState(true);
  const [loadingMore, setLoadingMore] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function loadBookmarks(nextPage: number, append: boolean) {
    try {
      if (append) setLoadingMore(true);
      else setLoading(true);
      setError(null);

      const result = await fetchBookmarks(nextPage, 20);
      setBookmarks((previous) => (append ? [...previous, ...result.bookmarks] : result.bookmarks));
      setPage(result.page);
      setHasMore(result.hasMore);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to load bookmarks.");
    } finally {
      setLoading(false);
      setLoadingMore(false);
    }
  }

  useEffect(() => {
    loadBookmarks(0, false);
  }, []);

  async function handleRemoveBookmark(postId: number) {
    try {
      await unbookmarkPost(postId);
      setBookmarks((previous) => previous.filter((item) => item.post.id !== postId));
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to remove bookmark.");
    }
  }

  return (
    <main className="min-h-screen bg-gray-100 px-4 py-8">
      <div className="mx-auto w-full max-w-lg space-y-4">
        <div>
          <h1 className="text-xl font-bold text-gray-900">Bookmarks</h1>
          <p className="mt-1 text-sm text-gray-500">Posts you saved for later.</p>
        </div>

        {loading && <p className="text-center text-sm text-gray-500">Loading bookmarks...</p>}
        {error && <p className="text-center text-sm text-red-600">{error}</p>}
        {!loading && !error && bookmarks.length === 0 && (
          <p className="text-center text-sm text-gray-500">You have no bookmarked posts.</p>
        )}

        {bookmarks.map((item) => (
          <div key={item.post.id} className="relative">
            <PostCard post={item.post} />
            <button
              type="button"
              onClick={() => handleRemoveBookmark(item.post.id)}
              className="absolute right-4 top-4 rounded-md border border-gray-200 bg-white px-2 py-1 text-xs font-semibold text-gray-600 hover:bg-gray-50"
            >
              Remove bookmark
            </button>
          </div>
        ))}

        {hasMore && !loading && (
          <button
            type="button"
            onClick={() => loadBookmarks(page + 1, true)}
            disabled={loadingMore}
            className="w-full rounded-md bg-gray-800 px-4 py-2 text-sm font-semibold text-white hover:bg-gray-900 disabled:opacity-60"
          >
            {loadingMore ? "Loading..." : "Load more"}
          </button>
        )}
      </div>
    </main>
  );
}
