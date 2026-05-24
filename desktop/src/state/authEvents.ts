export const TOKEN_EXPIRED_EVENT = "token-expired";

export function signalTokenExpired(): void {
  window.dispatchEvent(new Event(TOKEN_EXPIRED_EVENT));
}

export function listenTokenExpired(callback: () => void): () => void {
  window.addEventListener(TOKEN_EXPIRED_EVENT, callback);

  return () => {
    window.removeEventListener(TOKEN_EXPIRED_EVENT, callback);
  };
}