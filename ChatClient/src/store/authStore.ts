import { create } from 'zustand'
import type { UserResponse } from '../types'

interface AuthState {
  token: string | null
  currentUser: UserResponse | null
  setToken: (token: string) => void
  setCurrentUser: (user: UserResponse) => void
  logout: () => void
}

export const useAuthStore = create<AuthState>((set) => ({
  token: localStorage.getItem('jwt'),
  currentUser: null,

  setToken: (token) => {
    localStorage.setItem('jwt', token)
    set({ token })
  },

  setCurrentUser: (user) => set({ currentUser: user }),

  logout: () => {
    localStorage.removeItem('jwt')
    set({ token: null, currentUser: null })
  },
}))
