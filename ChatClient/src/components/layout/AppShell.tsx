import type { ReactNode } from 'react'

interface AppShellProps {
  sidebar: ReactNode
  main: ReactNode
}

export function AppShell({ sidebar, main }: AppShellProps) {
  return (
    <div className="flex h-screen bg-surface-base overflow-hidden">
      <aside className="w-60 flex-shrink-0 border-r border-surface-border flex flex-col bg-surface-panel">
        {sidebar}
      </aside>
      <main className="flex flex-1 min-w-0">
        {main}
      </main>
    </div>
  )
}
