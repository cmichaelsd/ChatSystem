import { create } from 'zustand'
import type { UserResponse } from '../types'

interface UserCacheState {
  cache: Record<string, UserResponse>
  addUser: (user: UserResponse) => void
  getUser: (id: string) => UserResponse | undefined
}

export const useUserCacheStore = create<UserCacheState>((set, get) => ({
  cache: {},
  addUser: (user) =>
    set((s) => ({ cache: { ...s.cache, [user.id]: user } })),
  getUser: (id) => get().cache[id],
}))
