export function Spinner({ className = '' }: { className?: string }) {
  return (
    <div
      className={`w-4 h-4 border border-text-muted border-t-accent-highlight rounded-full animate-spin ${className}`}
    />
  )
}
