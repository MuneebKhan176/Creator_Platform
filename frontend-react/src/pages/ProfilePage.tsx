import { useEffect, useRef, useState, type FormEvent } from "react";
import { Link } from "react-router-dom";
import { apiGet, apiPut, apiUpload } from "../api/client";

interface ProfileResponse {
  username: string;
  email: string;
  displayName: string;
  bio: string | null;
  location: string | null;
  websiteUrl: string | null;
  profilePictureUrl: string | null;
  bannerUrl: string | null;
  createdAt: string;
}

interface ProfileFormState {
  displayName: string;
  bio: string;
  location: string;
  websiteUrl: string;
}

const emptyForm: ProfileFormState = { displayName: "", bio: "", location: "", websiteUrl: "" };

export default function ProfilePage() {
  const [profile, setProfile] = useState<ProfileResponse | null>(null);
  const [form, setForm] = useState<ProfileFormState>(emptyForm);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [uploadingPicture, setUploadingPicture] = useState(false);
  const [uploadingBanner, setUploadingBanner] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);

  const pictureInputRef = useRef<HTMLInputElement>(null);
  const bannerInputRef = useRef<HTMLInputElement>(null);

  useEffect(() => {
    loadProfile();
  }, []);

  async function loadProfile() {
    setLoading(true);
    setError(null);
    try {
      const res = await apiGet<ProfileResponse>("/api/user/profile");
      const data = res.data!;
      setProfile(data);
      setForm({
        displayName: data.displayName ?? "",
        bio: data.bio ?? "",
        location: data.location ?? "",
        websiteUrl: data.websiteUrl ?? "",
      });
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to load profile");
    } finally {
      setLoading(false);
    }
  }

  async function handleSave(e: FormEvent) {
    e.preventDefault();
    setSaving(true);
    setError(null);
    setSuccessMessage(null);
    try {
      const res = await apiPut<ProfileResponse>("/api/user/profile", form);
      setProfile(res.data!);
      setSuccessMessage("Profile updated");
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to update profile");
    } finally {
      setSaving(false);
    }
  }

  async function handleFileSelected(
    file: File | undefined,
    endpoint: "/api/user/profile/picture" | "/api/user/profile/banner",
    setUploading: (v: boolean) => void
  ) {
    if (!file) return;

    if (!["image/jpeg", "image/png", "image/webp"].includes(file.type)) {
      setError("Only JPEG, PNG, or WEBP images are allowed");
      return;
    }
    if (file.size > 5 * 1024 * 1024) {
      setError("Image must be smaller than 5MB");
      return;
    }

    setUploading(true);
    setError(null);
    setSuccessMessage(null);
    try {
      const res = await apiUpload<ProfileResponse>(endpoint, file);
      setProfile(res.data!);
      setSuccessMessage("Image updated");
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to upload image");
    } finally {
      setUploading(false);
    }
  }

  if (loading) {
    return (
      <main className="min-h-screen flex items-center justify-center bg-gray-100">
        <p className="text-sm text-gray-500">Loading profile…</p>
      </main>
    );
  }

  return (
    <main className="min-h-screen bg-gray-100 pb-16">
      <div className="mx-auto max-w-3xl">
        {/* Banner */}
        <div className="relative h-48 w-full overflow-hidden bg-gradient-to-r from-gray-700 to-gray-900 sm:rounded-b-xl">
          {profile?.bannerUrl && (
            <img src={profile.bannerUrl} alt="Profile banner" className="h-full w-full object-cover" />
          )}
          <button
            type="button"
            onClick={() => bannerInputRef.current?.click()}
            disabled={uploadingBanner}
            className="absolute bottom-3 right-3 rounded-md bg-black/50 px-3 py-1.5 text-xs font-medium text-white backdrop-blur transition hover:bg-black/70 disabled:opacity-60"
          >
            {uploadingBanner ? "Uploading…" : "Change banner"}
          </button>
          <input
            ref={bannerInputRef}
            type="file"
            accept="image/jpeg,image/png,image/webp"
            className="hidden"
            onChange={(e) =>
              handleFileSelected(e.target.files?.[0], "/api/user/profile/banner", setUploadingBanner)
            }
          />

          {/* Profile picture, overlapping the banner */}
          <div className="absolute -bottom-12 left-6">
            <div className="relative h-24 w-24 rounded-full ring-4 ring-gray-100 bg-gray-300 overflow-hidden">
              {profile?.profilePictureUrl ? (
                <img src={profile.profilePictureUrl} alt="Profile" className="h-full w-full object-cover" />
              ) : (
                <div className="flex h-full w-full items-center justify-center text-lg font-semibold text-gray-600">
                  {profile?.displayName?.[0]?.toUpperCase() ?? "?"}
                </div>
              )}
              <button
                type="button"
                onClick={() => pictureInputRef.current?.click()}
                disabled={uploadingPicture}
                className="absolute inset-0 flex items-center justify-center bg-black/0 text-xs font-medium text-transparent transition hover:bg-black/40 hover:text-white disabled:bg-black/40 disabled:text-white"
              >
                {uploadingPicture ? "Uploading…" : "Change"}
              </button>
              <input
                ref={pictureInputRef}
                type="file"
                accept="image/jpeg,image/png,image/webp"
                className="hidden"
                onChange={(e) =>
                  handleFileSelected(e.target.files?.[0], "/api/user/profile/picture", setUploadingPicture)
                }
              />
            </div>
          </div>
        </div>

        <div className="px-6 pt-16">
          <div className="flex items-center justify-between">
            <div>
              <h1 className="text-xl font-semibold text-gray-900">{profile?.displayName}</h1>
              <p className="text-sm text-gray-500">@{profile?.username}</p>
            </div>
            <Link to="/dashboard" className="text-sm font-medium text-gray-500 hover:text-gray-800">
              ← Back to dashboard
            </Link>
          </div>

          {(error || successMessage) && (
            <div
              className={`mt-4 rounded-md px-4 py-2 text-sm ${
                error ? "bg-red-50 text-red-700" : "bg-green-50 text-green-700"
              }`}
            >
              {error ?? successMessage}
            </div>
          )}

          <form onSubmit={handleSave} className="mt-6 space-y-5 rounded-xl border border-gray-200 bg-white p-6 shadow-sm">
            <div>
              <label className="block text-sm font-medium text-gray-700">Display name</label>
              <input
                type="text"
                maxLength={100}
                value={form.displayName}
                onChange={(e) => setForm((f) => ({ ...f, displayName: e.target.value }))}
                className="mt-1 w-full rounded-md border border-gray-300 px-3 py-2 text-sm focus:border-gray-500 focus:outline-none focus:ring-1 focus:ring-gray-500"
                required
              />
            </div>

            <div>
              <div className="flex items-center justify-between">
                <label className="block text-sm font-medium text-gray-700">Bio</label>
                <span className="text-xs text-gray-400">{form.bio.length}/500</span>
              </div>
              <textarea
                maxLength={500}
                rows={3}
                value={form.bio}
                onChange={(e) => setForm((f) => ({ ...f, bio: e.target.value }))}
                className="mt-1 w-full rounded-md border border-gray-300 px-3 py-2 text-sm focus:border-gray-500 focus:outline-none focus:ring-1 focus:ring-gray-500"
              />
            </div>

            <div className="grid grid-cols-1 gap-5 sm:grid-cols-2">
              <div>
                <label className="block text-sm font-medium text-gray-700">Location</label>
                <input
                  type="text"
                  maxLength={100}
                  value={form.location}
                  onChange={(e) => setForm((f) => ({ ...f, location: e.target.value }))}
                  className="mt-1 w-full rounded-md border border-gray-300 px-3 py-2 text-sm focus:border-gray-500 focus:outline-none focus:ring-1 focus:ring-gray-500"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700">Website</label>
                <input
                  type="text"
                  maxLength={255}
                  placeholder="https://"
                  value={form.websiteUrl}
                  onChange={(e) => setForm((f) => ({ ...f, websiteUrl: e.target.value }))}
                  className="mt-1 w-full rounded-md border border-gray-300 px-3 py-2 text-sm focus:border-gray-500 focus:outline-none focus:ring-1 focus:ring-gray-500"
                />
              </div>
            </div>

            <div className="flex items-center justify-between border-t border-gray-100 pt-4">
              <span className="text-xs text-gray-400">{profile?.email}</span>
              <button
                type="submit"
                disabled={saving}
                className="rounded-md bg-gray-800 px-4 py-2 text-sm font-semibold text-white transition hover:bg-gray-900 disabled:opacity-60"
              >
                {saving ? "Saving…" : "Save changes"}
              </button>
            </div>
          </form>
        </div>
      </div>
    </main>
  );
}