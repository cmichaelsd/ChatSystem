import type { StoredMessage } from '../../types'
import { MessageBubble } from './MessageBubble'

interface MessageListProps {
  messages: StoredMessage[]
  currentUserId: string
}

export function MessageList({ messages, currentUserId }: MessageListProps) {
  if (messages.length === 0) {
    return (
      <div className="flex-1 flex items-center justify-center text-text-muted text-sm">
        No messages yet. Say something.
      </div>
    )
  }

  return (
    <div className="flex flex-col">
      {messages.map((msg, i) => (
        <MessageBubble key={`${msg.fromUserId}-${msg.sentAt}-${i}`} message={msg} currentUserId={currentUserId} />
      ))}
    </div>
  )
}
