import { apiPost } from "./api.js";

const form = document.getElementById("register-form") as HTMLFormElement;
const usernameInput = document.getElementById("username") as HTMLInputElement;
const emailInput = document.getElementById("email") as HTMLInputElement;
const passwordInput = document.getElementById("password") as HTMLInputElement;
const confirmPasswordInput = document.getElementById("confirmPassword") as HTMLInputElement;
const submitButton = document.getElementById("submit-btn") as HTMLButtonElement;
const messageEl = document.getElementById("message") as HTMLDivElement;

const EMAIL_REGEX = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
const MIN_PASSWORD_LENGTH = 8;

function showMessage(text: string, kind: "error" | "info" | "success"): void {
  messageEl.textContent = text;
  messageEl.className = `message ${kind}`;
  messageEl.hidden = false;
}

function clearMessage(): void {
  messageEl.hidden = true;
  messageEl.textContent = "";
}

function setLoading(isLoading: boolean): void {
  submitButton.disabled = isLoading;
  submitButton.textContent = isLoading ? "Registering..." : "Register";
}

// Frontend validation is a UX convenience only; the backend re-validates everything.
function validateForm(username: string, email: string, password: string, confirmPassword: string): string | null {
  if (!username.trim()) return "Username is required.";
  if (!email.trim()) return "Email is required.";
  if (!EMAIL_REGEX.test(email.trim())) return "Please enter a valid email address.";
  if (!password) return "Password is required.";
  if (password.length < MIN_PASSWORD_LENGTH) return `Password must be at least ${MIN_PASSWORD_LENGTH} characters.`;
  if (!confirmPassword) return "Please confirm your password.";
  if (password !== confirmPassword) return "Passwords do not match.";
  return null;
}

form.addEventListener("submit", async (event: SubmitEvent) => {
  event.preventDefault();
  clearMessage();

  const username = usernameInput.value;
  const email = emailInput.value;
  const password = passwordInput.value;
  const confirmPassword = confirmPasswordInput.value;

  const validationError = validateForm(username, email, password, confirmPassword);
  if (validationError) {
    showMessage(validationError, "error");
    return;
  }

  setLoading(true);
  try {
    const result = await apiPost("/api/auth/register", {
      username: username.trim(),
      email: email.trim(),
      password,
      confirmPassword,
    });

    showMessage(result.message, "success");

    // Move to the verification page with the email pre-filled.
    const params = new URLSearchParams({ email: email.trim() });
    window.location.href = `verify.html?${params.toString()}`;
  } catch (err) {
    const message = err instanceof Error ? err.message : "Registration failed.";
    showMessage(message, "error");
  } finally {
    setLoading(false);
  }
});
