<template>
  <header class="chat-header">
    <div class="left">
      <slot name="left"></slot>
    </div>
    <div class="center">
      <slot name="default"></slot>
    </div>
    <div class="right">
      <ThemeToggle />
      <slot name="right"></slot>
    </div>
  </header>
</template>

<script setup>
import ThemeToggle from './ThemeToggle.vue'
</script>

<style scoped>
.chat-header {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  height: 60px;
  background: var(--bg-header);
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 20px;
  z-index: 100;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

.left, .right {
  display: flex;
  align-items: center;
  gap: 10px;
}

.center {
  flex: 1;
  text-align: center;
  font-weight: 500;
}

@media (max-width: 768px) {
  .chat-header {
    padding: 0 10px;
    height: 50px;
  }
}
</style> 