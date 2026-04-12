import { Link } from 'react-router-dom'
import { RegisterForm } from '../components/auth/RegisterForm'

export function RegisterPage() {
  return (
    <div className="min-h-screen bg-surface-base flex items-center justify-center px-4">
      <div className="w-full max-w-sm">
        <div className="mb-8">
          <div className="text-accent-highlight font-mono text-xs uppercase tracking-widest mb-2">
            ChatSystem
          </div>
          <h1 className="text-2xl font-mono text-text-primary font-light">
            Create account
          </h1>
          <p className="text-sm text-text-muted mt-1 font-mono">
            Join the network.
          </p>
        </div>

        <div className="border border-surface-border bg-surface-panel p-6">
          <RegisterForm />
        </div>

        <p className="mt-4 text-center text-xs text-text-muted font-mono">
          Already registered?{' '}
          <Link to="/login" className="text-accent-highlight hover:underline">
            Sign in
          </Link>
        </p>
      </div>
    </div>
  )
}
