<template>
  <div>
    <div class="receipt-order-edit-wrapper app-container" style="margin-bottom: 60px" v-loading="loading">
      <el-card header="出库单基本信息">
        <el-form label-width="108px" :model="form" ref="shipmentForm" :rules="rules">
          <el-row :gutter="24">
            <el-col :span="11">
              <el-form-item label="出库单号" prop="orderNo">
                <el-input class="w200" v-model="form.orderNo" placeholder="出库单号"
                          :disabled="form.id"></el-input>
              </el-form-item>
            </el-col>
            <el-col :span="6">
              <el-form-item label="仓库" prop="warehouseId">
                <el-select v-model="form.warehouseId" placeholder="请选择仓库" @change="handleChangeWarehouse"
                           filterable>
                  <el-option v-for="item in useWmsStore().warehouseList" :key="item.id" :label="item.warehouseName"
                             :value="item.id"/>
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="6">
              <el-form-item label="总数量" prop="totalQuantity">
                <el-input-number style="width: 100%" v-model="form.totalQuantity" :controls="false" :precision="0"
                                 :disabled="true"></el-input-number>
              </el-form-item>
            </el-col>
          </el-row>
          <el-row :gutter="24">
            <el-col :span="11">
              <el-form-item label="出库类型" prop="optType">
                <el-radio-group v-model="form.optType">
                  <el-radio-button
                    v-for="item in wms_shipment_type"
                    :key="item.value"
                    :label="item.value"
                  >{{ item.label }}
                  </el-radio-button
                  >
                </el-radio-group>
              </el-form-item>
            </el-col>
            <el-col :span="6">
              <el-form-item label="客户" prop="merchantId">
                <el-select v-model="form.merchantId" placeholder="请选择客户" clearable filterable>
                  <el-option v-for="item in useWmsStore().merchantList" :key="item.id" :label="item.merchantName"
                             :value="item.id"/>
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="6">
              <el-form-item label="业务单号" prop="bizOrderNo">
                <el-input v-model="form.bizOrderNo" placeholder="请输入业务单号"></el-input>
              </el-form-item>
            </el-col>
          </el-row>
          <el-row :gutter="24">
            <el-col :span="11">
              <el-form-item label="备注" prop="remark">
                <el-input
                  v-model="form.remark"
                  placeholder="备注...100个字符以内"
                  rows="4"
                  maxlength="100"
                  type="textarea"
                  show-word-limit="show-word-limit"
                ></el-input>
              </el-form-item>
            </el-col>
            <el-col :span="6">
              <div style="display: flex;align-items: start">
                <el-form-item label="总金额" prop="totalAmount">
                  <el-input-number style="width: 100%;" v-model="form.totalAmount" :precision="2" :min="0"></el-input-number>
                </el-form-item>
                <el-button link type="primary" @click="handleAutoCalc" style="line-height: 32px">自动计算
                </el-button>
              </div>
            </el-col>
          </el-row>
        </el-form>
      </el-card>
      <el-card header="商品明细" class="mt10">
        <div class="receipt-order-content">
          <div class="flex-space-between mb8">
            <div style="display: flex; align-items: center; gap: 12px">
              <span>审批 | 一物一码：</span>
              <el-switch
                :before-change="goSaasTip"
                class="mr10 ml10"
                inline-prompt
                size="large"
                :active-value="true"
                :inactive-value="false"
                active-text="开启"
                inactive-text="关闭"
              />
            </div>
            <div style="display: flex; gap: 8px; align-items: center">
              <el-button type="danger" plain size="default" @click="handleBatchDelete" icon="Delete" :disabled="!selectedRows.length">批量删除</el-button>
              <el-button type="warning" plain size="default" @click="openBatchLocationDialog" icon="LocationInformation" :disabled="!selectedRows.length">批量修改库位</el-button>
              <el-popover
                placement="top"
                title="自动拆分说明"
                :width="300"
                trigger="hover"
                content="根据各库位剩余库存，从最小库位号开始自动拆分出库数量到多个起点库位"
              >
                <template #reference>
                  <el-button type="warning" plain size="default" @click="handleAutoSplit" icon="Switch"
                             :disabled="!form.details?.length">自动拆分
                  </el-button>
                </template>
              </el-popover>
              <el-popover
                placement="left"
                title="提示"
                :width="200"
                trigger="hover"
                :disabled="form.warehouseId"
                content="请先选择仓库！"
              >
                <template #reference>
                  <el-button type="primary" plain="plain" size="default" @click="showAddItem" icon="Plus"
                             :disabled="!form.warehouseId">添加商品
                  </el-button>
                </template>
              </el-popover>
            </div>
          </div>
          <el-table :data="form.details" border empty-text="暂无商品明细" ref="detailsTableRef" @selection-change="handleSelectionChange">
            <el-table-column type="selection" width="55" />
            <el-table-column label="商品信息" prop="itemSku.itemName">
              <template #default="{ row }">
                <div>{{
                    row.item.itemName + (row.item.itemCode ? ('(' + row.item.itemCode + ')') : '')
                  }}
                </div>
                <div v-if="row.item.itemBrand">
                  品牌：{{ useWmsStore().itemBrandMap.get(row.item.itemBrand).brandName }}
                </div>
              </template>
            </el-table-column>
            <el-table-column label="规格信息">
              <template #default="{ row }">
                <div>{{ row.itemSku.skuName }}</div>
                <div v-if="row.itemSku.barcode">条码：{{row.itemSku.barcode}}</div>
              </template>
            </el-table-column>
            <el-table-column label="出库数量" prop="quantity" width="180">
              <template #default="scope">
                <el-input-number
                  v-model="scope.row.quantity"
                  placeholder="出库数量"
                  :min="1"
                  :precision="0"
                  @change="handleChangeQuantity"
                ></el-input-number>
              </template>
            </el-table-column>
            <el-table-column label="金额" prop="amount" width="180">
              <template #default="scope">
                <el-input-number
                  v-model="scope.row.amount"
                  placeholder="金额"
                  :precision="2"
                  :min="0"
                  :max="2147483647"
                ></el-input-number>
              </template>
            </el-table-column>
            <el-table-column label="容器号" width="160">
              <template #default="scope">
                <el-input v-model="scope.row.containerNo" placeholder="选择库位后自动带出" :disabled="true"></el-input>
              </template>
            </el-table-column>
            <el-table-column label="起点库位" width="160">
              <template #default="scope">
                <el-select v-model="scope.row.sourceLocation" placeholder="请选择起点库位" clearable filterable @change="(val) => handleSourceLocationChange(val, scope.$index)">
                  <el-option v-for="item in occupiedStorageList" :key="item.locationCode" :label="item.locationCode" :value="item.locationCode"/>
                </el-select>
              </template>
            </el-table-column>
            <el-table-column label="目标库位号" width="160">
              <template #default="scope">
                <el-select v-model="scope.row.targetLocation" placeholder="请选择目标库位" clearable filterable>
                  <el-option v-for="item in shipmentEndList" :key="item.locationCode" :label="item.locationCode" :value="item.locationCode"/>
                </el-select>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="100" align="right" fixed="right">
              <template #default="scope">
                <el-button icon="Delete" type="danger" plain size="small"
                           @click="handleDeleteDetail(scope.row, scope.$index)" link>删除
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-card>
      <InventorySelect
        ref="inventorySelectRef"
        :model-value="inventorySelectShow"
        @handleOkClick="handleOkClick"
        @handleCancelClick="inventorySelectShow = false"
        :size="'90%'"
        :select-warehouse-disable="false"
        :warehouse-id="form.warehouseId"
        :selected-inventory="selectedInventory"
      />
      <el-dialog v-model="batchLocationVisible" title="批量修改库位" width="440px" append-to-body>
        <el-form label-width="90px">
          <el-form-item label="库位类型">
            <el-radio-group v-model="batchLocationType">
              <el-radio-button label="source">起点库位</el-radio-button>
              <el-radio-button label="target">目标库位号</el-radio-button>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="库位">
            <el-select v-model="batchLocationValue" placeholder="请选择库位" clearable filterable style="width: 100%">
              <el-option v-for="item in batchLocationOptions" :key="item.locationCode" :label="item.locationCode" :value="item.locationCode"/>
            </el-select>
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="batchLocationVisible = false">取消</el-button>
          <el-button type="primary" @click="confirmBatchModifyLocation">确定</el-button>
        </template>
      </el-dialog>
    </div>
    <div class="footer-global">
      <div class="btn-box">
        <div>
          <el-button @click="doShipment" type="primary" class="ml10">完成出库</el-button>
          <el-button @click="handleCancelRcsTask" type="warning" v-if="form.id">撤销任务</el-button>
          <el-button @click="updateToInvalid" type="danger" v-if="form.id">作废</el-button>
        </div>
        <div>
          <el-button @click="save" type="primary">暂存</el-button>
          <el-button @click="cancel" class="mr10">取消</el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup name="ShipmentOrderEdit">
