import { useState } from 'react'
import { Input } from '../ui/Input'
import { Button } from '../ui/Button'
import { ErrorBanner } from '../ui/ErrorBanner'
import { register } from '../../lib/api'
import { useNavigate } from 'react-router-dom'

export function RegisterForm() {
  const [username, setUsername] = useState('')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const navigate = useNavigate()

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setLoading(true)
    setError(null)
    try {
      await register(username, email, password)
      navigate('/login')
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Registration failed')
    } finally {
      setLoading(false)
    }
  }

  return (
    <form onSubmit={handleSubmit} className="flex flex-col gap-4">
      {error && <ErrorBanner message={error} />}
      <Input
        id="username"
        label="Username"
        type="text"
        value={username}
        onChange={(e) => setUsername(e.target.value)}
        placeholder="your_handle"
        required
        autoFocus
      />
      <Input
        id="email"
        label="Email"
        type="email"
        value={email}
        onChange={(e) => setEmail(e.target.value)}
        placeholder="you@example.com"
        required
      />
      <Input
        id="password"
        label="Password"
        type="password"
        value={password}
        onChange={(e) => setPassword(e.target.value)}
        placeholder="••••••••"
        required
      />
      <Button type="submit" loading={loading} className="mt-2 w-full justify-center">
        Register
      </Button>
    </form>
  )
}
