import { useEffect } from 'react'
import { wsManager } from '../lib/wsManager'

export function useWebSocket(token: string | null) {
  useEffect(() => {
    if (!token) return
    wsManager.connect(token)
    return () => {
      wsManager.disconnect()
    }
  }, [token])
}
