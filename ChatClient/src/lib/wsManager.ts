import { WS_URL, WS_RECONNECT_DELAY_MS } from './constants'
import { getGroup, getGroups } from './api'
import { useMessageStore } from '../store/messageStore'
import { usePresenceStore } from '../store/presenceStore'
import { useGroupStore } from '../store/groupStore'
import type { WsIncomingMessage, InboundWsSend } from '../types'

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

    this.ws.onopen = () => {
      getGroups().then((groups) => {
        useGroupStore.getState().setGroups(groups)
      }).catch(() => {})
    }

    this.ws.onmessage = (event) => {
      try {
        const msg: WsIncomingMessage = JSON.parse(event.data)
        if (msg.type === 'chat') {
          useMessageStore.getState().appendMessage(msg.conversationId, {
            fromUserId: msg.fromUserId,
            conversationId: msg.conversationId,
            content: msg.content,
            sentAt: new Date().toISOString(),
          })
        } else if (msg.type === 'presence') {
          usePresenceStore.getState().setUserPresence(msg.userId, msg.online)
        } else if (msg.type === 'group_added') {
          getGroup(msg.conversationId).then((group) => {
            useGroupStore.getState().addGroup(group)
          }).catch((e) => console.error('Failed to handle group_added event:', e))
        }
      } catch (e) {
        console.error('WS message error:', e)
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
