import type { ButtonHTMLAttributes } from 'react'

interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: 'primary' | 'ghost' | 'danger'
  loading?: boolean
}

const variants = {
  primary:
    'bg-accent-highlight text-surface-base hover:brightness-110 disabled:opacity-50',
  ghost:
    'bg-transparent text-text-muted hover:text-text-primary hover:bg-surface-elevated disabled:opacity-50',
  danger:
    'bg-transparent text-accent-danger hover:bg-accent-danger/10 disabled:opacity-50',
}

export function Button({
  variant = 'primary',
  loading = false,
  className = '',
  children,
  disabled,
  ...props
}: ButtonProps) {
  return (
    <button
      className={`
        font-mono text-sm px-3 py-1.5 transition-all duration-150 cursor-pointer
        border border-surface-border
        ${variants[variant]}
        ${className}
      `}
      disabled={disabled || loading}
      {...props}
    >
      {loading ? '...' : children}
    </button>
  )
}
