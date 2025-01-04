import request from '@/utils/request'

// API 路径常量
const API = {
  CREATE_ROOM: '/room/create',
  JOIN_ROOM: '/room/join',
  LEAVE_ROOM: '/room/leave',
  ROOM_INFO: '/room/info',
  ONLINE_USERS: '/room/users',
  MESSAGES: '/room/messages',
  SEND_MESSAGE: '/room/message'
}

export const chatApi = {
  // 创建聊天室
  createRoom: async () => {
    return request({
      url: API.CREATE_ROOM,
      method: 'post'
    })
  },

  // 加入聊天室
  joinRoom: async (roomId, username) => {
    return request({
      url: API.JOIN_ROOM,
      method: 'post',
      data: { 
        roomId,
        username
      }
    })
  },

  // 离开聊天室
  leaveRoom: async (roomId, username) => {
    return request({
      url: API.LEAVE_ROOM,
      method: 'post',
      data: { roomId, username }
    })
  },

  // 获取聊天室信息
  getRoomInfo: async (roomId) => {
    return request({
      url: API.ROOM_INFO,
      method: 'post',
      data: { roomId }
    })
  },

  // 获取在线用户列表
  getOnlineUsers: async (roomId, timeout = 30, signal) => {
    return request({
      url: API.ONLINE_USERS,
      method: 'post',
      data: { 
        roomId,
        timeout
      },
      signal
    })
  },

  // 获取聊天记录
  getMessages: async (roomId, lastMessageId = 0, timeout = 30, signal) => {
    console.log('Getting messages:', { roomId, lastMessageId });
    return request({
      url: API.MESSAGES,
      method: 'post',
      data: { 
        roomId,
        lastMessageId,
        timeout
      },
      signal
    });
  },

  // 发送消息
  sendMessage: async (roomId, message) => {
    console.log('Sending message to API:', { roomId, ...message })
    return request({
      url: API.SEND_MESSAGE,
      method: 'post',
      data: {
        roomId,
        content: message.content,
        username: message.username
      }
    })
  }
} 