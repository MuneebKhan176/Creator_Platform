// frontend-react/src/components/CommentSection.tsx
import { useState } from "react";
import { fetchComments, createComment } from "../api/comments";
import type { Comment } from "../api/comments";

interface CommentSectionProps {
  postId: number;
  initialCount: number;
}

export default function CommentSection({ postId, initialCount }: CommentSectionProps) {
  const [expanded, setExpanded] = useState(false);
  const [comments, setComments] = useState<Comment[]>([]);
  const [count, setCount] = useState(initialCount);
  const [loading, setLoading] = useState(false);
  const [loaded, setLoaded] = useState(false);
  const [text, setText] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function handleToggle() {
    const next = !expanded;
    setExpanded(next);
    if (next && !loaded) {
      setLoading(true);
      setError(null);
      try {
        const result = await fetchComments(postId);
        setComments(result.comments);
        setCount(result.totalElements);
        setLoaded(true);
      } catch (err) {
        setError(err instanceof Error ? err.message : "Failed to load comments.");
      } finally {
        setLoading(false);
      }
    }
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (!text.trim()) return;

    setSubmitting(true);
    setError(null);
    try {
      const comment = await createComment(postId, text.trim());
      setComments((prev) => [...prev, comment]);
      setCount((prev) => prev + 1);
      setText("");
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to add comment.");
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="mt-3 border-t border-gray-100 pt-3">
      <button
        type="button"
        onClick={handleToggle}
        className="text-xs font-semibold text-gray-500 hover:text-gray-800"
      >
        {count === 0 ? "Add a comment" : `${count} comment${count === 1 ? "" : "s"}`}
      </button>

      {expanded && (
        <div className="mt-2 space-y-2">
          {loading && <p className="text-xs text-gray-400">Loading comments...</p>}
          {error && <p className="text-xs text-red-600">{error}</p>}

          {!loading &&
            comments.map((c) => (
              <div key={c.id} className="rounded-md bg-gray-50 px-3 py-2 text-sm">
                <div className="flex items-baseline justify-between">
                  <span className="font-semibold text-gray-800">{c.authorUsername}</span>
                  <span className="text-xs text-gray-400">{new Date(c.createdAt).toLocaleString()}</span>
                </div>
                <p className="mt-0.5 whitespace-pre-wrap text-gray-700">{c.content}</p>
              </div>
            ))}

          <form onSubmit={handleSubmit} className="flex items-center gap-2">
            <input
              value={text}
              onChange={(e) => setText(e.target.value)}
              placeholder="Write a comment..."
              maxLength={1000}
              className="flex-1 rounded-md border border-gray-300 px-3 py-1.5 text-sm focus:border-gray-500 focus:outline-none"
            />
            <button
              type="submit"
              disabled={submitting || !text.trim()}
              className="rounded-md bg-gray-800 px-3 py-1.5 text-xs font-semibold text-white hover:bg-gray-900 disabled:opacity-50"
            >
              {submitting ? "Posting..." : "Reply"}
            </button>
          </form>
        </div>
      )}
    </div>
  );
}