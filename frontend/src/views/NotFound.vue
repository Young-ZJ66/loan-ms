<template>
  <div class="not-found">
    <div class="content">
      <h1>404</h1>
      <p>抱歉，您访问的页面不存在</p>
      <el-button type="primary" @click="goHome">返回首页</el-button>
    </div>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'
import { useUserStore } from '../stores/user'

const router = useRouter()
const userStore = useUserStore()

const goHome = () => {
  if (userStore.isAdmin) {
    router.push('/admin/dashboard')
  } else {
    router.push('/client/dashboard')
  }
}
</script>

<style scoped>
.not-found {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background: var(--bg-primary);
}
.content {
  text-align: center;
}
.content h1 {
  font-size: 96px;
  color: var(--primary-color);
  margin-bottom: 16px;
}
.content p {
  font-size: 18px;
  color: var(--text-secondary);
  margin-bottom: 24px;
}
</style>
