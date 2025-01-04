<template>
  <div class="app">
    <router-view></router-view>
  </div>
</template>

<script>
import { onMounted } from 'vue'
import { useRouter } from 'vue-router'

export default {
  name: 'App',
  setup() {
    const router = useRouter()

    onMounted(() => {
      // 如果当前在根路径，自动重定向到聊天页面
      if (router.currentRoute.value.path === '/') {
        router.push('/chat')
      }
    })
  }
}
</script>

<style>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

html, body {
  height: 100vh;
  width: 100vw;
  overflow: hidden;
}

body {
  font-family: Arial, sans-serif;
  background-color: #f5f5f5;
  color: #333;
}

.app {
  height: 100%;
  width: 100%;
  overflow: hidden;
}

html[data-theme='dark'] {
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

html[data-theme='light'] {
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

body {
  background-color: var(--bg-primary);
  color: var(--text-color);
  transition: background-color 0.3s, color 0.3s;
}
</style>
