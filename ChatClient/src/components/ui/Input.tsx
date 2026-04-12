import type { InputHTMLAttributes } from 'react'

interface InputProps extends InputHTMLAttributes<HTMLInputElement> {
  label?: string
}

export function Input({ label, className = '', id, ...props }: InputProps) {
  return (
    <div className="flex flex-col gap-1">
      {label && (
        <label
          htmlFor={id}
          className="text-xs text-text-muted uppercase tracking-widest"
        >
          {label}
        </label>
      )}
      <input
        id={id}
        className={`
          font-mono text-sm bg-surface-base text-text-primary
          border border-surface-border px-3 py-2
          focus:outline-none focus:border-accent-highlight
          placeholder:text-text-muted transition-colors
          ${className}
        `}
        {...props}
      />
    </div>
  )
}
