<template>
  <div class="page-container flex-center">
    <el-card class="kyc-card glass-panel dark-mode-card" shadow="hover">
      <div class="card-header">
        <h2>实名认证</h2>
        <p>请填写真实身份信息</p>
      </div>

      <!-- 结果展示区 -->
      <div v-if="profile && !isEditing" class="result-box">
         <el-icon class="icon" :class="profile.status === 1 ? 'success-icon' : (profile.status === 2 ? 'danger-icon' : 'warning-icon')">
            <Warning v-if="profile.status === 0" />
            <CircleCheck v-else-if="profile.status === 1" />
            <CircleClose v-else />
         </el-icon>
         <h3>{{ profile.status === 0 ? '审核中' : (profile.status === 1 ? '已通过' : '已驳回') }}</h3>
         <p v-if="profile.status === 1" style="color: #67c23a; margin-top:10px;">您可以前往【申请贷款】发起贷款申请。</p>
         <div v-if="profile.status === 2" style="margin-top:20px;">
            <el-button type="primary" color="#626aef" style="color:white; width:200px" size="large" @click="reSubmit">重新提交资料</el-button>
         </div>
      </div>

      <!-- 填表区 -->
      <el-form v-if="!profile || isEditing" :model="form" :rules="rules" ref="formRef" label-position="top" class="kyc-form">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="真实姓名" prop="realName">
              <el-input v-model="form.realName" placeholder="必须与身份证保持一致" size="large"></el-input>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="身份证号" prop="idCard">
              <el-input v-model="form.idCard" placeholder="输入18位有效证件号" size="large"></el-input>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="身份证人像面" prop="idCardFront">
              <el-upload
                class="avatar-uploader"
                action="/api/upload"
                :headers="getUploadHeaders()"
                :show-file-list="false"
                :on-success="res => handleSuccess(res, 'idCardFront')"
                :before-upload="beforeUpload">
                <img v-if="form.idCardFront" :src="fileUrl(form.idCardFront)" class="avatar" />
                <div v-else class="upload-placeholder">
                  <el-icon class="avatar-uploader-icon"><Plus /></el-icon>
                  <p>点击上传</p>
                </div>
              </el-upload>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="身份证国徽面" prop="idCardBack">
              <el-upload
                class="avatar-uploader"
                action="/api/upload"
                :headers="getUploadHeaders()"
                :show-file-list="false"
                :on-success="res => handleSuccess(res, 'idCardBack')"
                :before-upload="beforeUpload">
                <img v-if="form.idCardBack" :src="fileUrl(form.idCardBack)" class="avatar" />
                <div v-else class="upload-placeholder">
                  <el-icon class="avatar-uploader-icon"><Plus /></el-icon>
                  <p>点击上传</p>
                </div>
              </el-upload>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="开户银行" prop="bankName">
              <el-select v-model="form.bankName" placeholder="请选择收款银行" size="large" style="width: 100%" filterable>
                <el-option v-for="bank in bankOptions" :key="bank" :label="bank" :value="bank" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="银行卡号" prop="bankCard">
              <el-input v-model="form.bankCard" placeholder="请输入银行卡号" size="large"></el-input>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="手机号" prop="phone">
              <el-input v-model="form.phone" placeholder="请输入本人手机号" size="large" maxlength="11" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="电子邮箱" prop="email">
              <el-input v-model="form.email" placeholder="用于接收还款提醒公告" size="large" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item>
          <el-button type="primary" size="large" @click="submit" :loading="submitting" class="submit-btn" color="#626aef" style="color:white;">
            确认提交认证
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import request from '../../utils/request'
import { ElMessage } from 'element-plus'
import { Warning, CircleCheck, CircleClose, Plus } from '@element-plus/icons-vue'
import { fileUrl } from '../../utils/format'

const formRef = ref(null)
const profile = ref(null)
const isEditing = ref(false)
const loading = ref(false)
const submitting = ref(false)

const getUploadHeaders = () => {
  return { Authorization: 'Bearer ' + (localStorage.getItem('token') || '') }
}

