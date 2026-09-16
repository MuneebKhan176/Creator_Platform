// frontend-react/src/components/PostFeed.tsx
import { useEffect, useState } from "react";
import { fetchFeed } from "../api/posts";
import type { Post } from "../api/posts";
import PostCard from "./PostCard";
import PostComposer from "./PostComposer";

export default function PostFeed() {
  const [posts, setPosts] = useState<Post[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    async function load() {
      try {
        const feed = await fetchFeed();
        if (!cancelled) setPosts(feed.posts);
      } catch (err) {
        if (!cancelled) setError(err instanceof Error ? err.message : "Failed to load feed.");
      } finally {
        if (!cancelled) setLoading(false);
      }
    }
    load();
    return () => {
      cancelled = true;
    };
  }, []);

  function handlePostCreated(post: Post) {
    setPosts((prev) => [post, ...prev]);
  }

  return (
    <div className="mx-auto w-full max-w-lg space-y-4">
      <PostComposer onPostCreated={handlePostCreated} />

      {loading && <p className="text-center text-sm text-gray-500">Loading feed...</p>}
      {error && <p className="text-center text-sm text-red-600">{error}</p>}
      {!loading && !error && posts.length === 0 && (
        <p className="text-center text-sm text-gray-500">No posts yet. Be the first to share something.</p>
      )}

      {posts.map((post) => (
        <PostCard key={post.id} post={post} />
      ))}
    </div>
  );
}