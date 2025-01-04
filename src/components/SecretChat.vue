<template>
  <div class="secret-chat">
    <ChatHeader>
      <template #default>
        <template v-if="!inRoom">
          <h2>Secret Chat</h2>
        </template>
        <template v-else>
          <h3>Room: {{ roomId }} ({{ onlineUsers.size }} online)</h3>
        </template>
      </template>
      <template #left v-if="inRoom">
        <button class="icon-btn" @click="showUserList = !showUserList">
          <span class="user-count">{{ onlineUsers.size }}</span>
          <span class="user-icon">👥</span>
        </button>
      </template>
      <template #right v-if="inRoom">
        <button @click="leaveRoom" class="danger-btn">Leave</button>
      </template>
    </ChatHeader>

    <div v-if="!inRoom" class="join-options">
      <div class="buttons">
        <button @click="createRoom" class="primary-btn">Create Room</button>
        <button @click="showJoinForm = true" class="secondary-btn">Join Room</button>
      </div>
      
      <div v-if="showJoinForm" class="join-form">
        <input 
          v-model="roomId" 
          type="text" 
          placeholder="Enter Room ID"
        >
        <button @click="joinRoom" class="primary-btn">Join</button>
      </div>
    </div>

    <div v-else class="chat-room">
      <div v-if="showUserList" class="user-list">
        <div class="user-list-header">
          <h4>Online Users ({{ onlineUsers.size }})</h4>
          <button class="close-btn" @click="showUserList = false">&times;</button>
        </div>
        <div class="user-list-content">
          <div v-for="user in Array.from(onlineUsers)" :key="user" class="user-item">
            <UserAvatar :username="user" />
            <span class="user-name">{{ user }}</span>
          </div>
        </div>
      </div>

      <div class="messages" ref="messageContainer">
        <div v-for="msg in messages" :key="msg.id" 
             class="message" 
             :class="{ 'own-message': msg.user === username }">
          <UserAvatar :username="msg.user" />
          <div class="message-content">
            <div class="message-header">
              <span class="user">{{ msg.user }}</span>
              <span class="time">{{ formatTime(msg.timestamp) }}</span>
            </div>
            <div class="text">{{ msg.content }}</div>
          </div>
        </div>
      </div>

      <div class="input-area">
        <input 
          v-model="newMessage" 
          @keyup.enter="sendMessage"
          placeholder="Type a message..."
        >
        <button @click="sendMessage" class="primary-btn">Send</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { v4 as uuidv4 } from 'uuid'
import UserAvatar from './UserAvatar.vue'
import ChatHeader from './ChatHeader.vue'
import { chatApi } from '../api/chat'
import UserIdentifier from '../utils/userIdentifier'
import { useRouter } from 'vue-router'

const inRoom = ref(false)
const showJoinForm = ref(false)
const roomId = ref('')
const messages = ref([])
const newMessage = ref('')
const messageContainer = ref(null)
const username = ref('')
const onlineUsers = ref(new Set())
const showUserList = ref(false)
const loading = ref(false)
const error = ref(null)
const isPolling = ref(false)
const isUserListPolling = ref(false)
let pollingInterval = null
const router = useRouter()

const MAX_RETRY_COUNT = 3  // 最大重试次数
const retryCount = ref(0)  // 重试计数器

const createRoom = async () => {
  try {
    if (!username.value) {
      error.value = 'Username not generated yet'
      return
    }
    loading.value = true
    const response = await chatApi.createRoom()
    console.log('Response:', response)
    roomId.value = response.data.roomId
    inRoom.value = true
    await joinRoom(roomId.value)
  } catch (err) {
    error.value = err.message || 'Failed to create room'
    console.error(err)
  } finally {
    loading.value = false
  }
}

const joinRoom = async () => {
  if (roomId.value.trim() && username.value) {
    try {
      loading.value = true
      const { data } = await chatApi.joinRoom(roomId.value, username.value)
      // 立即更新在线用户列表
      if (data && data.onlineUsers) {
        onlineUsers.value = new Set(data.onlineUsers)
        console.log('Initial online users:', Array.from(onlineUsers.value))
      }
      inRoom.value = true
      showJoinForm.value = false
      startPolling()
      startUserListPolling()
      await fetchMessages()
    } catch (err) {
      console.error('Join room error:', err)
      error.value = err.message || 'Failed to join room'
      console.error(err)
    } finally {
      loading.value = false
    }
  } else {
    error.value = username.value ? 'Please enter room ID' : 'Username not generated yet'
  }
}

