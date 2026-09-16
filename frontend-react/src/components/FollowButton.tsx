// frontend-react/src/components/FollowButton.tsx
import { useEffect, useState } from "react";
import { followUser, getFollowStatus, unfollowUser } from "../api/follow";

interface FollowButtonProps {
  userId: number;
  initiallyFollowing?: boolean;
}

export default function FollowButton({ userId, initiallyFollowing = false }: FollowButtonProps) {
  const [following, setFollowing] = useState(initiallyFollowing);
  const [pending, setPending] = useState(false);

  useEffect(() => {
    let cancelled = false;
    getFollowStatus(userId)
      .then((status) => {
        if (!cancelled) setFollowing(status.following);
      })
      .catch(() => {});
    return () => {
      cancelled = true;
    };
  }, [userId]);

  async function handleClick() {
    if (pending) return;
    const previous = following;
    setPending(true);
    setFollowing(!previous);
    try {
      const status = previous ? await unfollowUser(userId) : await followUser(userId);
      setFollowing(status.following);
    } catch {
      setFollowing(previous);
    } finally {
      setPending(false);
    }
  }

  return (
    <button
      type="button"
      onClick={handleClick}
      disabled={pending}
      className={`rounded-md px-3 py-1.5 text-xs font-semibold transition disabled:opacity-60 ${
        following
          ? "border border-gray-300 text-gray-700 hover:bg-gray-50"
          : "bg-gray-800 text-white hover:bg-gray-900"
      }`}
    >
      {following ? "Following" : "Follow"}
    </button>
  );
}
