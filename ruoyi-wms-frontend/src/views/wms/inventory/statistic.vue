<template>
  <div class="app-container">
    <el-card>
      <el-form :model="queryParams" ref="queryRef" label-width="90px" :inline="true">
        <el-form-item class="col4" label="维度 " prop="itemId">
          <el-radio-group v-model="queryType" size="default" @change="handleSortTypeChange">
            <el-radio-button label="warehouse">仓库</el-radio-button>
            <el-radio-button label="item">商品</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item class="col4" label="仓库" prop="warehouseId">
          <el-select style="width: 100%" v-model="queryParams.warehouseId" placeholder="请选择仓库"
                     filterable clearable>
            <el-option v-for="item in useWmsStore().warehouseList" :key="item.id" :label="item.warehouseName"
                       :value="item.id"/>
          </el-select>
        </el-form-item>
        <el-form-item class="col4" label="商品名称" prop="itemName">
          <el-input v-model="queryParams.itemName" clearable placeholder="商品名称"></el-input>
        </el-form-item>
        <el-form-item class="col4" label="商品编号" prop="itemCode">
          <el-input v-model="queryParams.itemCode" clearable placeholder="商品编号"></el-input>
        </el-form-item>
        <el-form-item class="col4" label="规格名称" prop="skuName">
          <el-input v-model="queryParams.skuName" clearable placeholder="规格名称"></el-input>
        </el-form-item>
        <el-form-item class="col4" label="规格编号" prop="skuCode">
          <el-input v-model="queryParams.skuCode" clearable placeholder="规格编号"></el-input>
        </el-form-item>
        <el-form-item class="col4" style="margin-left: 32px">
          <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
          <el-button icon="Refresh" @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>
    <el-card class="mt20">
      <div class="mb8 flex-space-between">
        <div style="font-size: large">库存统计</div>
        <div>
          <el-button
            type="warning"
            plain
            icon="Download"
            @click="handleExport"
            v-hasPermi="['wms:inventory:all']"
          >导出</el-button>
          <el-checkbox v-model="filterable" label="过滤掉库存为0的商品" size="large" @change="handleChangeFilterZero"/>
        </div>
      </div>
      <el-table :data="inventoryList" border :span-method="spanMethod"
                cell-class-name="vertical-top-cell" v-loading="loading" empty-text="暂无库存">
        <template v-if="queryType == 'warehouse'">
          <el-table-column label="仓库" prop="warehouseId">
            <template #default="{ row }">
              <div>{{ useWmsStore().warehouseMap.get(row.warehouseId)?.warehouseName }}</div>
            </template>
          </el-table-column>
          <el-table-column label="商品信息" prop="warehouseIdAndItemId">
            <template #default="{ row }">
              <div>{{ row.item.itemName }}</div>
              <div v-if="row.item.itemCode">商品编号：{{ row.item.itemCode }}</div>
            </template>
          </el-table-column>
          <el-table-column label="规格信息" :prop="skuId">
            <template #default="{ row }">
              <div>{{ row.itemSku.skuName }}</div>
              <div v-if="row.itemSku.skuCode">规格编号：{{ row.itemSku.skuCode }}</div>
            </template>
          </el-table-column>
        </template>
        <template v-else>
          <el-table-column label="商品信息" prop="itemId">
            <template #default="{ row }">
              <div>{{ row.item.itemName }}</div>
              <div v-if="row.item.itemCode">商品编号：{{ row.item.itemCode }}</div>
            </template>
          </el-table-column>
          <el-table-column label="规格信息" prop="skuId">
            <template #default="{ row }">
              <div>{{ row.itemSku.skuName }}</div>
              <div v-if="row.itemSku.skuCode">规格编号：{{ row.itemSku.skuCode }}</div>
            </template>
          </el-table-column>
          <el-table-column label="仓库" prop="skuIdAndWarehouseId">
            <template #default="{row}">
              <div>{{ useWmsStore().warehouseMap.get(row.warehouseId)?.warehouseName }}</div>
            </template>
          </el-table-column>
        </template>
        <el-table-column label="库存" prop="quantity" align="right">
          <template #default="{ row }">
            <el-statistic :value="Number(row.quantity)" :precision="0"/>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" align="center" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" icon="Location" @click="handleViewLocation(row)">查看库位</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-row>
        <pagination v-show="total>0" :total="total" v-model:page="queryParams.pageNum"
                    v-model:limit="queryParams.pageSize" @pagination="getList"/>
      </el-row>
    </el-card>

    <!-- 库位明细弹窗 -->
    <el-dialog v-model="locationDialogVisible" title="库位明细" width="600px" append-to-body>
      <div class="mb10" style="font-size: 14px; color: #606266;">
        <span v-if="locationDialogTitle">{{ locationDialogTitle }}</span>
      </div>
      <el-table :data="locationInventoryList" border v-loading="locationLoading" empty-text="暂无库位库存数据">
        <el-table-column label="库位编码" prop="locationCode" align="center" />
        <el-table-column label="剩余库存" prop="quantity" align="center">
          <template #default="{ row }">
            <el-tag :type="row.quantity > 0 ? 'success' : 'info'">{{ row.quantity }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="容器号" prop="containerNo" align="center">
          <template #default="{ row }">
            <span>{{ row.containerNo || '-' }}</span>
          </template>
        </el-table-column>
      </el-table>
      <div class="mt10" style="text-align: right; color: #909399; font-size: 13px;" v-if="locationInventoryList.length > 0">
        库位库存总量：{{ locationTotalQuantity }}
      </div>
      <template #footer>
        <el-button @click="locationDialogVisible = false">关 闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="Inventory">
import {
  listInventoryBoard
} from '@/api/wms/inventory';
import { listInventoryBySku } from '@/api/wms/location';
import {computed, getCurrentInstance, onMounted, ref} from 'vue';
import {ElForm} from 'element-plus';
import {getRowspanMethod} from "@/utils/getRowSpanMethod";
import {useWmsStore} from '@/store/modules/wms'

const {proxy} = getCurrentInstance();
const spanMethod = computed(() => getRowspanMethod(inventoryList.value, rowSpanArray.value))

const inventoryList = ref([]);
const buttonLoading = ref(false);
const loading = ref(true);
const showSearch = ref(true);
const ids = ref([]);
const single = ref(true);
const multiple = ref(true);
const total = ref(0);
const rowSpanArray = ref(['warehouseId', 'warehouseIdAndItemId', 'warehouseIdAndSkuId'])

const filterable = ref(false)
const queryType = ref("warehouse")
const queryParams = ref({
  pageNum: 1,
  pageSize: 10,
  skuId: undefined,
  warehouseId: undefined,
  itemName: undefined,
  itemCode: undefined,
  skuName: undefined,
  skuCode: undefined,
  minQuantity: undefined
})

/** 查询库存列表 */
const getList = async () => {
  let query = {...queryParams.value}
  if (filterable.value) {
    query.minQuantity = 1
  } else {
    query.minQuantity = undefined
  }
  loading.value = true;
  const res = await listInventoryBoard(query,queryType.value);
  inventoryList.value = res.rows;
  inventoryList.value.forEach(it => {
    if (queryType.value == "warehouse") {
      it.warehouseIdAndItemId = it.warehouseId + '-' + it.item.id
    } else if (queryType.value == "item") {
      it.itemId = it.item.id
      it.skuIdAndWarehouseId = it.skuId + '-' + it.warehouseId
    }
  })
  total.value = res.total;
  loading.value = false;
}

/** 搜索按钮操作 */
const handleQuery = () => {
  queryParams.value.pageNum = 1;
  getList();
}

/** 重置按钮操作 */
const resetQuery = () => {
  proxy.resetForm("queryRef");
  handleQuery();
}

/** 导出按钮操作 */
function handleExport() {
  const query = { ...queryParams.value }
  if (filterable.value) {
    query.minQuantity = 1
  } else {
    query.minQuantity = undefined
  }
  proxy.download('/wms/inventory/export', query, `inventory_${new Date().getTime()}.xlsx`)
}
const calcSubtotal = (row) => {
  const tempList = inventoryList.value.filter(it => it.itemId === row.itemId)
  let sum = 0
  tempList.forEach(it => {
    sum += Number(it.quantity)
  })
  return sum
}

const handleSortTypeChange = (e) => {
  if (e === "warehouse") {
    rowSpanArray.value = ['warehouseId', 'warehouseIdAndItemId']
  }  else if (e === "item") {
    rowSpanArray.value = ['itemId', 'skuId','skuIdAndWarehouseId']
  }
  queryParams.value.pageNum = 1;
  getList()
}

const handleChangeFilterZero = (e) => {
  queryParams.value.pageNum = 1;
  getList()
}

// 库位明细弹窗
const locationDialogVisible = ref(false)
const locationLoading = ref(false)
const locationInventoryList = ref([])
const locationDialogTitle = ref('')

// 库位库存总量（弹窗中所有库位的库存之和）
const locationTotalQuantity = computed(() => {
  return locationInventoryList.value.reduce((sum, item) => {
    return sum + Number(item.quantity || 0)
  }, 0)
})

/** 查看库位明细 */
const handleViewLocation = (row) => {
  const skuId = row.skuId || row.itemSku?.id
  if (!skuId) {
    proxy.$modal.msgWarning('无法获取SKU信息')
    return
  }
  const itemName = row.item?.itemName || ''
  const skuName = row.itemSku?.skuName || ''
  locationDialogTitle.value = `商品：${itemName} / 规格：${skuName}`
  locationDialogVisible.value = true
  locationLoading.value = true
  locationInventoryList.value = []
  listInventoryBySku(skuId).then(res => {
    locationInventoryList.value = res.data || []
  }).catch(() => {
    locationInventoryList.value = []
  }).finally(() => {
    locationLoading.value = false
  })
}

onMounted(() => {
  getList();
});
</script>
<style>
.el-statistic__content {
  font-size: 14px;
}
.el-table .vertical-top-cell {
  vertical-align: top
}
.mb10 {
  margin-bottom: 10px;
}
.mt10 {
  margin-top: 10px;
}
</style>
