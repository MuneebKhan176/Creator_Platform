// frontend-react/src/api/likes.ts
import { apiPost, apiDelete } from "./client";

export interface LikeStatus {
  postId: number;
  liked: boolean;
  likeCount: number;
}

export async function likePost(postId: number): Promise<LikeStatus> {
  const result = await apiPost<LikeStatus>(`/api/v1/posts/${postId}/likes`, {});
  return result.data as LikeStatus;
}

export async function unlikePost(postId: number): Promise<LikeStatus> {
  const result = await apiDelete<LikeStatus>(`/api/v1/posts/${postId}/likes`);
  return result.data as LikeStatus;
}