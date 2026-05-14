import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/login',
    name: 'Login',
    meta: { title: '登录' },
    component: () => import('../views/Login.vue')
  },
  {
    path: '/',
    redirect: '/login'
  },
  {
    path: '/client',
    component: () => import('../layout/ClientLayout.vue'),
    redirect: '/client/dashboard',
    meta: { title: '贷款申请系统' },
    children: [
      { path: 'dashboard', name: 'ClientDashboard', component: () => import('../views/client/Dashboard.vue') },
      { path: 'kyc', name: 'ClientKyc', component: () => import('../views/client/KycSubmit.vue') },
      { path: 'apply', name: 'ClientApply', component: () => import('../views/client/ApplyLoan.vue') },
      { path: 'bills', name: 'ClientBills', component: () => import('../views/client/MyBills.vue') },
      { path: 'messages', name: 'ClientMessages', component: () => import('../views/client/Messages.vue') }
    ]
  },
  {
    path: '/admin',
    component: () => import('../layout/AdminLayout.vue'),
    redirect: '/admin/dashboard',
    meta: { title: '贷款审批系统' },
    children: [
      { path: 'dashboard', name: 'AdminDashboard', component: () => import('../views/admin/Dashboard.vue') },
      { path: 'kyc', name: 'AdminKyc', component: () => import('../views/admin/KycApproval.vue') },
      { path: 'loan', name: 'AdminLoan', component: () => import('../views/admin/LoanApproval.vue') },
      { path: 'finance', name: 'AdminFinance', component: () => import('../views/admin/Finance.vue') },
      { path: 'products', name: 'AdminProducts', component: () => import('../views/admin/ProductManage.vue') }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  // 查找匹配路由中最深层的拥有 meta.title 的路由，或者由父级直接覆盖
  const title = to.matched.slice().reverse().find(m => m.meta && m.meta.title)?.meta.title || '贷款业务管控系统'
  document.title = title
  next()
})

export default router
