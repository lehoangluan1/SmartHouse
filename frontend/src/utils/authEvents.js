const LOGOUT_EVENT_NAME = "app:logout";

export function dispatchLogoutEvent() {
  window.dispatchEvent(new Event(LOGOUT_EVENT_NAME));
}

export function addLogoutListener(handler) {
  window.addEventListener(LOGOUT_EVENT_NAME, handler);
  return () => window.removeEventListener(LOGOUT_EVENT_NAME, handler);
}