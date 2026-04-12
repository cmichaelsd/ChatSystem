import { create } from 'zustand'
import type { StoredMessage } from '../types'

interface MessageState {
  messages: Record<string, StoredMessage[]>
  setMessages: (conversationId: string, messages: StoredMessage[]) => void
  appendMessage: (conversationId: string, message: StoredMessage) => void
}

export const useMessageStore = create<MessageState>((set) => ({
  messages: {},

  setMessages: (conversationId, messages) =>
    set((s) => ({ messages: { ...s.messages, [conversationId]: messages } })),

  appendMessage: (conversationId, message) =>
    set((s) => ({
      messages: {
        ...s.messages,
        [conversationId]: [...(s.messages[conversationId] ?? []), message],
      },
    })),
}))
