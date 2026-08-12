import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('token') || '')
  const role = ref(localStorage.getItem('role') !== null ? parseInt(localStorage.getItem('role')) : null)
  const username = ref(localStorage.getItem('username') || '')

  const isLoggedIn = computed(() => !!token.value)
  const isAdmin = computed(() => role.value === 1)
  const isClient = computed(() => role.value === 0)

  const setAuth = (newToken, newRole, newUsername) => {
    token.value = newToken
    role.value = newRole
    username.value = newUsername
    localStorage.setItem('token', newToken)
    localStorage.setItem('role', String(newRole))
    localStorage.setItem('username', newUsername)
  }

  const setUsername = (name) => {
    username.value = name
    if (name) {
      localStorage.setItem('username', name)
    }
  }

  const logout = () => {
    token.value = ''
    role.value = null
    username.value = ''
    localStorage.removeItem('token')
    localStorage.removeItem('role')
    localStorage.removeItem('username')
  }

  return {
    token,
    role,
    username,
    isLoggedIn,
    isAdmin,
    isClient,
    setAuth,
    setUsername,
    logout
  }
})
