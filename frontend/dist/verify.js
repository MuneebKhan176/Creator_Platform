import { apiPost } from "./api.js";
const form = document.getElementById("verify-form");
const emailInput = document.getElementById("email");
const codeInput = document.getElementById("code");
const submitButton = document.getElementById("submit-btn");
const messageEl = document.getElementById("message");
function setState(state, text) {
    submitButton.disabled = state === "verifying" || state === "success";
    switch (state) {
        case "verifying":
            submitButton.textContent = "Verifying...";
            messageEl.textContent = "Verifying...";
            messageEl.className = "message info";
            messageEl.hidden = false;
            break;
        case "success":
            submitButton.textContent = "Verified";
            messageEl.textContent = text ?? "Verification successful.";
            messageEl.className = "message success";
            messageEl.hidden = false;
            break;
        case "error":
            submitButton.textContent = "Verify";
            // Server messages already distinguish "Invalid verification code" vs
            // "Verification code has expired" vs "Registration failed" cases.
            messageEl.textContent = text ?? "Registration failed.";
            messageEl.className = "message error";
            messageEl.hidden = false;
            break;
        default:
            submitButton.textContent = "Verify";
            messageEl.hidden = true;
            messageEl.textContent = "";
    }
}
// Pre-fill the email from the query string set by the registration page.
const emailFromQuery = new URLSearchParams(window.location.search).get("email");
if (emailFromQuery) {
    emailInput.value = emailFromQuery;
}
form.addEventListener("submit", async (event) => {
    event.preventDefault();
    const email = emailInput.value.trim();
    const code = codeInput.value.trim();
    if (!email) {
        setState("error", "Email is required.");
        return;
    }
    if (!/^\d{6}$/.test(code)) {
        setState("error", "Enter the 6-digit code from your email.");
        return;
    }
    setState("verifying");
    try {
        const result = await apiPost("/api/auth/verify-email", { email, code });
        setState("success", result.message);
    }
    catch (err) {
        const message = err instanceof Error ? err.message : "Registration failed.";
        setState("error", message);
    }
});
