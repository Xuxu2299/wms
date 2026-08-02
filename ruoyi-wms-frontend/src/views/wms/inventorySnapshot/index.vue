<template>
  <div class="app-container">
    <el-card>
      <el-form :inline="true" :model="queryParams" ref="queryRef">
        <el-form-item label="快照日期" prop="date">
          <el-select v-model="queryParams.date" placeholder="选择快照日期" clearable @change="handleQuery" style="width: 200px">
            <el-option
              v-for="d in snapshotDates"
              :key="d"
              :label="d"
              :value="d"
            />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="Search" @click="handleQuery">查询</el-button>
          <el-button type="success" icon="Camera" @click="handleCreate" :loading="createLoading">创建快照</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card class="mt20">
      <el-table v-loading="loading" :data="snapshotList" border empty-text="暂无快照数据">
        <el-table-column label="商品名称" prop="itemName" min-width="120" />
        <el-table-column label="规格名称" prop="skuName" min-width="100" />
        <el-table-column label="仓库" prop="warehouseName" min-width="100" />
        <el-table-column label="库存数量" prop="quantity" align="right" width="100">
          <template #default="{ row }">
            <el-statistic :value="Number(row.quantity)" :precision="0"/>
          </template>
        </el-table-column>
        <el-table-column label="安全下限" prop="minStock" align="right" width="100">
          <template #default="{ row }">
            <span :class="{ 'text-danger': Number(row.minStock) > 0 && Number(row.quantity) < Number(row.minStock) }">
              {{ row.minStock }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="安全上限" prop="maxStock" align="right" width="100">
          <template #default="{ row }">
            <span :class="{ 'text-warning': Number(row.maxStock) > 0 && Number(row.quantity) > Number(row.maxStock) }">
              {{ row.maxStock }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="快照日期" prop="snapshotDate" align="center" width="120" />
      </el-table>
    </el-card>
  </div>
</template>

<script setup name="InventorySnapshot">
import { listInventorySnapshot, listSnapshotDates, createSnapshot } from '@/api/wms/inventorySnapshot'

const { proxy } = getCurrentInstance()

const loading = ref(false)
const createLoading = ref(false)
const snapshotList = ref([])
const snapshotDates = ref([])
const queryParams = ref({
  date: undefined
})

function loadDates() {
  listSnapshotDates().then(res => {
    if (res.code === 200) {
      snapshotDates.value = res.data || []
      if (snapshotDates.value.length > 0 && !queryParams.value.date) {
        queryParams.value.date = snapshotDates.value[0]
        getList()
      }
    }
  })
}

function getList() {
  if (!queryParams.value.date) {
    proxy.$modal.msgWarning('请选择快照日期')
    return
  }
  loading.value = true
  listInventorySnapshot(queryParams.value.date).then(res => {
    if (res.code === 200) {
      snapshotList.value = res.data || []
    }
  }).finally(() => {
    loading.value = false
  })
}

function handleQuery() {
  getList()
}

function handleCreate() {
  proxy.$modal.confirm('确认立即创建当前库存快照？').then(() => {
    createLoading.value = true
    createSnapshot().then(() => {
      proxy.$modal.msgSuccess('快照创建成功')
      loadDates()
    }).finally(() => {
      createLoading.value = false
    })
  }).catch(() => {})
}

onMounted(() => {
  loadDates()
})
</script>

<style scoped>
.text-danger {
  color: #f56c6c;
  font-weight: bold;
}
.text-warning {
  color: #e6a23c;
  font-weight: bold;
}
</style>
