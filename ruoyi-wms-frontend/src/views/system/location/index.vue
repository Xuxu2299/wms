<template>
  <div class="app-container">
    <el-card shadow="hover">
      <template #header>
        <div class="card-header">
          <span><el-icon><Grid /></el-icon> 库位管理</span>
          <div>
            <el-button type="primary" plain icon="Refresh" @click="loadList" :loading="loading">刷新</el-button>
            <el-button type="warning" plain icon="Unlock" @click="handleBatchRelease" :disabled="!selectedRows.length">
              批量释放({{ selectedRows.length }})
            </el-button>
            <el-button type="danger" plain icon="Delete" @click="handleReleaseAllOccupied"
                       :disabled="occupiedCount === 0">
              释放所有占用库位({{ occupiedCount }})
            </el-button>
          </div>
        </div>
      </template>

      <el-alert
        title="释放库位说明"
        type="warning"
        :closable="false"
        show-icon
        style="margin-bottom: 15px"
      >
        <template #default>
          <p style="margin: 0">「释放」操作会将库位状态重置为<span style="color: #67c23a; font-weight: bold;">空位</span>并清除容器号。</p>
          <p style="margin: 5px 0 0 0">适用于库位数据与实际不符（如数据库初始化后商品信息丢失导致库位卡在占用状态）的修复场景。</p>
        </template>
      </el-alert>

      <!-- 区域筛选 -->
      <el-form :inline="true" style="margin-bottom: 10px">
        <el-form-item label="区域">
          <el-radio-group v-model="filterArea" @change="handleFilterChange">
            <el-radio-button label="">全部</el-radio-button>
            <el-radio-button label="A">A区(存储)</el-radio-button>
            <el-radio-button label="R">R区(入库起点)</el-radio-button>
            <el-radio-button label="C">C区(出库终点)</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="filterStatus" @change="handleFilterChange">
            <el-radio-button label="">全部</el-radio-button>
            <el-radio-button label="1">有货</el-radio-button>
            <el-radio-button label="0">空位</el-radio-button>
          </el-radio-group>
        </el-form-item>
      </el-form>

      <el-table :data="filteredList" border v-loading="loading" @selection-change="handleSelectionChange"
                empty-text="暂无库位数据" style="width: 100%">
        <el-table-column type="selection" width="55" />
        <el-table-column label="库位编码" prop="locationCode" align="center" width="120" />
        <el-table-column label="区域" prop="area" align="center" width="80">
          <template #default="{ row }">
            <el-tag :type="areaTagType(row.area)">{{ areaLabel(row.area) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" prop="status" align="center" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'danger' : 'success'">
              {{ row.status === 1 ? '有货' : '空位' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="容器号" prop="containerNo" align="center" width="140">
          <template #default="{ row }">
            <span v-if="row.containerNo">{{ row.containerNo }}</span>
            <span v-else style="color: #c0c4cc">-</span>
          </template>
        </el-table-column>
        <el-table-column label="所属仓库" align="center">
          <template #default="{ row }">
            <span>{{ useWmsStore().warehouseMap.get(row.warehouseId)?.warehouseName || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="备注" prop="remark" align="center" show-overflow-tooltip />
        <el-table-column label="操作" width="220" align="center" fixed="right">
          <template #default="{ row }">
            <el-button
              link
              type="warning"
              icon="Unlock"
              @click="handleRelease(row)"
              :disabled="row.status === 0 && !row.containerNo"
            >释放</el-button>
            <el-button link type="primary" icon="Edit" @click="handleEdit(row)">编辑</el-button>
            <el-button link type="danger" icon="Delete" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 统计信息 -->
      <div class="mt10" style="display: flex; gap: 30px; color: #909399; font-size: 13px;">
        <span>库位总数：{{ filteredList.length }}</span>
        <span>有货：{{ occupiedCount }}</span>
        <span>空位：{{ emptyCount }}</span>
      </div>
    </el-card>

    <!-- 新增/编辑库位对话框 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="500px" @close="resetForm">
      <el-form ref="locationFormRef" :model="locationForm" :rules="formRules" label-width="90px">
        <el-form-item label="库位编码" prop="locationCode">
          <el-input v-model="locationForm.locationCode" placeholder="如 A21、R3、C3" :disabled="!!locationForm.id" />
        </el-form-item>
        <el-form-item label="区域" prop="area">
          <el-select v-model="locationForm.area" placeholder="请选择区域" style="width: 100%">
            <el-option label="A区（存储）" value="A" />
            <el-option label="R区（入库起点）" value="R" />
            <el-option label="C区（出库终点）" value="C" />
          </el-select>
        </el-form-item>
        <el-form-item label="所属仓库" prop="warehouseId">
          <el-select v-model="locationForm.warehouseId" placeholder="请选择仓库" filterable style="width: 100%">
            <el-option v-for="item in useWmsStore().warehouseList" :key="item.id" :label="item.warehouseName" :value="item.id"/>
          </el-select>
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="locationForm.remark" type="textarea" :rows="2" placeholder="可选" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitForm" :loading="submitLoading">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="LocationManage">
import { listAllLocations, releaseLocations, addLocation, updateLocation, deleteLocation } from "@/api/wms/location";
import { useWmsStore } from '@/store/modules/wms'
import { computed, getCurrentInstance, onMounted, ref } from 'vue'

const { proxy } = getCurrentInstance()

const loading = ref(false)
const locationList = ref([])
const selectedRows = ref([])
const filterArea = ref('')
const filterStatus = ref('')

const useWmsStoreInstance = useWmsStore()

/** 过滤后的列表 */
const filteredList = computed(() => {
  return locationList.value.filter(item => {
    if (filterArea.value && item.area !== filterArea.value) return false
    if (filterStatus.value !== '' && String(item.status) !== filterStatus.value) return false
    return true
  })
})

const occupiedCount = computed(() => filteredList.value.filter(it => it.status === 1).length)
const emptyCount = computed(() => filteredList.value.filter(it => it.status === 0).length)

/** 区域标签类型 */
const areaTagType = (area) => {
  const map = { 'A': 'primary', 'R': 'success', 'C': 'warning' }
  return map[area] || 'info'
}

/** 区域标签文字 */
const areaLabel = (area) => {
  const map = { 'A': 'A区', 'R': 'R区', 'C': 'C区' }
  return map[area] || area
}

/** 加载库位列表 */
function loadList() {
  loading.value = true
  listAllLocations().then(res => {
    locationList.value = res.data || []
  }).finally(() => {
    loading.value = false
  })
}

/** 筛选变化 */
function handleFilterChange() {
  // computed 自动响应
}

/** 多选变化 */
function handleSelectionChange(rows) {
  selectedRows.value = rows
}

/** 释放单个库位 */
function handleRelease(row) {
  proxy.$modal.confirm(`确定要释放库位 ${row.locationCode} 吗？该操作将清除其状态和容器号，不可恢复。`).then(() => {
    return releaseLocations([row.locationCode])
  }).then(res => {
    if (res.code === 200) {
      proxy.$modal.msgSuccess(`库位 ${row.locationCode} 已释放`)
      loadList()
    }
  }).catch(() => {})
}

/** 批量释放选中的库位 */
function handleBatchRelease() {
  if (!selectedRows.value.length) return
  const codes = selectedRows.value.map(it => it.locationCode)
  proxy.$modal.confirm(`确定要释放选中的 ${codes.length} 个库位吗？此操作不可恢复。`).then(() => {
    return releaseLocations(codes)
  }).then(res => {
    if (res.code === 200) {
      proxy.$modal.msgSuccess(`已释放 ${res.data} 个库位`)
      loadList()
    }
  }).catch(() => {})
}

/** 释放所有有货库位 */
function handleReleaseAllOccupied() {
  const occupiedCodes = filteredList.value.filter(it => it.status === 1).map(it => it.locationCode)
  if (!occupiedCodes.length) return
  proxy.$modal.confirm(`确定要释放当前列表中所有 ${occupiedCodes.length} 个有货库位吗？此操作将清除它们的状态和容器号，不可恢复！`).then(() => {
    return releaseLocations(occupiedCodes)
  }).then(res => {
    if (res.code === 200) {
      proxy.$modal.msgSuccess(`已释放 ${res.data} 个库位`)
      loadList()
    }
  }).catch(() => {})
}

// ===== 新增/编辑/删除相关 =====
const dialogVisible = ref(false)
const dialogTitle = ref('')
const submitLoading = ref(false)
const locationFormRef = ref(null)
const locationForm = ref({
  id: undefined,
  locationCode: '',
  area: 'A',
  warehouseId: undefined,
  remark: ''
})
const formRules = {
  locationCode: [{ required: true, message: '请输入库位编码', trigger: 'blur' }],
  area: [{ required: true, message: '请选择区域', trigger: 'change' }],
  warehouseId: [{ required: true, message: '请选择仓库', trigger: 'change' }]
}

/** 重置表单 */
function resetForm() {
  locationForm.value = {
    id: undefined,
    locationCode: '',
    area: 'A',
    warehouseId: undefined,
    remark: ''
  }
  locationFormRef.value?.resetFields()
}

/** 新增 */
function handleAdd() {
  resetForm()
  dialogTitle.value = '新增库位'
  dialogVisible.value = true
}

/** 编辑 */
function handleEdit(row) {
  resetForm()
  dialogTitle.value = '编辑库位'
  locationForm.value = {
    id: row.id,
    locationCode: row.locationCode,
    area: row.area,
    warehouseId: row.warehouseId,
    remark: row.remark || ''
  }
  dialogVisible.value = true
}

/** 提交新增/编辑 */
function submitForm() {
  locationFormRef.value?.validate(valid => {
    if (!valid) return
    submitLoading.value = true
    const data = { ...locationForm.value }
    const action = data.id ? updateLocation(data) : addLocation(data)
    action.then(res => {
      if (res.code === 200) {
        proxy.$modal.msgSuccess(data.id ? '修改成功' : '新增成功')
        dialogVisible.value = false
        loadList()
      } else {
        proxy.$modal.msgError(res.msg || '操作失败')
      }
    }).finally(() => {
      submitLoading.value = false
    })
  })
}

/** 删除 */
function handleDelete(row) {
  proxy.$modal.confirm(`确定要删除库位「${row.locationCode}」吗？有货库位需先释放才能删除。`).then(() => {
    return deleteLocation(row.id)
  }).then(res => {
    if (res.code === 200) {
      proxy.$modal.msgSuccess(`库位「${row.locationCode}」已删除`)
      loadList()
    } else {
      proxy.$modal.msgError(res.msg || '删除失败')
    }
  }).catch(() => {})
}

onMounted(() => {
  loadList()
})
</script>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.mt10 {
  margin-top: 10px;
}
</style>