import {computed, getCurrentInstance, onMounted, reactive, ref, toRef, toRefs, watch} from "vue";
import {addShipmentOrder, getShipmentOrder, updateShipmentOrder, shipment} from "@/api/wms/shipmentOrder";
import {delShipmentOrderDetail} from "@/api/wms/shipmentOrderDetail";
import {ElMessage, ElMessageBox} from "element-plus";
import {useRoute} from "vue-router";
import {useWmsStore} from '@/store/modules/wms'
import {numSub, generateNo} from '@/utils/ruoyi'
import InventorySelect from "@/views/components/InventorySelect.vue";
import {getWarehouseAndSkuKey} from "@/utils/wmsUtil"
import { listOccupiedStorage, listShipmentEnd, getContainerNoByLocation, listInventoryBySku } from "@/api/wms/location";
import { cancelTaskByOrder } from "@/api/wms/rcs";

const {proxy} = getCurrentInstance();
const {wms_shipment_type} = proxy.useDict("wms_shipment_type");

const loading = ref(false)
const initFormData = {
  id: undefined,
  orderNo: undefined,
  optType: "2",
  merchantId: undefined,
  bizOrderNo: undefined,
  totalAmount: undefined,
  orderStatus: 0,
  remark: undefined,
  warehouseId: undefined,
  totalQuantity: 0,
  details: [],
}
const inventorySelectRef = ref(null)
const selectedInventory = ref([])
const occupiedStorageList = ref([])
const shipmentEndList = ref([])
const detailsTableRef = ref(null)
const selectedRows = ref([])
const data = reactive({
  form: {...initFormData},
  rules: {
    orderNo: [
      {required: true, message: "出库单号不能为空", trigger: "blur"}
    ],
    optType: [
      {required: true, message: "出库类型不能为空", trigger: "change"}
    ],
    warehouseId: [
      {required: true, message: "请选择仓库", trigger: ['blur', 'change']}
    ],
  }
});
const {form, rules} = toRefs(data);
const cancel = async () => {
  await proxy?.$modal.confirm('确认取消编辑出库单吗？');
  close()
}
const close = () => {
  const obj = {path: "/shipmentOrder"};
  proxy?.$tab.closeOpenPage(obj);
}
const inventorySelectShow = ref(false)

