import { useState } from 'react'
import { wsManager } from '../../lib/wsManager'

interface MessageInputProps {
  conversationId: string
  groupName: string
}

export function MessageInput({ conversationId, groupName }: MessageInputProps) {
  const [content, setContent] = useState('')

  const handleSend = () => {
    const trimmed = content.trim()
    if (!trimmed) return
    wsManager.send({ conversationId, content: trimmed })
    setContent('')
  }

  const handleKeyDown = (e: React.KeyboardEvent<HTMLTextAreaElement>) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault()
      handleSend()
    }
  }

  return (
    <div className="border-t border-surface-border p-4 bg-surface-panel">
      <div className="flex gap-2 items-end border border-surface-border bg-surface-base focus-within:border-accent-highlight/50 transition-colors">
        <textarea
          value={content}
          onChange={(e) => setContent(e.target.value)}
          onKeyDown={handleKeyDown}
          placeholder={`Message #${groupName}`}
          rows={1}
          className="flex-1 bg-transparent text-sm text-text-primary font-mono px-3 py-2.5 resize-none focus:outline-none placeholder:text-text-muted"
          style={{ minHeight: '42px', maxHeight: '120px' }}
          onInput={(e) => {
            const el = e.currentTarget
            el.style.height = 'auto'
            el.style.height = `${Math.min(el.scrollHeight, 120)}px`
          }}
        />
        <button
          onClick={handleSend}
          disabled={!content.trim()}
          className="m-1.5 px-3 py-1.5 text-xs font-mono bg-accent-highlight text-surface-base hover:brightness-110 disabled:opacity-40 disabled:cursor-not-allowed transition-all cursor-pointer"
        >
          Send
        </button>
      </div>
      <div className="mt-1 text-xs text-text-muted">
        Enter to send · Shift+Enter for newline
      </div>
    </div>
  )
}
