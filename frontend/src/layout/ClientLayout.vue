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
        <el-menu :default-active="activeMenu" class="el-menu-vertical" router background-color="transparent">
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
      <el-form :model="pwdForm" :rules="pwdRules" ref="pwdFormRef" label-position="top">
        <el-form-item label="原密码" prop="oldPassword">
          <el-input v-model="pwdForm.oldPassword" type="password" show-password placeholder="请输入当前密码" />
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="pwdForm.newPassword" type="password" show-password placeholder="请输入新密码" />
        </el-form-item>
        <el-form-item label="确认新密码" prop="confirmPassword">
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
import { computed, ref, nextTick, onMounted, onUnmounted } from 'vue'
import { Odometer, User, DocumentAdd, Wallet, Bell, UserFilled, ArrowDown, SwitchButton, Key } from '@element-plus/icons-vue'
import request from '../utils/request'
import { authApi } from '../api'
import { useUserStore } from '../stores/user'
import { ElMessage } from 'element-plus'
import { jwtDecode } from 'jwt-decode'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const activeMenu = computed(() => route.path)
const unreadCount = ref(0)
let timer = null

// 读取账号名：优先用 store，缺失时从 token 解码并回写
const getAccountName = () => {
  if (userStore.username) return userStore.username
  try {
    if (userStore.token) {
      const payload = jwtDecode(userStore.token)
      const name = payload.sub || payload.username || ''
      if (name) userStore.setUsername(name)
      return name
    }
  } catch (e) {}
  return '用户'
}
const username = ref(getAccountName())

const fetchUnreadCount = async () => {
  try {
    const res = await request.get('/message/unread-count')
    unreadCount.value = res.data || 0
  } catch (e) {
    console.error('fetchUnreadCount error:', e)
  }
}

const pwdVisible = ref(false)
const pwdLoading = ref(false)
const pwdFormRef = ref(null)
const pwdForm = ref({ oldPassword: '', newPassword: '', confirmPassword: '' })
const pwdRules = {
  oldPassword: [
    { required: true, message: '请输入当前密码', trigger: 'blur' }
  ],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 8, message: '密码长度至少8位', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (!/^(?=.*[A-Za-z])(?=.*\d)[A-Za-z\d]+$/.test(value)) {
          callback(new Error('密码必须包含字母和数字'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (value !== pwdForm.value.newPassword) {
          callback(new Error('两次输入的新密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ]
}

const openPwdDialog = () => {
  pwdForm.value = { oldPassword: '', newPassword: '', confirmPassword: '' }
  pwdVisible.value = true
  nextTick(() => {
    pwdFormRef.value?.clearValidate()
  })
}

const submitPwd = async () => {
  if (!pwdFormRef.value) return
  try {
    await pwdFormRef.value.validate()
  } catch (e) {
    return
  }
  pwdLoading.value = true
  try {
    await authApi.changePassword({
      oldPassword: pwdForm.value.oldPassword,
      newPassword: pwdForm.value.newPassword
    })
    ElMessage.success('密码修改成功，请重新登录！')
    pwdVisible.value = false
    await logout()
  } finally {
    pwdLoading.value = false
  }
}

const logout = async () => {
  try {
    await authApi.logout()
  } catch (e) {
    // 忽略吊销失败，继续清理本地状态
  }
  userStore.logout()
  router.push('/login')
}

onMounted(() => {
  fetchUnreadCount()
  timer = setInterval(fetchUnreadCount, 60000)
  // 监听消息已读事件
  window.addEventListener('unread-changed', fetchUnreadCount)
  // 已通过认证则显示真实姓名
  request.get('/kyc/my').then(res => {
    const profile = res.data
    if (profile && profile.status === 1 && profile.realName) {
      username.value = profile.realName
    }
  }).catch(() => {})
})

onUnmounted(() => {
  if (timer) clearInterval(timer)
  window.removeEventListener('unread-changed', fetchUnreadCount)
})
</script>

<style scoped>
@import './layout.css';

.menu-custom-badge {
    background-color: rgba(245, 108, 108, 0.65) !important;
    color: #ffffff !important;
  border: 1px solid rgba(245, 108, 108, 0.8);
  border-radius: 10px;
  padding: 0 6px;
  font-size: 11px;
  line-height: 16px;
  height: 16px;
  min-width: 16px;
  margin-left: auto;
  display: inline-flex;
  justify-content: center;
  align-items: center;
  box-sizing: border-box;
  font-weight: 700;
}
</style>