// 选择商品 start
const showAddItem = () => {
  inventorySelectRef.value.getList()
  inventorySelectShow.value = true
}
// 选择成功
const handleOkClick = (item) => {
  inventorySelectShow.value = false
  selectedInventory.value = [...item]
  item.forEach(it => {
    if (!form.value.details.find(detail => getWarehouseAndSkuKey(detail) === getWarehouseAndSkuKey(it))) {
      form.value.details.push(
        {
          itemSku: it.itemSku,
          item: it.item,
          skuId: it.skuId,
          amount: undefined,
          quantity: undefined,
          warehouseId: form.value.warehouseId,
          inventoryId: it.id,
        })
    }
  })
}
// 选择商品 end

// 初始化receipt-order-form ref
const shipmentForm = ref()

const save = async () => {
  await proxy?.$modal.confirm('确认暂存出库单吗？');
  doSave()
}

const getParamsBeforeSave = (orderStatus) => {
  let details = []
  if (form.value.details?.length) {
    // 构建参数
    details = form.value.details.map(it => {
      return {
        id: it.id,
        receiptOrderId: form.value.id,
        skuId: it.skuId,
        amount: it.amount,
        quantity: it.quantity,
        warehouseId: form.value.warehouseId,
        containerNo: it.containerNo,
        sourceLocation: it.sourceLocation,
        targetLocation: it.targetLocation,
      }
    })
  }

  return {
    id: form.value.id,
    orderNo: form.value.orderNo,
    optType: form.value.optType,
    merchantId: form.value.merchantId,
    bizOrderNo: form.value.bizOrderNo,
    orderStatus,
    remark: form.value.remark,
    totalAmount: form.value.totalAmount,
    totalQuantity: form.value.totalQuantity,
    warehouseId: form.value.warehouseId,
    details: details
  }
}

