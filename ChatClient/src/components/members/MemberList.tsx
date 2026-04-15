import { useState } from 'react'
import type { UserResponse } from '../../types'
import { MemberItem } from './MemberItem'
import { usePresenceStore } from '../../store/presenceStore'
import { searchUsers, addGroupMember } from '../../lib/api'

interface MemberListProps {
  members: UserResponse[]
  groupId: string
  isOwner: boolean
  onMemberAdded: () => void
}

export function MemberList({ members, groupId, isOwner, onMemberAdded }: MemberListProps) {
  const presence = usePresenceStore((s) => s.presence)

  const [adding, setAdding] = useState(false)
  const [input, setInput] = useState('')
  const [suggestions, setSuggestions] = useState<UserResponse[]>([])
  const [addError, setAddError] = useState<string | null>(null)
  const [searching, setSearching] = useState(false)

  const memberIdSet = new Set(members.map((m) => m.id))

  const online = members.filter((m) => presence[m.id])
  const offline = members.filter((m) => !presence[m.id])

  async function handleInputChange(value: string) {
    setInput(value)
    setAddError(null)
    if (!value.trim()) {
      setSuggestions([])
      return
    }
    setSearching(true)
    try {
      const results = await searchUsers(value.trim())
      setSuggestions(results.filter((u) => !memberIdSet.has(u.id)))
    } catch {
      setSuggestions([])
    } finally {
      setSearching(false)
    }
  }

  async function handleSelect(user: UserResponse) {
    setAddError(null)
    try {
      await addGroupMember(groupId, user.id)
      setInput('')
      setSuggestions([])
      setAdding(false)
      onMemberAdded()
    } catch (e: unknown) {
      setAddError(e instanceof Error ? e.message : 'Failed to add member')
    }
  }

  function handleClose() {
    setAdding(false)
    setInput('')
    setSuggestions([])
    setAddError(null)
  }

  return (
    <div className="flex flex-col py-2">
      {isOwner && (
        <div className="px-4 pb-2">
          {adding ? (
            <div className="flex flex-col gap-1">
              <div className="flex gap-1">
                <input
                  autoFocus
                  value={input}
                  onChange={(e) => handleInputChange(e.target.value)}
                  onKeyDown={(e) => { if (e.key === 'Escape') handleClose() }}
                  placeholder="search username"
                  className="flex-1 bg-surface-base border border-surface-border rounded px-2 py-1 text-xs font-mono text-text-primary placeholder-text-muted focus:outline-none focus:border-accent-highlight"
                />
                <button
                  onClick={handleClose}
                  className="px-2 py-1 text-xs font-mono text-text-muted hover:text-text-primary cursor-pointer"
                >
                  ✕
                </button>
              </div>
              {input.trim() && !searching && suggestions.length === 0 && (
                <div className="text-xs text-text-muted font-mono px-1">No users found</div>
              )}
              {suggestions.length > 0 && (
                <div className="flex flex-col border border-surface-border rounded overflow-hidden">
                  {suggestions.map((u) => (
                    <button
                      key={u.id}
                      onClick={() => handleSelect(u)}
                      className="px-3 py-1.5 text-xs font-mono text-text-primary text-left hover:bg-surface-elevated cursor-pointer transition-colors"
                    >
                      {u.username}
                    </button>
                  ))}
                </div>
              )}
              {addError && (
                <div className="text-xs text-accent-danger font-mono">{addError}</div>
              )}
            </div>
          ) : (
            <button
              onClick={() => { setAdding(true); setAddError(null) }}
              className="text-xs font-mono text-text-muted hover:text-accent-highlight transition-colors cursor-pointer"
            >
              + add member
            </button>
          )}
        </div>
      )}

      {online.length > 0 && (
        <>
          <div className="px-4 pt-2 pb-1 text-xs uppercase tracking-widest text-text-muted">
            Online — {online.length}
          </div>
          {online.map((u) => (
            <MemberItem key={u.id} user={u} isOnline />
          ))}
        </>
      )}
      {offline.length > 0 && (
        <>
          <div className="px-4 pt-3 pb-1 text-xs uppercase tracking-widest text-text-muted">
            Offline — {offline.length}
          </div>
          {offline.map((u) => (
            <MemberItem key={u.id} user={u} isOnline={false} />
          ))}
        </>
      )}
    </div>
  )
}
