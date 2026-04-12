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

export interface GroupMemberResponse {
  group_id: string
  user_id: string
  joined_at: string
}

export interface StoredMessage {
  fromUserId: string
  conversationId: string
  content: string
  sentAt: string
}

export interface OutboundWsMessage {
  fromUserId: string
  toUserId: string
  conversationId: string
  content: string
}

export interface InboundWsSend {
  conversationId: string
  content: string
}

export interface PresenceBatchRequest {
  user_ids: string[]
}

export interface PresenceBatchResponse {
  presence: Record<string, boolean>
}