const doSave = (orderStatus = 0) => {
  //验证shipmentForm表单
  shipmentForm.value?.validate((valid) => {
    // 校验
    if (!valid) {
      return ElMessage.error('请填写必填项')
    }

    //('提交前校验',form.value)
    const params = getParamsBeforeSave(orderStatus)

    loading.value = true
    if (params.id) {
      updateShipmentOrder(params).then((res) => {
        if (res.code === 200) {
          ElMessage.success(res.msg)
          close()
        } else {
          ElMessage.error(res.msg)
        }
      }).finally(()=>{
        loading.value = false
      })
    } else {
      addShipmentOrder(params).then((res) => {
        if (res.code === 200) {
          ElMessage.success(res.msg)
          close()
        } else {
          ElMessage.error(res.msg)
        }
      }).finally(()=>{
        loading.value = false
      })
    }
  })
}

const doShipment = async () => {
  await proxy?.$modal.confirm('确认出库吗？');
  shipmentForm.value?.validate((valid) => {
    // 校验
    if (!valid) {
      return ElMessage.error('请填写必填项')
    }
    if (!form.value.details?.length) {
      return ElMessage.error('请选择商品')
    }
    const invalidQuantityList = form.value.details.filter(it => !it.quantity)
    if (invalidQuantityList?.length) {
      return ElMessage.error('请选择数量')
    }
    const params = getParamsBeforeSave(2)

    loading.value = true
    shipment(params).then((res) => {
      if (res.code === 200) {
        ElMessage.success('出库中，等待AGV回调完成')
        close()
      } else {
        ElMessage.error(res.msg)
      }
    }).finally(()=>{
      loading.value = false
    })
  })
}

const updateToInvalid = async () => {
  await proxy?.$modal.confirm('确认作废出库单吗？');
  doSave(-1)
}

const handleCancelRcsTask = async () => {
  await proxy?.$modal.confirm('确认撤销该出库单关联的 RCS 任务吗？');
  const details = (form.value.details || []).map(it => ({
    containerNo: it.containerNo,
    sourceLocation: it.sourceLocation,
    targetLocation: it.targetLocation,
  }))
  loading.value = true
  cancelTaskByOrder({
    orderNo: form.value.orderNo,
    taskType: 1,
    details: details
  }).then((res) => {
    if (res.code === 200) {
      ElMessage.success(res.msg || '撤销任务完成')
    } else {
      ElMessage.error(res.msg)
    }
  }).finally(() => {
    loading.value = false
  })
}

