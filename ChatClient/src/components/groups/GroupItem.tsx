import type { GroupResponse } from '../../types'

interface GroupItemProps {
  group: GroupResponse
  isActive: boolean
  onClick: () => void
}

export function GroupItem({ group, isActive, onClick }: GroupItemProps) {
  return (
    <button
      onClick={onClick}
      className={`
        w-full text-left px-4 py-2.5 text-sm font-mono transition-colors duration-100 cursor-pointer
        border-l-2 hover:bg-surface-elevated
        ${isActive
          ? 'border-accent-highlight text-text-primary bg-surface-elevated'
          : 'border-transparent text-text-muted hover:text-text-primary'
        }
      `}
    >
      <span className="text-text-muted mr-1">#</span>
      {group.name}
    </button>
  )
}
