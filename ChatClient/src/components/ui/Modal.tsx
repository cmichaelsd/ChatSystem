import type { ReactNode } from 'react'

interface ModalProps {
  isOpen: boolean
  onClose: () => void
  title: string
  children: ReactNode
}

export function Modal({ isOpen, onClose, title, children }: ModalProps) {
  if (!isOpen) return null

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/60"
      onClick={onClose}
    >
      <div
        className="bg-surface-elevated border border-surface-border w-full max-w-sm p-6"
        onClick={(e) => e.stopPropagation()}
      >
        <div className="flex items-center justify-between mb-5">
          <span className="text-xs uppercase tracking-widest text-text-muted">
            {title}
          </span>
          <button
            onClick={onClose}
            className="text-text-muted hover:text-text-primary text-lg leading-none cursor-pointer"
          >
            ×
          </button>
        </div>
        {children}
      </div>
    </div>
  )
}
