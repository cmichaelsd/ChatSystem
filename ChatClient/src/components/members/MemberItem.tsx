import type { UserResponse } from '../../types'
import { PresenceDot } from '../ui/PresenceDot'

interface MemberItemProps {
  user: UserResponse
  isOnline: boolean
}

export function MemberItem({ user, isOnline }: MemberItemProps) {
  return (
    <div className="flex items-center gap-2.5 px-4 py-1.5">
      <PresenceDot isOnline={isOnline} />
      <span
        className={`text-sm font-mono truncate ${
          isOnline ? 'text-text-primary' : 'text-text-muted'
        }`}
      >
        {user.username}
      </span>
    </div>
  )
}
