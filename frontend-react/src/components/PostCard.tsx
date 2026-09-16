// frontend-react/src/components/PostCard.tsx — added the like button
import { useState } from "react";
import type { Post } from "../api/posts";
import { likePost, unlikePost } from "../api/likes";
import CommentSection from "./CommentSection";

function formatTimestamp(iso: string): string {
  return new Date(iso).toLocaleString();
}

export default function PostCard({ post }: { post: Post }) {
  const [liked, setLiked] = useState(post.likedByCurrentUser);
  const [likeCount, setLikeCount] = useState(post.likeCount);
  const [pending, setPending] = useState(false);

  const images = post.media.filter((m) => m.mediaType === "IMAGE");
  const videos = post.media.filter((m) => m.mediaType === "VIDEO");

  async function handleLikeToggle() {
    if (pending) return;

    // Optimistic update, reverted on failure.
    const previousLiked = liked;
    const previousCount = likeCount;
    setPending(true);
    setLiked(!previousLiked);
    setLikeCount(previousLiked ? previousCount - 1 : previousCount + 1);

    try {
      const status = previousLiked ? await unlikePost(post.id) : await likePost(post.id);
      setLiked(status.liked);
      setLikeCount(status.likeCount);
    } catch {
      setLiked(previousLiked);
      setLikeCount(previousCount);
    } finally {
      setPending(false);
    }
  }

  return (
    <article className="rounded-xl border border-gray-200 bg-white p-4 shadow-sm">
      <div className="flex items-center justify-between">
        <span className="text-sm font-semibold text-gray-900">{post.authorUsername}</span>
        <span className="text-xs text-gray-400">{formatTimestamp(post.createdAt)}</span>
      </div>

      {post.content && <p className="mt-2 whitespace-pre-wrap text-sm text-gray-800">{post.content}</p>}

      {images.length > 0 && (
        <div className={`mt-3 grid gap-2 ${images.length === 1 ? "grid-cols-1" : "grid-cols-2"}`}>
          {images.map((img) => (
            <img key={img.id} src={img.url} alt="" className="w-full rounded-md object-cover" />
          ))}
        </div>
      )}

      {videos.map((vid) => (
        <video key={vid.id} src={vid.url} controls className="mt-3 w-full rounded-md" />
      ))}

      <div className="mt-3 flex items-center gap-4 border-t border-gray-100 pt-3">
        <button
          type="button"
          onClick={handleLikeToggle}
          disabled={pending}
          className={`text-xs font-semibold ${liked ? "text-rose-600" : "text-gray-500 hover:text-gray-800"} disabled:opacity-60`}
        >
          {liked ? "♥" : "♡"} {likeCount > 0 ? likeCount : "Like"}
        </button>
      </div>

      <CommentSection postId={post.id} initialCount={post.commentCount} />
    </article>
  );
}