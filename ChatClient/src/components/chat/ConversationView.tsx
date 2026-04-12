import { useEffect, useRef, useState } from 'react'
import { getMessages } from '../../lib/api'
import { useMessageStore } from '../../store/messageStore'
import { MessageList } from './MessageList'
import { MessageInput } from './MessageInput'
import { Spinner } from '../ui/Spinner'
import { ErrorBanner } from '../ui/ErrorBanner'
import type { StoredMessage } from '../../types'

const EMPTY_MESSAGES: StoredMessage[] = []

interface ConversationViewProps {
  conversationId: string
  groupName: string
  currentUserId: string
}

export function ConversationView({
  conversationId,
  groupName,
  currentUserId,
}: ConversationViewProps) {
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const scrollRef = useRef<HTMLDivElement>(null)

  const messages = useMessageStore((s) => s.messages[conversationId] ?? EMPTY_MESSAGES)
  const setMessages = useMessageStore((s) => s.setMessages)

  useEffect(() => {
    setLoading(true)
    setError(null)
    getMessages(conversationId)
      .then((msgs) => setMessages(conversationId, msgs))
      .catch((e) => setError(e.message))
      .finally(() => setLoading(false))
  }, [conversationId])

  useEffect(() => {
    const el = scrollRef.current
    if (el) el.scrollTop = el.scrollHeight
  }, [messages.length])

  return (
    <div className="flex flex-col flex-1 min-h-0">
      {/* Header */}
      <div className="flex items-center gap-2 px-5 py-3 border-b border-surface-border bg-surface-panel flex-shrink-0">
        <span className="text-text-muted">#</span>
        <span className="text-sm font-mono text-text-primary">{groupName}</span>
      </div>

      {/* Messages */}
      <div ref={scrollRef} className="flex-1 overflow-y-auto px-5 py-4 min-h-0">
        {loading && (
          <div className="flex justify-center py-8">
            <Spinner />
          </div>
        )}
        {error && <ErrorBanner message={error} />}
        {!loading && (
          <MessageList messages={messages} currentUserId={currentUserId} />
        )}
      </div>

      {/* Input */}
      <MessageInput conversationId={conversationId} groupName={groupName} />
    </div>
  )
}
