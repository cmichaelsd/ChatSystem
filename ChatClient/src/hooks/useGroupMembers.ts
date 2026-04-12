import { useEffect, useState } from 'react'
import { getGroupMembers } from '../lib/api'
import { useUserCacheStore } from '../store/userCacheStore'
import type { UserResponse } from '../types'

export function useGroupMembers(groupId: string | null, refreshKey = 0) {
  const [members, setMembers] = useState<UserResponse[]>([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const addUser = useUserCacheStore((s) => s.addUser)

  useEffect(() => {
    if (!groupId) {
      setMembers([])
      return
    }
    setLoading(true)
    setError(null)
    getGroupMembers(groupId)
      .then((users) => {
        users.forEach(addUser)
        setMembers(users)
      })
      .catch((e) => setError(e.message))
      .finally(() => setLoading(false))
  }, [groupId, refreshKey])

  return { members, loading, error }
}
