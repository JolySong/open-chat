<template>
  <div class="avatar" :style="{ backgroundColor: avatarColor }">
    {{ initials }}
  </div>
</template>

<script>
import { computed } from 'vue'

export default {
  name: 'UserAvatar',
  props: {
    username: {
      type: String,
      required: true
    }
  },
  setup(props) {
    const colors = [
      '#1abc9c', '#2ecc71', '#3498db', '#9b59b6', 
      '#34495e', '#16a085', '#27ae60', '#2980b9', 
      '#8e44ad', '#2c3e50', '#f1c40f', '#e67e22', 
      '#e74c3c', '#95a5a6', '#f39c12', '#d35400'
    ]

    const initials = computed(() => {
      return props.username.charAt(0).toUpperCase()
    })

    const avatarColor = computed(() => {
      const index = Math.abs(props.username.split('').reduce((acc, char) => {
        return acc + char.charCodeAt(0)
      }, 0) % colors.length)
      return colors[index]
    })

    return {
      initials,
      avatarColor
    }
  }
}
</script>

<style scoped>
.avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-weight: bold;
  font-size: 1.2em;
}
</style> 