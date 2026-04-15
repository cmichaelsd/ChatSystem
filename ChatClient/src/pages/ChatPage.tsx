import { useEffect, useState } from 'react'
import { AppShell } from '../components/layout/AppShell'
import { GroupList } from '../components/groups/GroupList'
import { CreateGroupModal } from '../components/groups/CreateGroupModal'
import { ConversationView } from '../components/chat/ConversationView'
import { MemberList } from '../components/members/MemberList'
import { Button } from '../components/ui/Button'
import { Spinner } from '../components/ui/Spinner'
import { ErrorBanner } from '../components/ui/ErrorBanner'
import { useWebSocket } from '../hooks/useWebSocket'
import { useGroupMembers } from '../hooks/useGroupMembers'
import { getGroups, getMe } from '../lib/api'
import { useAuthStore } from '../store/authStore'
import { useGroupStore } from '../store/groupStore'
import { wsManager } from '../lib/wsManager'

export function ChatPage() {
  const token = useAuthStore((s) => s.token)
  const currentUser = useAuthStore((s) => s.currentUser)
  const setCurrentUser = useAuthStore((s) => s.setCurrentUser)
  const logout = useAuthStore((s) => s.logout)

  const groups = useGroupStore((s) => s.groups)
  const activeGroupId = useGroupStore((s) => s.activeGroupId)
  const setGroups = useGroupStore((s) => s.setGroups)
  const addGroup = useGroupStore((s) => s.addGroup)
  const setActiveGroup = useGroupStore((s) => s.setActiveGroup)

  const [loadingGroups, setLoadingGroups] = useState(false)
  const [groupsError, setGroupsError] = useState<string | null>(null)
  const [showCreateModal, setShowCreateModal] = useState(false)
  const [memberRefreshKey, setMemberRefreshKey] = useState(0)

  useWebSocket(token)

  const { members } = useGroupMembers(activeGroupId, memberRefreshKey)

  useEffect(() => {
    if (!currentUser) {
      getMe().then(setCurrentUser).catch(() => {})
    }
  }, [])

  useEffect(() => {
    setLoadingGroups(true)
    getGroups()
      .then(setGroups)
      .catch((e) => setGroupsError(e.message))
      .finally(() => setLoadingGroups(false))
  }, [])

  const activeGroup = groups.find((g) => g.id === activeGroupId) ?? null

  const handleLogout = () => {
    wsManager.disconnect()
    logout()
    window.location.href = '/login'
  }

  const sidebar = (
    <div className="flex flex-col h-full">
      {/* Header */}
      <div className="px-4 py-3 border-b border-surface-border">
        <div className="text-xs text-accent-highlight uppercase tracking-widest mb-0.5">
          ChatSystem
        </div>
        {currentUser && (
          <div className="text-xs text-text-muted font-mono truncate">
            {currentUser.username}
          </div>
        )}
      </div>

      {/* Groups label */}
      <div className="px-4 pt-4 pb-1 flex items-center justify-between">
        <span className="text-xs uppercase tracking-widest text-text-muted">Groups</span>
        <button
          onClick={() => setShowCreateModal(true)}
          className="text-text-muted hover:text-accent-highlight text-lg leading-none cursor-pointer transition-colors"
          title="New group"
        >
          +
        </button>
      </div>

      {/* Group list */}
      <div className="flex-1 overflow-y-auto">
        {loadingGroups && (
          <div className="flex justify-center py-4">
            <Spinner />
          </div>
        )}
        {groupsError && (
          <div className="px-4">
            <ErrorBanner message={groupsError} />
          </div>
        )}
        {!loadingGroups && (
          <GroupList
            groups={groups}
            activeGroupId={activeGroupId}
            onSelect={setActiveGroup}
          />
        )}
      </div>

      {/* Footer */}
      <div className="px-4 py-3 border-t border-surface-border">
        <Button variant="ghost" onClick={handleLogout} className="w-full text-xs">
          Disconnect
        </Button>
      </div>
    </div>
  )

  const main = (
    <div className="flex flex-1 min-w-0">
      {activeGroup ? (
        <>
          <ConversationView
            conversationId={activeGroup.id}
            groupName={activeGroup.name}
            currentUserId={currentUser?.id ?? ''}
          />
          {/* Member list panel */}
          <div className="w-52 flex-shrink-0 border-l border-surface-border bg-surface-panel overflow-y-auto">
            <div className="px-4 py-3 border-b border-surface-border">
              <span className="text-xs uppercase tracking-widest text-text-muted">
                Members
              </span>
            </div>
            <MemberList
              members={members}
              groupId={activeGroup.id}
              isOwner={activeGroup.created_by === currentUser?.id}
              onMemberAdded={() => setMemberRefreshKey((k) => k + 1)}
            />
          </div>
        </>
      ) : (
        <div className="flex-1 flex items-center justify-center text-text-muted text-sm font-mono">
          <div className="text-center">
            <div className="text-3xl mb-3 opacity-30">#</div>
            <div>Select a group to start chatting</div>
          </div>
        </div>
      )}
    </div>
  )

  return (
    <>
      <AppShell sidebar={sidebar} main={main} />
      <CreateGroupModal
        isOpen={showCreateModal}
        onClose={() => setShowCreateModal(false)}
        onCreated={addGroup}
      />
    </>
  )
}
