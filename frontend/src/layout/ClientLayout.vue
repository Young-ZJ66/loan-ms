<template>
  <el-container class="layout-container">
    <el-header class="top-header">
      <div class="logo">贷款申请平台</div>
      <div class="user-profile">
        <el-dropdown trigger="click" placement="bottom-end">
          <span class="username-btn">
            <el-icon style="margin-right:4px;"><UserFilled /></el-icon>
            {{ username }}
            <el-icon class="el-icon--right"><ArrowDown /></el-icon>
            <span v-if="unreadCount > 0" class="header-dot"></span>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item @click="router.push('/client/messages')">
                <el-icon><Bell /></el-icon>
                消息中心
                <el-badge :value="unreadCount" :hidden="unreadCount === 0" style="margin-left:8px" />
              </el-dropdown-item>
              <el-dropdown-item divided @click="openPwdDialog">
                <el-icon><Key /></el-icon>
                修改密码
              </el-dropdown-item>
              <el-dropdown-item divided @click="logout" style="color:#f56c6c;">
                <el-icon><SwitchButton /></el-icon>
                安全退出
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </el-header>

    <el-container class="main-body">
      <el-aside width="220px" class="side-nav">
        <el-menu :default-active="activeMenu" class="el-menu-vertical" router background-color="transparent" text-color="var(--text-primary)" active-text-color="var(--primary-color)">
          <div class="menu-title">金融服务</div>
          <el-menu-item index="/client/dashboard"><el-icon><Odometer /></el-icon><span>我的额度</span></el-menu-item>
          <el-menu-item index="/client/kyc"><el-icon><User /></el-icon><span>实名认证</span></el-menu-item>
          <el-menu-item index="/client/apply"><el-icon><DocumentAdd /></el-icon><span>申请贷款</span></el-menu-item>
          <el-menu-item index="/client/bills"><el-icon><Wallet /></el-icon><span>账单与还款</span></el-menu-item>
          <el-menu-item index="/client/messages">
            <el-icon><Bell /></el-icon>
            <span>消息中心</span>
            <span v-if="unreadCount > 0" class="menu-custom-badge">{{ unreadCount }}</span>
          </el-menu-item>
        </el-menu>
      </el-aside>
      <el-main class="content-area">
        <router-view v-slot="{ Component }">
          <transition name="fade" mode="out-in"><component :is="Component" /></transition>
        </router-view>
      </el-main>
    </el-container>

    <!-- 修改密码弹窗 -->
    <el-dialog v-model="pwdVisible" title="修改登录密码" width="400px" custom-class="dark-dialog" append-to-body>
      <el-form label-position="top">
        <el-form-item label="原密码" required>
          <el-input v-model="pwdForm.oldPassword" type="password" show-password placeholder="请输入当前密码" />
        </el-form-item>
        <el-form-item label="新密码" required>
          <el-input v-model="pwdForm.newPassword" type="password" show-password placeholder="请输入新密码" />
        </el-form-item>
        <el-form-item label="确认新密码" required>
          <el-input v-model="pwdForm.confirmPassword" type="password" show-password placeholder="请再次输入新密码" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="pwdVisible = false">取消</el-button>
        <el-button type="primary" @click="submitPwd" :loading="pwdLoading">确认修改</el-button>
      </template>
    </el-dialog>
  </el-container>
</template>

<script setup>
import { useRouter, useRoute } from 'vue-router'
import { computed, ref, onMounted, onUnmounted } from 'vue'
import { Odometer, User, DocumentAdd, Wallet, Bell, UserFilled, ArrowDown, SwitchButton, Key } from '@element-plus/icons-vue'
import request from '../utils/request'
import { ElMessage } from 'element-plus'

const router = useRouter()
const route = useRoute()
const activeMenu = computed(() => route.path)
const unreadCount = ref(0)

// 初始用账号名兜底，后续异步换成真实姓名
const getAccountName = () => {
  const stored = localStorage.getItem('username')
  if (stored) return stored
  try {
    const token = localStorage.getItem('token')
    if (token) {
      const payload = JSON.parse(atob(token.split('.')[1]))
      const name = payload.sub || payload.username || ''
      if (name) localStorage.setItem('username', name)
      return name
    }
  } catch {}
  return '用户'
}
const username = ref(getAccountName())

const fetchUnreadCount = async () => {
  try {
    const res = await request.get('/message/unread-count')
    unreadCount.value = res.data || 0
  } catch {}
}

const pwdVisible = ref(false)
const pwdLoading = ref(false)
const pwdForm = ref({ oldPassword: '', newPassword: '', confirmPassword: '' })

const openPwdDialog = () => {
  pwdForm.value = { oldPassword: '', newPassword: '', confirmPassword: '' }
  pwdVisible.value = true
}

const submitPwd = async () => {
  if (!pwdForm.value.oldPassword || !pwdForm.value.newPassword) {
    ElMessage.error('密码不能为空')
    return
  }
  if (pwdForm.value.newPassword !== pwdForm.value.confirmPassword) {
    ElMessage.error('两次输入的新密码不一致')
    return
  }
  pwdLoading.value = true
  try {
    await request.post('/auth/change-password', {
      oldPassword: pwdForm.value.oldPassword,
      newPassword: pwdForm.value.newPassword
    })
    ElMessage.success('密码修改成功，请重新登录！')
    pwdVisible.value = false
    logout()
  } finally {
    pwdLoading.value = false
  }
}

const logout = () => {
  localStorage.clear()
  router.push('/login')
}

onMounted(() => {
  fetchUnreadCount()
  // 每分钟轮询一次未读数
  const timer = setInterval(fetchUnreadCount, 60000)
  // 监听消息已读事件实时刷新红点
  window.addEventListener('unread-changed', fetchUnreadCount)
  // 组件卸载时清理定时器和事件监听
  onUnmounted(() => {
    clearInterval(timer)
    window.removeEventListener('unread-changed', fetchUnreadCount)
  })
  // 异步查询 KYC 实名姓名：若已通过认证则用真实姓名展示
  request.get('/kyc/my').then(res => {
    const profile = res.data
    if (profile && profile.status === 1 && profile.realName) {
      username.value = profile.realName
    }
  }).catch(() => {})
})
</script>

<style scoped>
@import './layout.css';

.menu-custom-badge {
  background-color: #f56c6c;
  color: white;
  border-radius: 10px;
  padding: 0 6px;
  font-size: 12px;
  line-height: 18px;
  height: 18px;
  min-width: 18px;
  margin-left: auto;
  display: inline-flex;
  justify-content: center;
  align-items: center;
  box-sizing: border-box;
}
</style>
