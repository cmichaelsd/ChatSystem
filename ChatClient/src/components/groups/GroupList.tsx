import type { GroupResponse } from '../../types'
import { GroupItem } from './GroupItem'

interface GroupListProps {
  groups: GroupResponse[]
  activeGroupId: string | null
  onSelect: (id: string) => void
}

export function GroupList({ groups, activeGroupId, onSelect }: GroupListProps) {
  if (groups.length === 0) {
    return (
      <div className="px-4 py-3 text-xs text-text-muted">
        No groups yet.
      </div>
    )
  }

  return (
    <div className="flex flex-col">
      {groups.map((group) => (
        <GroupItem
          key={group.id}
          group={group}
          isActive={group.id === activeGroupId}
          onClick={() => onSelect(group.id)}
        />
      ))}
    </div>
  )
}
