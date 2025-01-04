import { createRouter, createWebHistory } from 'vue-router'
import SecretChat from '../components/SecretChat.vue'

const routes = [
  {
    path: '/chat',
    name: 'SecretChat',
    component: SecretChat
  },
  {
    path: '/',
    name: 'Home',
    redirect: '/chat'
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router 