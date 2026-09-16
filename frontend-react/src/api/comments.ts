// frontend-react/src/api/comments.ts
import { apiGet, apiPost } from "./client";

export interface Comment {
  id: number;
  postId: number;
  authorId: number;
  authorUsername: string;
  content: string;
  createdAt: string;
}

export interface CommentListResponse {
  comments: Comment[];
  page: number;
  size: number;
  totalElements: number;
  hasMore: boolean;
}

export async function fetchComments(postId: number, page = 0, size = 20): Promise<CommentListResponse> {
  const result = await apiGet<CommentListResponse>(`/api/v1/posts/${postId}/comments?page=${page}&size=${size}`);
  return result.data as CommentListResponse;
}

export async function createComment(postId: number, content: string): Promise<Comment> {
  const result = await apiPost<Comment>(`/api/v1/posts/${postId}/comments`, { content });
  return result.data as Comment;
}