const leaveRoom = async () => {
  try {
    if (inRoom.value) {
      console.log('Leaving room:', roomId.value, username.value)
      await chatApi.leaveRoom(roomId.value, username.value)
    }
  } catch (err) {
    console.error('Leave room error:', err)
    console.error(err)
  } finally {
    inRoom.value = false
    roomId.value = ''
    messages.value = []
    onlineUsers.value.clear()
    showUserList.value = false
    stopPolling()
    stopUserListPolling()
    router.push('/')
  }
}

const sendMessage = async () => {
  if (newMessage.value.trim()) {
    try {
      loading.value = true
      console.log('Sending message:', {
        content: newMessage.value,
        username: username.value,
        roomId: roomId.value
      })
      await chatApi.sendMessage(roomId.value, {
        content: newMessage.value,
        username: username.value,
        roomId: roomId.value
      })
      newMessage.value = ''
      scrollToBottom()
    } catch (err) {
      console.error('Send message error:', err)
      error.value = 'Failed to send message'
      console.error(err)
    } finally {
      loading.value = false
    }
  }
}

const fetchMessages = async () => {
  if (isPolling.value) {
    console.log('Previous polling still in progress, skipping...')
    return
  }
  try {
    isPolling.value = true
    const lastMsg = messages.value[messages.value.length - 1]
    const lastMessageId = lastMsg ? lastMsg.id : 0
    const controller = new AbortController()
    const timeoutId = setTimeout(() => controller.abort(), 60000)

    try {
      const { data } = await chatApi.getMessages(roomId.value, lastMessageId, 30, controller.signal)
      retryCount.value = 0  // 成功后重置重试计数
      if (data && data.messages && Array.isArray(data.messages)) {
        const newMessages = data.messages.map(msg => ({
          id: msg.id,
          user: msg.username,
          content: msg.content,
          timestamp: msg.createdAt
        }))
        if (lastMessageId === 0) {
          messages.value = newMessages
        } else {
          messages.value = [...messages.value, ...newMessages]
        }
        console.log('Processed messages:', messages.value)
        if (newMessages.length > 0) {
          scrollToBottom()
        }
      }
    } catch (err) {
      if (err.name === 'AbortError') {
        console.log('Polling timeout, restarting...')
      } else {
        // 检查是否是严重错误
        if (err.response && (err.response.status === 404 || err.response.status === 500)) {
          retryCount.value++
          console.error(`Server error (${err.response.status}), retry count: ${retryCount.value}`)
          if (retryCount.value >= MAX_RETRY_COUNT) {
            console.error('Max retry count reached, stopping polling')
            error.value = 'Server error, please try again later'
            stopPolling()
            return
          }
        }
        throw err
      }
    } finally {
      clearTimeout(timeoutId)
    }
  } catch (err) {
    error.value = err.message || 'Failed to fetch messages'
    console.error('Fetch messages error:', err)
  } finally {
    isPolling.value = false
    if (inRoom.value) {
      // 只有在重试次数未达到最大值时继续轮询
      if (retryCount.value < MAX_RETRY_COUNT) {
        startPolling()
      }
    }
  }
}

const userListRetryCount = ref(0)  // 用户列表重试计数器

