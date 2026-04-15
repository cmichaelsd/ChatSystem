import { API_BASE } from './constants'
import type {
  Token,
  UserResponse,
  GroupResponse,
  StoredMessage,
} from '../types'

function getToken(): string | null {
  return localStorage.getItem('jwt')
}

async function authedFetch(
  url: string,
  options: RequestInit = {}
): Promise<Response> {
  const token = getToken()
  const res = await fetch(url, {
    ...options,
    headers: {
      ...(options.headers ?? {}),
      Authorization: `Bearer ${token}`,
    },
  })
  if (res.status === 401) {
    localStorage.removeItem('jwt')
    window.location.href = '/login'
  }
  return res
}

export async function login(username: string, password: string): Promise<Token> {
  const form = new URLSearchParams()
  form.append('username', username)
  form.append('password', password)
  const res = await fetch(`${API_BASE}/auth/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    body: form.toString(),
  })
  if (!res.ok) throw new Error('Invalid credentials')
  return res.json()
}

export async function register(
  username: string,
  email: string,
  password: string
): Promise<UserResponse> {
  const res = await fetch(`${API_BASE}/auth/register`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username, email, password }),
  })
  if (!res.ok) {
    const err = await res.json().catch(() => ({}))
    throw new Error(err.detail ?? 'Registration failed')
  }
  return res.json()
}

export async function getMe(): Promise<UserResponse> {
  const res = await authedFetch(`${API_BASE}/users/me`)
  if (!res.ok) throw new Error('Failed to fetch current user')
  return res.json()
}

export async function getUser(userId: string): Promise<UserResponse> {
  const res = await authedFetch(`${API_BASE}/users/${userId}`)
  if (!res.ok) throw new Error('User not found')
  return res.json()
}

export async function getGroups(): Promise<GroupResponse[]> {
  const res = await authedFetch(`${API_BASE}/groups`)
  if (!res.ok) throw new Error('Failed to fetch groups')
  return res.json()
}

export async function createGroup(name: string): Promise<GroupResponse> {
  const res = await authedFetch(`${API_BASE}/groups`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ name }),
  })
  if (!res.ok) throw new Error('Failed to create group')
  return res.json()
}

export async function getGroupMembers(groupId: string): Promise<UserResponse[]> {
  const res = await authedFetch(`${API_BASE}/groups/${groupId}/members`)
  if (!res.ok) throw new Error('Failed to fetch members')
  return res.json()
}

export async function searchUsers(username: string): Promise<UserResponse[]> {
  const res = await authedFetch(`${API_BASE}/users/search?username=${encodeURIComponent(username)}`)
  if (!res.ok) throw new Error('Failed to search users')
  return res.json()
}

export async function addGroupMember(groupId: string, userId: string): Promise<void> {
  const res = await authedFetch(`${API_BASE}/groups/${groupId}/members/${userId}`, {
    method: 'POST',
  })
  if (!res.ok) {
    const err = await res.json().catch(() => ({}))
    throw new Error(err.detail ?? 'Failed to add member')
  }
}

export async function getMessages(conversationId: string): Promise<StoredMessage[]> {
  const res = await authedFetch(`${API_BASE}/conversations/${conversationId}/messages`)
  if (!res.ok) throw new Error('Failed to fetch messages')
  return res.json()
}
