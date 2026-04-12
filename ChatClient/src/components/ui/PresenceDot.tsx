interface PresenceDotProps {
  isOnline: boolean
}

export function PresenceDot({ isOnline }: PresenceDotProps) {
  return (
    <span
      className={`inline-block w-2 h-2 rounded-full flex-shrink-0 ${
        isOnline ? 'bg-accent-online' : 'bg-surface-border'
      }`}
      title={isOnline ? 'Online' : 'Offline'}
    />
  )
}
