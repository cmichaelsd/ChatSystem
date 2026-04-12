import { useState } from 'react'
import { Modal } from '../ui/Modal'
import { Input } from '../ui/Input'
import { Button } from '../ui/Button'
import { ErrorBanner } from '../ui/ErrorBanner'
import { createGroup } from '../../lib/api'
import type { GroupResponse } from '../../types'

interface CreateGroupModalProps {
  isOpen: boolean
  onClose: () => void
  onCreated: (group: GroupResponse) => void
}

export function CreateGroupModal({ isOpen, onClose, onCreated }: CreateGroupModalProps) {
  const [name, setName] = useState('')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setLoading(true)
    setError(null)
    try {
      const group = await createGroup(name.trim())
      setName('')
      onCreated(group)
      onClose()
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to create group')
    } finally {
      setLoading(false)
    }
  }

  return (
    <Modal isOpen={isOpen} onClose={onClose} title="New Group">
      <form onSubmit={handleSubmit} className="flex flex-col gap-4">
        {error && <ErrorBanner message={error} />}
        <Input
          id="group-name"
          label="Group Name"
          type="text"
          value={name}
          onChange={(e) => setName(e.target.value)}
          placeholder="my-group"
          required
          autoFocus
        />
        <div className="flex gap-2 justify-end">
          <Button type="button" variant="ghost" onClick={onClose}>
            Cancel
          </Button>
          <Button type="submit" loading={loading} disabled={!name.trim()}>
            Create
          </Button>
        </div>
      </form>
    </Modal>
  )
}
