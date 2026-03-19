export function dispatchLogoutEvent() {
  window.dispatchEvent(new CustomEvent("app:logout"));
}