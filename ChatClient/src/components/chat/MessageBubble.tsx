import { useEffect, useState } from 'react'
import type { StoredMessage } from '../../types'
import { useUserCacheStore } from '../../store/userCacheStore'
import { getUser } from '../../lib/api'

interface MessageBubbleProps {
  message: StoredMessage
  currentUserId: string
}

function formatTime(sentAt: string): string {
  try {
    return new Date(sentAt).toLocaleTimeString([], {
      hour: '2-digit',
      minute: '2-digit',
    })
  } catch {
    return ''
  }
}

export function MessageBubble({ message, currentUserId }: MessageBubbleProps) {
  const isMine = message.fromUserId === currentUserId
  const getCache = useUserCacheStore((s) => s.getUser)
  const addUser = useUserCacheStore((s) => s.addUser)
  const cached = useUserCacheStore((s) => s.cache[message.fromUserId])
  const [, forceRender] = useState(0)

  useEffect(() => {
    if (!cached) {
      getUser(message.fromUserId)
        .then((user) => {
          addUser(user)
          forceRender((n) => n + 1)
        })
        .catch(() => {})
    }
  }, [message.fromUserId])

  const username = getCache(message.fromUserId)?.username ?? message.fromUserId

  return (
    <div className={`flex flex-col mb-4 ${isMine ? 'items-end' : 'items-start'}`}>
      <div className="flex items-baseline gap-2 mb-1">
        {!isMine && (
          <span className="text-xs text-accent-highlight font-mono">{username}</span>
        )}
        <span className="text-xs text-text-muted">{formatTime(message.sentAt)}</span>
        {isMine && (
          <span className="text-xs text-accent-highlight font-mono">{username}</span>
        )}
      </div>
      <div
        className={`
          max-w-[70%] px-3 py-2 text-sm font-mono leading-relaxed
          border
          ${isMine
            ? 'bg-accent-highlight/10 border-accent-highlight/30 text-text-primary'
            : 'bg-surface-elevated border-surface-border text-text-primary'
          }
        `}
      >
        {message.content}
      </div>
    </div>
  )
}
