import { useEffect } from 'react'
import { batchPresence } from '../lib/api'
import { usePresenceStore } from '../store/presenceStore'
import { PRESENCE_POLL_INTERVAL_MS } from '../lib/constants'

export function usePresencePoller(memberIds: string[]) {
  const setPresence = usePresenceStore((s) => s.setPresence)

  useEffect(() => {
    if (memberIds.length === 0) return

    const poll = () => {
      batchPresence(memberIds)
        .then((res) => setPresence(res.presence))
        .catch(() => {/* silently ignore presence errors */})
    }

    poll()
    const interval = setInterval(poll, PRESENCE_POLL_INTERVAL_MS)
    return () => clearInterval(interval)
  }, [memberIds.join(',')])
}
