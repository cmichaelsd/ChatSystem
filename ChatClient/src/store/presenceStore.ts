import { create } from 'zustand'

interface PresenceState {
  presence: Record<string, boolean>
  setPresence: (presence: Record<string, boolean>) => void
  setUserPresence: (userId: string, online: boolean) => void
}

export const usePresenceStore = create<PresenceState>((set) => ({
  presence: {},
  setPresence: (presence) => set({ presence }),
  setUserPresence: (userId, online) =>
    set((state) => ({ presence: { ...state.presence, [userId]: online } })),
}))
