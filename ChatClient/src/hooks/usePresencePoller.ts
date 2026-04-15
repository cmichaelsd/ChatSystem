import { useEffect } from 'react'
import { batchPresence } from '../lib/api'
import { usePresenceStore } from '../store/presenceStore'

export function usePresencePoller(memberIds: string[]) {
  const setPresence = usePresenceStore((s) => s.setPresence)

  useEffect(() => {
    if (memberIds.length === 0) return

    batchPresence(memberIds)
      .then((res) => setPresence(res.presence))
      .catch(() => {/* silently ignore presence errors */})
  }, [memberIds.join(',')])
}
