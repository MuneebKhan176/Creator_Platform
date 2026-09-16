// frontend-react/src/api/follow.ts
import { apiGet, apiPost, apiDelete } from "./client";

export interface FollowStatus {
  userId: number;
  following: boolean;
  followerCount: number;
  followingCount: number;
}

export interface UserSummary {
  id: number;
  username: string;
}

export interface UserListResponse {
  users: UserSummary[];
  page: number;
  size: number;
  totalElements: number;
  hasMore: boolean;
}

export async function followUser(userId: number): Promise<FollowStatus> {
  const result = await apiPost<FollowStatus>(`/api/v1/profile/${userId}/follow`, {});
  return result.data as FollowStatus;
}

export async function unfollowUser(userId: number): Promise<FollowStatus> {
  const result = await apiDelete<FollowStatus>(`/api/v1/profile/${userId}/follow`);
  return result.data as FollowStatus;
}

export async function getFollowStatus(userId: number): Promise<FollowStatus> {
  const result = await apiGet<FollowStatus>(`/api/v1/profile/${userId}/follow-status`);
  return result.data as FollowStatus;
}

export async function getFollowers(userId: number, page = 0, size = 20): Promise<UserListResponse> {
  const result = await apiGet<UserListResponse>(`/api/v1/profile/${userId}/followers?page=${page}&size=${size}`);
  return result.data as UserListResponse;
}

export async function getFollowing(userId: number, page = 0, size = 20): Promise<UserListResponse> {
  const result = await apiGet<UserListResponse>(`/api/v1/profile/${userId}/following?page=${page}&size=${size}`);
  return result.data as UserListResponse;
}