const form = ref({
  realName: '',
  idCard: '',
  idCardFront: '',
  idCardBack: '',
  bankName: '',
  bankCard: '',
  phone: '',
  email: ''
})

// 银行列表
const bankOptions = [
  '中国工商银行',
  '中国建设银行',
  '中国农业银行',
  '中国银行',
  '招商银行',
  '交通银行',
  '中国邮政储蓄银行',
  '兴业银行',
  '中信银行',
  '上海浦东发展银行',
  '中国民生银行',
  '中国光大银行',
  '平安银行',
  '华夏银行',
  '北京银行',
  '广发银行',
  '江苏银行',
  '上海银行',
  '宁波银行',
  '浙商银行',
  '南京银行',
  '重庆农村商业银行',
  '徽商银行',
  '上海农商银行',
  '杭州银行',
  '恒丰银行',
  '渤海银行',
  '北京农商银行',
  '广州农商银行',
  '成都银行',
  '中原银行',
  '天津银行',
  '厦门国际银行',
  '长沙银行',
  '汇丰银行 (中国)',
  '贵阳银行',
  '成都农商银行',
  '东莞农村商业银行',
  '深圳农商银行',
  '微众银行',
  '重庆银行',
  '哈尔滨银行',
  '吉林银行',
  '广州银行',
  '贵州银行',
  '苏州银行',
  '郑州银行',
  '江南农村商业银行',
  '昆仑银行',
  '齐鲁银行',
  '东莞银行',
  '杭州联合银行',
  '青岛银行',
  '天津农村商业银行',
  '桂林银行',
  '江西银行',
  '四川银行',
  '顺德农村商业银行',
  '湖南银行',
  '青岛农村商业银行',
  '九江银行',
  '河北银行',
  '浙江泰隆商业银行',
  '台州银行',
  '大连银行',
  '汉口银行',
  '甘肃银行',
  '西安银行',
  '渣打银行 (中国)',
  '湖北银行',
  '广西北部湾银行',
  '兰州银行',
  '珠海华润银行',
  '长安银行',
  '常熟农商银行',
  '广东南粤银行',
  '武汉农村商业银行',
  '广东华兴银行',
  '南海农商银行',
  '海南农商银行',
  '萧山农商银行',
  '晋商银行',
  '唐山银行',
  '威海银行',
  '浙江稠州商业银行',
  '三菱日联银行 (中国)',
  '温州银行',
  '厦门银行',
  '网商银行',
  '三井住友银行 (中国)',
  '富滇银行',
  '秦农银行',
  '瑞穗银行 (中国)',
  '沧州银行',
  '东亚银行 (中国)',
  '山西银行',
  '日照银行',
  '潍坊银行',
  '重庆三峡银行',
  '蒙商银行'
]

const rules = {
  realName: [{ required: true, message: '不可为空', trigger: 'blur' }],
  idCard: [
    { required: true, message: '不可为空', trigger: 'blur' },
    { pattern: /^\d{17}[\dXx]$/, message: '请输入18位合法身份证号', trigger: 'blur' }
  ],
  idCardFront: [{ required: true, message: '请上传', trigger: 'change' }],
  idCardBack: [{ required: true, message: '请上传', trigger: 'change' }],
  bankName: [{ required: true, message: '请选择', trigger: 'change' }],
  bankCard: [
    { required: true, message: '此处必填', trigger: 'blur' },
    { pattern: /^\d{16,19}$/, message: '银行卡号为16-19位数字', trigger: 'blur' }
  ],
  phone: [
    { required: true, message: '手机号不可为空', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的大陆手机号', trigger: 'blur' }
  ],
  email: [
    { required: true, message: '邮箱不可为空', trigger: 'blur' },
    { type: 'email', message: '邮箱格式不正确', trigger: 'blur' }
  ]
}

const handleSuccess = (res, field) => {
    if (res.code === 200) {
        form.value[field] = res.data
        ElMessage.success('图片上传成功')
    } else {
        ElMessage.error(res.msg || '上传失败')
    }
}

const beforeUpload = (file) => {
  const isValid = file.type === 'image/jpeg' || file.type === 'image/png' || file.type === 'image/webp'
  if (!isValid) ElMessage.error('仅仅支持 JPG/PNG/WEBP 图像！')
  if (file.size > 5*1024*1024) { ElMessage.error('图片大小不能超过5MB'); return false }
  return isValid
}

const submit = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (valid) {
      submitting.value = true
      try {
        await request.post('/kyc/submit', form.value)
        ElMessage.success('实名认证资料已提交，等待审核。')
        isEditing.value = false
        loadData()
      } finally {
        submitting.value = false
      }
    }
  })
}

