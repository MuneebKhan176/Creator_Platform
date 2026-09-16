import { useEffect, useState } from "react";
import { fetchBookmarks } from "../api/bookmarks";
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

  function handleBookmarkChange(postId: number, bookmarked: boolean) {
    if (!bookmarked) {
      setBookmarks((previous) => previous.filter((item) => item.post.id !== postId));
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
          <PostCard
            key={item.post.id}
            post={item.post}
            initialBookmarked={true}
            onBookmarkChange={(bookmarked) => handleBookmarkChange(item.post.id, bookmarked)}
          />
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
