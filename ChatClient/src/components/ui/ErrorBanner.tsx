interface ErrorBannerProps {
  message: string
}

export function ErrorBanner({ message }: ErrorBannerProps) {
  return (
    <div className="border border-accent-danger/40 bg-accent-danger/10 text-accent-danger text-xs px-3 py-2 font-mono">
      {message}
    </div>
  )
}
