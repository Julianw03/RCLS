const BACKEND_PORT = import.meta.env.VITE_BACKEND_PORT || '443';

export const BACKEND_URL = `https://127.0.0.1${BACKEND_PORT === '443' ? '' : `:${BACKEND_PORT}`}`;
export const BACKEND_WEB_SOCKET_URL = `wss://127.0.0.1${BACKEND_PORT === '443' ? '' : `:${BACKEND_PORT}`}/ws`;