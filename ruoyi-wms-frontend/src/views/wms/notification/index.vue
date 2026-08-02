<template>
  <div class="app-container">
    <el-card>
      <el-form :inline="true" :model="queryParams" ref="queryRef">
        <el-form-item label="消息类型" prop="notifyType">
          <el-select v-model="queryParams.notifyType" placeholder="全部" clearable style="width: 150px">
            <el-option label="库存预警" value="STOCK_WARNING" />
            <el-option label="AGV任务" value="AGV_TASK" />
            <el-option label="盘点提醒" value="CHECK_REMIND" />
            <el-option label="系统消息" value="SYSTEM" />
          </el-select>
        </el-form-item>
        <el-form-item label="已读状态" prop="readStatus">
          <el-select v-model="queryParams.readStatus" placeholder="全部" clearable style="width: 120px">
            <el-option label="未读" :value="0" />
            <el-option label="已读" :value="1" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
          <el-button icon="Refresh" @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card class="mt20">
      <el-row :gutter="10" class="mb8">
        <el-col :span="6">
          <span style="font-size: large">消息通知</span>
          <el-badge :value="unreadTotal" :hidden="unreadTotal === 0" class="ml10" type="danger" />
        </el-col>
        <el-col :span="1.5">
          <el-button type="success" plain icon="Check" @click="handleReadAll" :disabled="unreadTotal === 0">全部已读</el-button>
        </el-col>
        <el-col :span="1.5">
          <el-button type="danger" plain icon="Delete" @click="handleDelete" :disabled="ids.length === 0">批量删除</el-button>
        </el-col>
      </el-row>

      <el-table
        v-loading="loading"
        :data="notificationList"
        border
        @selection-change="handleSelectionChange"
        empty-text="暂无消息"
      >
        <el-table-column type="selection" width="55" align="center" />
        <el-table-column label="消息类型" align="center" width="120">
          <template #default="{ row }">
            <el-tag :type="getTypeTagType(row.notifyType)">{{ getTypeLabel(row.notifyType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="标题" prop="title" min-width="150" show-overflow-tooltip />
        <el-table-column label="内容" prop="content" min-width="300" show-overflow-tooltip />
        <el-table-column label="关联单号" prop="bizNo" align="center" width="140">
          <template #default="{ row }">
            {{ row.bizNo || '-' }}
          </template>
        </el-table-column>
        <el-table-column label="状态" align="center" width="80">
          <template #default="{ row }">
            <el-tag :type="row.readStatus === 0 ? 'danger' : 'info'" size="small">
              {{ row.readStatus === 0 ? '未读' : '已读' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="时间" align="center" width="160">
          <template #default="{ row }">
            {{ parseTime(row.createTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" align="center" width="120">
          <template #default="scope">
            <el-button
              v-if="scope.row.readStatus === 0"
              link type="primary"
              @click="handleRead(scope.row)"
            >标记已读</el-button>
            <el-button link type="danger" @click="handleDeleteRow(scope.row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-row>
        <pagination
          v-show="total > 0"
          :total="total"
          v-model:page="queryParams.pageNum"
          v-model:limit="queryParams.pageSize"
          @pagination="getList"
        />
      </el-row>
    </el-card>
  </div>
</template>

<script setup name="Notification">
import { listNotification, unreadCount, markAsRead, markAllAsRead, delNotification } from '@/api/wms/notification'
import { getCurrentInstance, reactive, ref, toRefs, onMounted } from 'vue'

const { proxy } = getCurrentInstance()

const loading = ref(true)
const notificationList = ref([])
const total = ref(0)
const unreadTotal = ref(0)
const ids = ref([])

const data = reactive({
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    notifyType: undefined,
    readStatus: undefined
  }
})
const { queryParams } = toRefs(data)

function getList() {
  loading.value = true
  listNotification(queryParams.value).then(res => {
    notificationList.value = res.data.records || []
    total.value = res.data.total || 0
    loading.value = false
  })
}

function getUnreadCount() {
  unreadCount().then(res => {
    unreadTotal.value = res.data || 0
  })
}

function handleQuery() {
  queryParams.value.pageNum = 1
  getList()
}

function resetQuery() {
  proxy.resetForm('queryRef')
  handleQuery()
}

function handleSelectionChange(selection) {
  ids.value = selection.map(item => item.id)
}

function handleRead(row) {
  markAsRead(row.id).then(() => {
    proxy.$modal.msgSuccess('已标记为已读')
    getList()
    getUnreadCount()
  })
}

function handleReadAll() {
  markAllAsRead().then(() => {
    proxy.$modal.msgSuccess('全部已读')
    getList()
    getUnreadCount()
  })
}

function handleDeleteRow(row) {
  proxy.$modal.confirm('确认删除该消息吗？').then(() => {
    delNotification(row.id).then(() => {
      proxy.$modal.msgSuccess('删除成功')
      getList()
      getUnreadCount()
    })
  })
}

function handleDelete() {
  proxy.$modal.confirm('确认删除选中的 ' + ids.value.length + ' 条消息吗？').then(() => {
    delNotification(ids.value.join(',')).then(() => {
      proxy.$modal.msgSuccess('删除成功')
      getList()
      getUnreadCount()
    })
  })
}

function getTypeLabel(type) {
  const map = {
    STOCK_WARNING: '库存预警',
    AGV_TASK: 'AGV任务',
    CHECK_REMIND: '盘点提醒',
    SYSTEM: '系统消息'
  }
  return map[type] || type
}

function getTypeTagType(type) {
  const map = {
    STOCK_WARNING: 'danger',
    AGV_TASK: 'warning',
    CHECK_REMIND: 'success',
    SYSTEM: 'info'
  }
  return map[type] || 'info'
}

onMounted(() => {
  getList()
  getUnreadCount()
})
</script>