const fetchOnlineUsers = async () => {
  if (isUserListPolling.value) {
    console.log('Previous user list polling still in progress, skipping...')
    return
  }
  try {
    isUserListPolling.value = true
    const controller = new AbortController()
    const timeoutId = setTimeout(() => controller.abort(), 60000)

    try {
      const { data } = await chatApi.getOnlineUsers(roomId.value, 30, controller.signal)
      userListRetryCount.value = 0  // 成功后重置重试计数
      console.log('Online users response:', data)
      if (data && data.onlineUsers && Array.isArray(data.onlineUsers)) {
        onlineUsers.value = new Set(data.onlineUsers)
        console.log('Updated online users:', Array.from(onlineUsers.value))
      }
    } catch (err) {
      if (err.name === 'AbortError') {
        console.log('User list polling timeout, restarting...')
      } else {
        // 检查是否是严重错误
        if (err.response && (err.response.status === 404 || err.response.status === 500)) {
          userListRetryCount.value++
          console.error(`Server error (${err.response.status}), retry count: ${userListRetryCount.value}`)
          if (userListRetryCount.value >= MAX_RETRY_COUNT) {
            console.error('Max retry count reached, stopping user list polling')
            error.value = 'Server error, please try again later'
            stopUserListPolling()
            return
          }
        }
        throw err
      }
    } finally {
      clearTimeout(timeoutId)
    }
  } catch (err) {
    error.value = err.message || 'Failed to fetch user list'
    console.error(err)
  } finally {
    isUserListPolling.value = false
    if (inRoom.value) {
      // 只有在重试次数未达到最大值时继续轮询
      if (userListRetryCount.value < MAX_RETRY_COUNT) {
        startUserListPolling()
      }
    }
  }
}

const startPolling = () => {
  if (!inRoom.value) return
  // 重置重试计数
  retryCount.value = 0
  fetchMessages().catch(console.error)
}

const stopPolling = () => {
  isPolling.value = false
  retryCount.value = 0
}

const scrollToBottom = () => {
  setTimeout(() => {
    if (messageContainer.value) {
      messageContainer.value.scrollTop = messageContainer.value.scrollHeight
    }
  }, 100)
}

const formatTime = (dateStr) => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  return date.toLocaleTimeString('zh-CN', {
    hour: '2-digit',
    minute: '2-digit'
  })
}

const startUserListPolling = () => {
  if (!inRoom.value) return
  // 重置重试计数
  userListRetryCount.value = 0
  fetchOnlineUsers().catch(console.error)
}

const stopUserListPolling = () => {
  isUserListPolling.value = false
  userListRetryCount.value = 0
}

onMounted(async () => {
  try {
    username.value = await UserIdentifier.generateUsername()
    console.log('Generated username:', username.value)
  } catch (err) {
    console.error('Failed to generate username:', err)
    // Fallback to random username
    username.value = `User${Math.floor(Math.random() * 1000)}`
    console.log('Using fallback username:', username.value)
  }
})

onUnmounted(() => {
  stopPolling()
  stopUserListPolling()
  if (inRoom.value) {
    leaveRoom()
  }
})

window.addEventListener('beforeunload', () => {
  if (inRoom.value) {
    chatApi.leaveRoom(roomId.value, username.value).catch(console.error)
  }
})
</script>

<style scoped>
.secret-chat {
  max-width: 100%;
  margin: 0 auto;
  height: 100vh;
  width: 100vw;
  display: flex;
  flex-direction: column;
  position: relative;
  overflow: hidden;
  padding-top: 60px;
}

.join-options {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
  text-align: center;
  padding: 2rem 1rem;
  background: var(--bg-secondary);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  margin: auto;
  width: 100%;
  height: 100%;
}

.buttons {
  margin: 20px 0;
  display: flex;
  gap: 10px;
  justify-content: center;
}

button {
  padding: 10px 20px;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-weight: 500;
  transition: all 0.3s ease;
}

.primary-btn {
  background-color: var(--primary-color);
  color: white;
}

.secondary-btn {
  background-color: var(--secondary-color);
  color: var(--text-color);
}

.danger-btn {
  background-color: var(--danger-color);
  color: white;
}

button:hover {
  opacity: 0.9;
  transform: translateY(-1px);
}

.join-form {
  margin-top: 20px;
  display: flex;
  gap: 10px;
  justify-content: center;
}

input {
  padding: 10px;
  border: 1px solid var(--border-color);
  border-radius: 6px;
  background: var(--bg-input);
  color: var(--text-color);
}

