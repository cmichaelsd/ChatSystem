export const API_BASE = import.meta.env.VITE_API_BASE ?? 'http://localhost:8001'
export const WS_URL = import.meta.env.VITE_WS_URL ?? 'ws://localhost:8080/ws'

export const PRESENCE_POLL_INTERVAL_MS = 10_000
export const WS_RECONNECT_DELAY_MS = 3_000
