import { create } from 'zustand'
import type { GroupResponse } from '../types'

interface GroupState {
  groups: GroupResponse[]
  activeGroupId: string | null
  setGroups: (groups: GroupResponse[]) => void
  addGroup: (group: GroupResponse) => void
  setActiveGroup: (id: string | null) => void
}

export const useGroupStore = create<GroupState>((set) => ({
  groups: [],
  activeGroupId: null,
  setGroups: (groups) => set({ groups }),
  addGroup: (group) => set((s) => ({
    groups: s.groups.some((g) => g.id === group.id) ? s.groups : [...s.groups, group],
  })),
  setActiveGroup: (id) => set({ activeGroupId: id }),
}))