const route = useRoute();
onMounted(() => {
  const id = route.query && route.query.id;
  if (id) {
    loadDetail(id)
  } else {
    form.value.orderNo = 'CK' + generateNo()
  }
  loadLocationOptions()
})

const loadLocationOptions = () => {
  listOccupiedStorage().then(res => {
    if (res.code === 200) {
      occupiedStorageList.value = res.data || []
    }
  })
  listShipmentEnd().then(res => {
    if (res.code === 200) {
      shipmentEndList.value = res.data || []
    }
  })
}

// 选择起点库位后自动带出容器号
const handleSourceLocationChange = (val, index) => {
  if (val) {
    getContainerNoByLocation(val).then(res => {
      if (res.code === 200 && form.value.details[index]) {
        form.value.details[index].containerNo = res.data || ''
      }
    })
  } else {
    // 清空库位时也清空容器号
    if (form.value.details[index]) {
      form.value.details[index].containerNo = ''
    }
  }
}


// 获取入库单详情
const loadDetail = (id) => {
  loading.value = true
  getShipmentOrder(id).then((response) => {
    if (response.data.details?.length) {
      selectedInventory.value = response.data.details.map(it => {
        return {
          id: it.id,
          skuId: it.skuId,
          warehouseId: it.warehouseId
        }
      })
    }
    form.value = {...response.data}
    inventorySelectRef.value.setWarehouseId(form.value.warehouseId)
    Promise.resolve();
  }).then(() => {
  }).finally(() => {
    loading.value = false
  })
}

const handleChangeWarehouse = (e) => {
  form.value.details = []
  inventorySelectRef.value.setWarehouseId(form.value.warehouseId)
}

const handleChangeQuantity = () => {
  let sum = 0
  form.value.details.forEach(it => {
    if (it.quantity) {
      sum += Number(it.quantity)
    }
  })
  form.value.totalQuantity = sum
}

const handleAutoCalc = () => {
  let sum = undefined
  form.value.details.forEach(it => {
    if (it.amount >= 0) {
      if (!sum) {
        sum = 0
      }
      sum = numSub(sum, -Number(it.amount))
    }
  })
  form.value.totalAmount = sum
}

const handleAutoSplit = async () => {
  if (!form.value.details?.length) {
    return ElMessage.warning('请先添加商品')
  }

  // 校验是否已填写出库数量
  const noQuantityList = form.value.details.filter(it => !it.quantity || it.quantity <= 0)
  if (noQuantityList.length) {
    return ElMessage.warning('请先填写所有商品的出库数量')
  }

  loading.value = true
  const newDetails = []
  let splitCount = 0

  try {
    for (const detail of form.value.details) {
      const skuId = detail.skuId
      const needQty = Number(detail.quantity)

      // 查询该SKU在各库位的剩余库存（已按库位编号升序排列）
      const res = await listInventoryBySku(skuId)
      const locationInventory = res.data || []

      if (locationInventory.length === 0) {
        // 没有库存记录，保持原样
        newDetails.push(detail)
        ElMessage.warning(`商品[${detail.itemSku?.skuName || skuId}]无库位库存记录，未拆分`)
        continue
      }

      // 从最小库位开始分配
      let remaining = needQty
      const splitRows = []
      for (const loc of locationInventory) {
        if (remaining <= 0) break
        const available = Number(loc.quantity)
        if (available <= 0) continue

        const takeQty = Math.min(remaining, available)
        splitRows.push({
          ...detail,
          id: undefined, // 拆分行是新行，不保留原ID
          quantity: takeQty,
          sourceLocation: loc.locationCode,
          containerNo: loc.containerNo || ''
        })
        remaining -= takeQty
      }

      if (remaining > 0) {
        // 库存不足，将剩余数量分配到最后一个库位
        if (splitRows.length > 0) {
          splitRows[splitRows.length - 1].quantity += remaining
        } else {
          // 完全没有可用库存，保持原样
          newDetails.push(detail)
          continue
        }
      }

      splitCount += splitRows.length
      newDetails.push(...splitRows)
    }

    form.value.details = newDetails
    handleChangeQuantity()
    ElMessage.success(`自动拆分完成，共拆分为 ${splitCount} 条明细`)
  } catch (err) {
    ElMessage.error('自动拆分失败：' + (err.message || '未知错误'))
  } finally {
    loading.value = false
  }
}

