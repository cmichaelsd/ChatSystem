import { Link } from 'react-router-dom'
import { LoginForm } from '../components/auth/LoginForm'

export function LoginPage() {
  return (
    <div className="min-h-screen bg-surface-base flex items-center justify-center px-4">
      <div className="w-full max-w-sm">
        {/* Logo / title */}
        <div className="mb-8">
          <div className="text-accent-highlight font-mono text-xs uppercase tracking-widest mb-2">
            ChatSystem
          </div>
          <h1 className="text-2xl font-mono text-text-primary font-light">
            Sign in
          </h1>
          <p className="text-sm text-text-muted mt-1 font-mono">
            Connect to the grid.
          </p>
        </div>

        <div className="border border-surface-border bg-surface-panel p-6">
          <LoginForm />
        </div>

        <p className="mt-4 text-center text-xs text-text-muted font-mono">
          No account?{' '}
          <Link to="/register" className="text-accent-highlight hover:underline">
            Register
          </Link>
        </p>
      </div>
    </div>
  )
}
