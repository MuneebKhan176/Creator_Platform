// frontend-react/src/api/posts.ts — added likeCount/likedByCurrentUser to Post, rest unchanged
import { apiGet, apiPostForm } from "./client";

export type PostMediaType = "IMAGE" | "VIDEO";

export interface PostMedia {
  id: number;
  mediaType: PostMediaType;
  url: string;
  createdAt: string;
}

export interface Post {
  id: number;
  authorId: number;
  authorUsername: string;
  content: string | null;
  createdAt: string;
  media: PostMedia[];
  commentCount: number;
  likeCount: number;              // NEW
  likedByCurrentUser: boolean;    // NEW
}

export interface FeedResponse {
  posts: Post[];
  page: number;
  size: number;
  totalElements: number;
  hasMore: boolean;
}

export async function fetchFeed(page = 0, size = 50): Promise<FeedResponse> {
  const result = await apiGet<FeedResponse>(`/api/v1/posts?page=${page}&size=${size}`);
  return result.data as FeedResponse;
}

export async function createPost(content: string, images: File[], video: File | null): Promise<Post> {
  const formData = new FormData();
  formData.append("content", content);
  images.forEach((file) => formData.append("media", file));
  if (video) {
    formData.append("media", video);
  }
  const result = await apiPostForm<Post>("/api/v1/posts", formData);
  return result.data as Post;
}