const loadData = async () => {
  loading.value = true
  try {
    const res = await request.get('/kyc/my')
    profile.value = res.data
  } finally {
    loading.value = false
  }
}

const reSubmit = () => {
  if (profile.value) {
    form.value = {
      realName: profile.value.realName || '',
      idCard: profile.value.idCard || '',
      idCardFront: profile.value.idCardFront || '',
      idCardBack: profile.value.idCardBack || '',
      bankName: profile.value.bankName || '',
      bankCard: profile.value.bankCard || '',
      phone: profile.value.phone || '',
      email: profile.value.email || ''
    }
    isEditing.value = true
  }
}

onMounted(() => loadData())
</script>

<style scoped>
.flex-center {
    display: flex;
    justify-content: center;
    align-items: center;
    min-height: calc(100vh - 120px);
    padding: 30px;
}

.kyc-card {
    width: 100%;
    max-width: 800px;
    background: rgba(30, 41, 59, 0.7);
    border: 1px solid rgba(255,255,255,0.1);
    box-shadow: 0 8px 32px rgba(0,0,0,0.3);
    border-radius: 16px;
    padding: 20px;
    color: #fff;
}
.card-header { text-align: center; margin-bottom: 30px; }
.card-header h2 { font-size: 26px; color: #fff; margin-bottom: 8px;}
.card-header p { color: #94a3b8; font-size: 14px;}

.result-box {
    text-align: center; padding: 40px 0; color: #fff;
}
.result-box .icon { font-size: 60px; margin-bottom: 20px; }
.warning-icon { color: #e6a23c; }
.success-icon { color: #67c23a; }
.danger-icon { color: #f56c6c; }

:deep(.el-form-item__label) { color: #cbd5e0 !important; font-weight: bold;}
:deep(.el-input__wrapper), :deep(.el-select__wrapper) { background: rgba(0,0,0,0.3) !important; box-shadow: 0 0 0 1px rgba(255,255,255,0.1) inset !important; }
:deep(.el-input__inner) { color: #fff !important; }
:deep(.el-select__input) { color: #fff !important; }
:deep(.el-select__selected-item) { color: #fff !important; }
:deep(.el-select__selected-item.is-transparent) { color: var(--el-text-color-placeholder) !important; }

.submit-btn { width: 100%; margin-top: 20px; font-weight: bold; border-radius: 8px; }

/* 图像上传组件样式重塑 */
.avatar-uploader {
  width: 100%;
  display: block;
}
.avatar-uploader :deep(.el-upload) {
  border: 1px dashed rgba(255,255,255,0.3);
  border-radius: 8px;
  cursor: pointer;
  position: relative;
  overflow: hidden;
  transition: border-color 0.2s;
  background: rgba(0,0,0,0.2);
  width: 100%;
  height: 160px;
  display: flex;
  justify-content: center;
  align-items: center;
  box-sizing: border-box;
}
.avatar-uploader :deep(.el-upload:hover) { border-color: #626aef; }

/* 加号 + 提示文字的复合占位区 */
.upload-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  color: #8c939d;
}
.upload-placeholder .avatar-uploader-icon {
  font-size: 36px;
  color: #8c939d;
}
.upload-placeholder p {
  font-size: 12px;
  margin: 0;
  color: #94a3b8;
}
.avatar { width: 100%; height: 100%; object-fit: contain; display: block; }
</style>