.chat-room {
  display: flex;
  flex-direction: column;
  height: calc(100vh - 60px);
  background: var(--bg-secondary);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.messages {
  flex-grow: 1;
  overflow-y: auto;
  padding: 20px 15px;
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.message {
  display: flex;
  gap: 10px;
  align-items: flex-start;
  max-width: 80%;
  animation: fadeIn 0.3s ease;
}

.message.own-message {
  margin-left: auto;
  flex-direction: row-reverse;
}

.message-content {
  background: var(--bg-message);
  padding: 10px;
  border-radius: 8px;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.1);
  word-break: break-word;
  max-width: calc(100% - 50px);
}

.message-header {
  display: flex;
  gap: 8px;
  margin-bottom: 4px;
  font-size: 0.9em;
}

.time {
  color: var(--text-secondary);
  font-size: 0.8em;
}

.input-area {
  padding: 10px;
  display: flex;
  gap: 10px;
  background: var(--bg-header);
  border-radius: 0 0 8px 8px;
  height: 60px;
}

.input-area input {
  flex-grow: 1;
  min-width: 0;
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* 响应式设计 */
@media (max-width: 768px) {
  .secret-chat {
    padding: 0;
    padding-top: 50px;
  }

  .chat-room {
    height: calc(100vh - 50px);
  }

  .message {
    max-width: 90%;
  }

  .join-options {
    padding: 1rem 10px;
    margin: 0;
  }

  .messages {
    padding: 15px 10px;
  }

  .input-area {
    padding: 10px;
  }
}

@media (max-width: 480px) {
  .buttons {
    flex-direction: column;
    align-items: stretch;
  }

  button {
    margin: 5px 0;
  }
}

/* 深色模式支持 */
:root {
  --primary-color: #4CAF50;
  --secondary-color: #e9ecef;
  --danger-color: #dc3545;
  --bg-primary: #ffffff;
  --bg-secondary: #ffffff;
  --bg-header: #f8f9fa;
  --bg-message: #e9ecef;
  --bg-input: #ffffff;
  --text-color: #212529;
  --text-secondary: #6c757d;
  --border-color: #dee2e6;
  --bg-hover: rgba(0, 0, 0, 0.1);
}

@media (prefers-color-scheme: dark) {
  :root {
    --primary-color: #4CAF50;
    --secondary-color: #2d3436;
    --danger-color: #dc3545;
    --bg-primary: #1a1a1a;
    --bg-secondary: #2d3436;
    --bg-header: #232931;
    --bg-message: #34495e;
    --bg-input: #2d3436;
    --text-color: #f8f9fa;
    --text-secondary: #adb5bd;
    --border-color: #4a5568;
    --bg-hover: rgba(255, 255, 255, 0.1);
  }

  body {
    background-color: var(--bg-primary);
    color: var(--text-color);
  }

  input {
    color: var(--text-color);
  }
}

.user-list {
  position: absolute;
  left: 0;
  top: 60px;
  bottom: 0;
  width: 250px;
  background: var(--bg-secondary);
  border-right: 1px solid var(--border-color);
  z-index: 90;
  display: flex;
  flex-direction: column;
  animation: slideIn 0.3s ease;
}

.user-list-header {
  padding: 15px;
  border-bottom: 1px solid var(--border-color);
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.user-list-content {
  flex: 1;
  overflow-y: auto;
  padding: 10px;
}

.user-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px;
  border-radius: 4px;
}

.user-item:hover {
  background: var(--bg-hover);
}

.close-btn {
  background: none;
  border: none;
  font-size: 1.5em;
  padding: 0 5px;
  cursor: pointer;
  color: var(--text-color);
}

.icon-btn {
  background: none;
  border: none;
  position: relative;
  padding: 8px;
  cursor: pointer;
  color: var(--text-color);
}

.user-count {
  position: absolute;
  top: 0;
  right: 0;
  background: var(--primary-color);
  color: white;
  border-radius: 50%;
  padding: 2px 6px;
  font-size: 0.8em;
}

.user-icon {
  font-size: 1.2em;
}

@keyframes slideIn {
  from {
    transform: translateX(-100%);
  }
  to {
    transform: translateX(0);
  }
}

@media (max-width: 768px) {
  .user-list {
    top: 50px;
    width: 200px;
  }
}
</style> 