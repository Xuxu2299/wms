<template>
  <div class="app-container">
    <el-card>
      <el-form :inline="true" :model="queryParams" ref="queryRef">
        <el-form-item label="状态" prop="status">
          <el-select v-model="queryParams.status" placeholder="全部" clearable style="width: 120px">
            <el-option label="待拣货" :value="0" />
            <el-option label="拣货中" :value="1" />
            <el-option label="已完成" :value="2" />
            <el-option label="已取消" :value="3" />
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
        <el-col :span="6"><span style="font-size: large">波次拣货</span></el-col>
        <el-col :span="1.5">
          <el-button type="primary" plain icon="Plus" @click="handleAdd">创建波次</el-button>
        </el-col>
      </el-row>

      <el-table v-loading="loading" :data="waveList" border empty-text="暂无波次">
        <el-table-column label="波次号" prop="waveNo" min-width="180" />
        <el-table-column label="仓库" prop="warehouseName" min-width="100" />
        <el-table-column label="出库单数" prop="orderCount" align="center" width="100" />
        <el-table-column label="总数量" prop="totalQuantity" align="right" width="100" />
        <el-table-column label="状态" align="center" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">{{ getStatusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" align="center" width="160">
          <template #default="{ row }">
            {{ parseTime(row.createTime) }}
          </template>
        </el-table-column>
        <el-table-column label="备注" prop="remark" min-width="120" show-overflow-tooltip />
        <el-table-column label="操作" align="center" width="200">
          <template #default="scope">
            <el-button link type="primary" @click="handleDetail(scope.row)">详情</el-button>
            <el-button
              v-if="scope.row.status === 0 || scope.row.status === 1"
              link type="warning" @click="handleCancel(scope.row)"
            >取消</el-button>
            <el-button
              v-if="scope.row.status === 2 || scope.row.status === 3"
              link type="danger" @click="handleDelete(scope.row)"
            >删除</el-button>
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

    <!-- 创建波次对话框 -->
    <el-dialog v-model="createOpen" title="创建波次" width="600px">
      <el-form :model="createForm" label-width="100px">
        <el-form-item label="仓库">
          <el-select v-model="createForm.warehouseId" placeholder="选择仓库" style="width: 100%">
            <el-option
              v-for="w in useWmsStore().warehouseList"
              :key="w.id"
              :label="w.warehouseName"
              :value="w.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="出库单选择">
          <el-select
            v-model="createForm.shipmentOrderIds"
            multiple
            filterable
            placeholder="选择待出库单"
            style="width: 100%"
          >
            <el-option
              v-for="o in pendingOrders"
              :key="o.id"
              :label="o.orderNo + ' (数量:' + o.totalQuantity + ')'"
              :value="o.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="createForm.remark" type="textarea" placeholder="备注" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createOpen = false">取消</el-button>
        <el-button type="primary" @click="submitCreate" :loading="createLoading">确认创建</el-button>
      </template>
    </el-dialog>

    <!-- 波次详情对话框 -->
    <el-dialog v-model="detailOpen" title="波次详情" width="800px">
      <el-descriptions :column="3" border>
        <el-descriptions-item label="波次号">{{ detailData.waveNo }}</el-descriptions-item>
        <el-descriptions-item label="仓库">{{ detailData.warehouseName }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="getStatusType(detailData.status)">{{ getStatusLabel(detailData.status) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="出库单数">{{ detailData.orderCount }}</el-descriptions-item>
        <el-descriptions-item label="总数量">{{ detailData.totalQuantity }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ parseTime(detailData.createTime) }}</el-descriptions-item>
      </el-descriptions>
      <el-table :data="detailData.details" border class="mt20" empty-text="暂无明细">
        <el-table-column label="出库单号" prop="shipmentOrderNo" min-width="140" />
        <el-table-column label="商品名称" prop="itemName" min-width="120" />
        <el-table-column label="规格名称" prop="skuName" min-width="100" />
        <el-table-column label="源库位" prop="sourceLocation" align="center" width="80" />
        <el-table-column label="容器号" prop="containerNo" align="center" width="80" />
        <el-table-column label="数量" prop="quantity" align="right" width="80" />
        <el-table-column label="状态" align="center" width="80">
          <template #default="{ row }">
            <el-tag :type="row.pickStatus === 0 ? 'danger' : 'success'" size="small">
              {{ row.pickStatus === 0 ? '待拣' : '已拣' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" align="center" width="100">
          <template #default="scope">
            <el-button
              v-if="scope.row.pickStatus === 0 && (detailData.status === 0 || detailData.status === 1)"
              link type="success" @click="handlePick(scope.row)"
            >标记已拣</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup name="WavePick">
import { listWavePick, getWavePick, createWave, markPicked, cancelWavePick, delWavePick } from '@/api/wms/wavePick'
import { listShipmentOrder } from '@/api/wms/shipmentOrder'
import { useWmsStore } from '@/store/modules/wms'
import { getCurrentInstance, reactive, ref, toRefs, onMounted } from 'vue'

const { proxy } = getCurrentInstance()
const useStore = useWmsStore()

const loading = ref(true)
const waveList = ref([])
const total = ref(0)
const createOpen = ref(false)
const createLoading = ref(false)
const detailOpen = ref(false)
const detailData = ref({})
const pendingOrders = ref([])

const data = reactive({
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    status: undefined
  },
  createForm: {
    warehouseId: undefined,
    shipmentOrderIds: [],
    remark: ''
  }
})
const { queryParams, createForm } = toRefs(data)

function getList() {
  loading.value = true
  listWavePick(queryParams.value).then(res => {
    waveList.value = res.data.records || []
    total.value = res.data.total || 0
    loading.value = false
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

function handleAdd() {
  createForm.value = { warehouseId: undefined, shipmentOrderIds: [], remark: '' }
  // 加载待出库单
  listShipmentOrder({ orderStatus: 0, pageNum: 1, pageSize: 100 }).then(res => {
    pendingOrders.value = res.rows || []
    createOpen.value = true
  })
}

function submitCreate() {
  if (!createForm.value.shipmentOrderIds.length) {
    proxy.$modal.msgWarning('请选择出库单')
    return
  }
  createLoading.value = true
  createWave(createForm.value).then(() => {
    proxy.$modal.msgSuccess('波次创建成功')
    createOpen.value = false
    getList()
  }).finally(() => {
    createLoading.value = false
  })
}

function handleDetail(row) {
  getWavePick(row.id).then(res => {
    detailData.value = res.data
    detailOpen.value = true
  })
}

function handlePick(row) {
  markPicked(row.id).then(() => {
    proxy.$modal.msgSuccess('已标记为已拣')
    // 刷新详情
    getWavePick(detailData.value.id).then(res => {
      detailData.value = res.data
    })
    getList()
  })
}

function handleCancel(row) {
  proxy.$modal.confirm('确认取消波次 ' + row.waveNo + ' 吗？').then(() => {
    cancelWavePick(row.id).then(() => {
      proxy.$modal.msgSuccess('已取消')
      getList()
    })
  })
}

function handleDelete(row) {
  proxy.$modal.confirm('确认删除波次 ' + row.waveNo + ' 吗？').then(() => {
    delWavePick(row.id).then(() => {
      proxy.$modal.msgSuccess('删除成功')
      getList()
    })
  })
}

function getStatusLabel(status) {
  const map = { 0: '待拣货', 1: '拣货中', 2: '已完成', 3: '已取消' }
  return map[status] || '未知'
}

function getStatusType(status) {
  const map = { 0: 'info', 1: 'warning', 2: 'success', 3: 'danger' }
  return map[status] || 'info'
}

onMounted(() => {
  getList()
})
</script>
