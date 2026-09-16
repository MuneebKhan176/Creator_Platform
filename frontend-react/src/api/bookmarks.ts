// frontend-react/src/api/bookmarks.ts
import { apiGet, apiPost, apiDelete } from "./client";
import type { Post } from "./posts";

export interface BookmarkStatus {
  postId: number;
  bookmarked: boolean;
}

export interface BookmarkedPost {
  post: Post;
  bookmarkedAt: string;
}

export interface BookmarkListResponse {
  bookmarks: BookmarkedPost[];
  page: number;
  size: number;
  totalElements: number;
  hasMore: boolean;
}

export async function bookmarkPost(postId: number): Promise<BookmarkStatus> {
  const result = await apiPost<BookmarkStatus>(`/api/v1/profile/bookmarks/${postId}`, {});
  return result.data as BookmarkStatus;
}

export async function unbookmarkPost(postId: number): Promise<BookmarkStatus> {
  const result = await apiDelete<BookmarkStatus>(`/api/v1/profile/bookmarks/${postId}`);
  return result.data as BookmarkStatus;
}

export async function fetchBookmarks(page = 0, size = 20): Promise<BookmarkListResponse> {
  const result = await apiGet<BookmarkListResponse>(`/api/v1/profile/bookmarks?page=${page}&size=${size}`);
  return result.data as BookmarkListResponse;
}