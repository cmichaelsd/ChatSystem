import { useState } from 'react'
import { Input } from '../ui/Input'
import { Button } from '../ui/Button'
import { ErrorBanner } from '../ui/ErrorBanner'
import { login, getMe } from '../../lib/api'
import { useAuthStore } from '../../store/authStore'
import { useNavigate } from 'react-router-dom'

export function LoginForm() {
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const setToken = useAuthStore((s) => s.setToken)
  const setCurrentUser = useAuthStore((s) => s.setCurrentUser)
  const navigate = useNavigate()

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setLoading(true)
    setError(null)
    try {
      const token = await login(username, password)
      setToken(token.access_token)
      const user = await getMe()
      setCurrentUser(user)
      navigate('/chat')
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Login failed')
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
        id="password"
        label="Password"
        type="password"
        value={password}
        onChange={(e) => setPassword(e.target.value)}
        placeholder="••••••••"
        required
      />
      <Button type="submit" loading={loading} className="mt-2 w-full justify-center">
        Connect
      </Button>
    </form>
  )
}
