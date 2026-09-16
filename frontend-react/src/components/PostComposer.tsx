// frontend-react/src/components/PostComposer.tsx
import { useRef, useState } from "react";
import { createPost } from "../api/posts";
import type { Post } from "../api/posts";

const MAX_IMAGES = 10;

interface PostComposerProps {
  onPostCreated: (post: Post) => void;
}

export default function PostComposer({ onPostCreated }: PostComposerProps) {
  const [content, setContent] = useState("");
  const [images, setImages] = useState<File[]>([]);
  const [video, setVideo] = useState<File | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const imageInputRef = useRef<HTMLInputElement>(null);
  const videoInputRef = useRef<HTMLInputElement>(null);

  function handleImageChange(e: React.ChangeEvent<HTMLInputElement>) {
    const files = Array.from(e.target.files ?? []);
    setImages((prev) => [...prev, ...files].slice(0, MAX_IMAGES));
  }

  function handleVideoChange(e: React.ChangeEvent<HTMLInputElement>) {
    setVideo(e.target.files?.[0] ?? null);
  }

  function removeImage(index: number) {
    setImages((prev) => prev.filter((_, i) => i !== index));
  }

  function resetForm() {
    setContent("");
    setImages([]);
    setVideo(null);
    if (imageInputRef.current) imageInputRef.current.value = "";
    if (videoInputRef.current) videoInputRef.current.value = "";
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setError(null);

    if (!content.trim() && images.length === 0 && !video) {
      setError("Write something or attach an image/video before posting.");
      return;
    }

    setSubmitting(true);
    try {
      const post = await createPost(content.trim(), images, video);
      onPostCreated(post);
      resetForm();
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to create post.");
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <form onSubmit={handleSubmit} className="rounded-xl border border-gray-200 bg-white p-4 shadow-sm">
      <textarea
        value={content}
        onChange={(e) => setContent(e.target.value)}
        placeholder="What's on your mind?"
        rows={3}
        className="w-full resize-none rounded-md border border-gray-300 p-3 text-sm text-gray-900 focus:border-gray-500 focus:outline-none"
      />

      {images.length > 0 && (
        <div className="mt-3 flex flex-wrap gap-2">
          {images.map((file, index) => (
            <div key={`${file.name}-${index}`} className="relative">
              <img
                src={URL.createObjectURL(file)}
                alt={file.name}
                className="h-20 w-20 rounded-md object-cover"
              />
              <button
                type="button"
                onClick={() => removeImage(index)}
                className="absolute -right-1.5 -top-1.5 flex h-5 w-5 items-center justify-center rounded-full bg-gray-800 text-xs text-white"
                aria-label="Remove image"
              >
                ×
              </button>
            </div>
          ))}
        </div>
      )}

      {video && (
        <div className="mt-3 flex items-center justify-between rounded-md bg-gray-50 px-3 py-2 text-sm text-gray-700">
          <span className="truncate">{video.name}</span>
          <button type="button" onClick={() => setVideo(null)} className="text-gray-400 hover:text-gray-600">
            Remove
          </button>
        </div>
      )}

      {error && <p className="mt-2 text-sm text-red-600">{error}</p>}

      <div className="mt-3 flex items-center justify-between">
        <div className="flex items-center gap-3">
          <label className="cursor-pointer text-sm font-medium text-gray-600 hover:text-gray-900">
            Add images
            <input
              ref={imageInputRef}
              type="file"
              accept="image/jpeg,image/png,image/webp"
              multiple
              onChange={handleImageChange}
              className="hidden"
            />
          </label>
          <label className="cursor-pointer text-sm font-medium text-gray-600 hover:text-gray-900">
            Add video
            <input
              ref={videoInputRef}
              type="file"
              accept="video/mp4,video/quicktime,video/webm"
              onChange={handleVideoChange}
              className="hidden"
            />
          </label>
        </div>

        <button
          type="submit"
          disabled={submitting}
          className="rounded-md bg-gray-800 px-4 py-2 text-sm font-semibold text-white transition hover:bg-gray-900 disabled:opacity-50"
        >
          {submitting ? "Posting..." : "Post"}
        </button>
      </div>
    </form>
  );
}