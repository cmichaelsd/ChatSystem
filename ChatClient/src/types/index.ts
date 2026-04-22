export interface UserResponse {
  id: string
  username: string
  email: string
  created_at: string
}

export interface Token {
  access_token: string
  token_type: string
}

export interface GroupResponse {
  id: string
  name: string
  created_by: string
  created_at: string
}

export interface StoredMessage {
  fromUserId: string
  conversationId: string
  content: string
  sentAt: string
}

export interface OutboundWsMessage {
  type: 'chat'
  fromUserId: string
  toUserId: string
  conversationId: string
  content: string
}

export interface WsPresenceEvent {
  type: 'presence'
  userId: string
  online: boolean
}

export interface WsGroupAddedEvent {
  type: 'group_added'
  conversationId: string
}

export type WsIncomingMessage = OutboundWsMessage | WsPresenceEvent | WsGroupAddedEvent

export interface InboundWsSend {
  conversationId: string
  content: string
}

