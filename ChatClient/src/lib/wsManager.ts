import { WS_URL, WS_RECONNECT_DELAY_MS } from './constants'
import { useMessageStore } from '../store/messageStore'
import type { OutboundWsMessage, InboundWsSend } from '../types'

class WsManager {
  private ws: WebSocket | null = null
  private reconnectTimer: ReturnType<typeof setTimeout> | null = null
  private token: string | null = null
  private shouldReconnect = false

  connect(token: string) {
    this.token = token
    this.shouldReconnect = true
    this.open()
  }

  private open() {
    if (this.ws) {
      this.ws.onclose = null
      this.ws.onerror = null
      this.ws.close()
    }

    this.ws = new WebSocket(`${WS_URL}?token=${this.token}`)

    this.ws.onmessage = (event) => {
      try {
        const msg: OutboundWsMessage = JSON.parse(event.data)
        useMessageStore.getState().appendMessage(msg.conversationId, {
          fromUserId: msg.fromUserId,
          conversationId: msg.conversationId,
          content: msg.content,
          sentAt: new Date().toISOString(),
        })
      } catch {
        // ignore malformed frames
      }
    }

    this.ws.onclose = () => {
      if (this.shouldReconnect) {
        this.scheduleReconnect()
      }
    }

    this.ws.onerror = () => {
      this.ws?.close()
    }
  }

  private scheduleReconnect() {
    if (this.reconnectTimer) return
    this.reconnectTimer = setTimeout(() => {
      this.reconnectTimer = null
      const currentToken = localStorage.getItem('jwt') ?? this.token
      if (currentToken && this.shouldReconnect) {
        this.token = currentToken
        this.open()
      }
    }, WS_RECONNECT_DELAY_MS)
  }

  send(payload: InboundWsSend) {
    if (this.ws?.readyState === WebSocket.OPEN) {
      this.ws.send(JSON.stringify(payload))
    }
  }

  disconnect() {
    this.shouldReconnect = false
    if (this.reconnectTimer) {
      clearTimeout(this.reconnectTimer)
      this.reconnectTimer = null
    }
    if (this.ws) {
      this.ws.onclose = null
      this.ws.onerror = null
      this.ws.close()
      this.ws = null
    }
    this.token = null
  }

  get isConnected(): boolean {
    return this.ws?.readyState === WebSocket.OPEN
  }
}

export const wsManager = new WsManager()