const handleDeleteDetail = (row, index) => {
  if (row.id) {
    proxy.$modal.confirm('确认删除本条商品明细吗？如确认会立即执行！').then(function () {
      loading.value = true;
      return delShipmentOrderDetail(row.id);
    }).then(() => {
      form.value.details.splice(index, 1)
      proxy.$modal.msgSuccess("删除成功");
    }).finally(()=>{
      loading.value=false
    })
  } else {
    form.value.details.splice(index, 1)
  }
  const indexOfSelected = selectedInventory.value.findIndex(it => getWarehouseAndSkuKey(it) === getWarehouseAndSkuKey(row))
  selectedInventory.value.splice(indexOfSelected, 1)
}

// 多选回调
const handleSelectionChange = (rows) => {
  selectedRows.value = rows
}

// 批量删除明细
const handleBatchDelete = () => {
  if (!selectedRows.value.length) {
    return ElMessage.warning('请先选择要删除的商品明细')
  }
  const rowsToDelete = [...selectedRows.value]
  proxy.$modal.confirm(`确认删除选中的 ${rowsToDelete.length} 条商品明细吗？如确认会立即执行！`).then(() => {
    loading.value = true
    const idsToDelete = rowsToDelete.filter(r => r.id).map(r => r.id)
    const tasks = idsToDelete.length ? idsToDelete.map(id => delShipmentOrderDetail(id)) : [Promise.resolve()]
    return Promise.all(tasks)
  }).then(() => {
    const toDeleteSet = new Set(rowsToDelete)
    form.value.details = form.value.details.filter(d => !toDeleteSet.has(d))
    const deletedKeys = new Set(rowsToDelete.map(r => getWarehouseAndSkuKey(r)))
    selectedInventory.value = selectedInventory.value.filter(it => !deletedKeys.has(getWarehouseAndSkuKey(it)))
    selectedRows.value = []
    handleChangeQuantity()
    proxy.$modal.msgSuccess("批量删除成功")
  }).catch(() => {
  }).finally(() => {
    loading.value = false
  })
}

// 批量修改库位
const batchLocationVisible = ref(false)
const batchLocationType = ref('source')
const batchLocationValue = ref('')
const batchLocationOptions = computed(() => {
  return batchLocationType.value === 'source' ? occupiedStorageList.value : shipmentEndList.value
})
const openBatchLocationDialog = () => {
  if (!selectedRows.value.length) {
    return ElMessage.warning('请先选择要修改库位的商品明细')
  }
  batchLocationValue.value = ''
  batchLocationVisible.value = true
}
const confirmBatchModifyLocation = () => {
  if (!batchLocationValue.value) {
    return ElMessage.warning('请选择库位')
  }
  const field = batchLocationType.value === 'source' ? 'sourceLocation' : 'targetLocation'
  selectedRows.value.forEach(row => {
    row[field] = batchLocationValue.value
  })
  ElMessage.success('批量修改库位成功')
  batchLocationVisible.value = false
  selectedRows.value = []
  detailsTableRef.value?.clearSelection()
}
const goSaasTip = () => {
  ElMessageBox.alert('如需体验，请在公众号内回复：saas', '请去Saas版本体验', {
    confirmButtonText: '确定'
  })
  return false
}
</script>

<style lang="scss" scoped>
@import "@/assets/styles/variables.module";

.btn-box {
  width: calc(100% - #{$base-sidebar-width});
  display: flex;
  align-items: center;
  justify-content: space-between;
  float: right;
}

.el-statistic__content {
  font-size: 14px;
}
</style>
