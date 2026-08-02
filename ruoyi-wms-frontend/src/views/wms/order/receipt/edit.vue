<template>
  <div>
    <div class="receipt-order-edit-wrapper app-container" style="margin-bottom: 60px" v-loading="loading">
      <el-card header="入库单基本信息">
        <el-form label-width="108px" :model="form" ref="receiptForm" :rules="rules">
          <el-row :gutter="24">
            <el-col :span="11">
              <el-form-item label="入库单号" prop="orderNo">
                <el-input class="w200" v-model="form.orderNo" placeholder="入库单号" :disabled="form.id"></el-input>
              </el-form-item>
            </el-col>
            <el-col :span="6">
              <el-form-item label="仓库" prop="warehouseId">
                <el-select v-model="form.warehouseId" placeholder="请选择仓库" filterable>
                  <el-option v-for="item in useWmsStore().warehouseList" :key="item.id" :label="item.warehouseName" :value="item.id"/>
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="6">
              <el-form-item label="总数量" prop="totalQuantity">
                <el-input-number style="width:100%" v-model="form.totalQuantity" :controls="false" :precision="0" :disabled="true"></el-input-number>
              </el-form-item>
            </el-col>
          </el-row>
          <el-row :gutter="24">
            <el-col :span="11">
              <el-form-item label="入库类型" prop="optType">
                <el-radio-group v-model="form.optType">
                  <el-radio-button
                    v-for="item in wms_receipt_type"
                    :key="item.value"
                    :label="item.value"
                    >{{ item.label }}</el-radio-button
                  >
                </el-radio-group>
              </el-form-item>
            </el-col>
            <el-col :span="6">
              <el-form-item label="供应商" prop="merchantId">
                <el-select v-model="form.merchantId" placeholder="请选择供应商" clearable filterable>
                  <el-option v-for="item in useWmsStore().merchantList" :key="item.id" :label="item.merchantName" :value="item.id"/>
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
                  <el-input-number style="width:100%" v-model="form.totalAmount" :precision="2" :min="0"></el-input-number>
                </el-form-item>
                <el-button link type="primary" @click="handleAutoCalc" style="line-height: 32px">自动计算</el-button>
              </div>
            </el-col>
          </el-row>
        </el-form>
      </el-card>
      <el-card header="商品明细" class="mt10">
        <div class="receipt-order-content">
          <div class="flex-space-between mb8">
            <div style="display: flex; align-items: center; gap: 24px">
              <div>
                <span>审批 | 一物一码：</span>
                <el-switch
                  :before-change="goSaasTip"
                  class="mr10 ml10"
                  inline-prompt
                  size="large"
                  v-model="mode"
                  :active-value="true"
                  :inactive-value="false"
                  active-text="开启"
                  inactive-text="关闭"
                />
              </div>
              <div>
                <span>自动选位：</span>
                <el-switch
                  class="mr10 ml10"
                  inline-prompt
                  size="large"
                  v-model="autoTarget"
                  :active-value="true"
                  :inactive-value="false"
                  active-text="开启"
                  inactive-text="关闭"
                  @change="handleAutoTargetChange"
                />
                <span style="color: #909399; font-size: 12px">自动分配最小空闲库位</span>
              </div>
            </div>
            <div style="display: flex; gap: 8px; align-items: center">
              <el-button type="danger" plain size="default" @click="handleBatchDelete" icon="Delete" :disabled="!selectedRows.length">批量删除</el-button>
              <el-button type="warning" plain size="default" @click="openBatchLocationDialog" icon="LocationInformation" :disabled="!selectedRows.length">批量修改库位</el-button>
              <el-button type="success" plain size="default" @click="handleGenerateBatchNo" icon="MagicStick">自动生成批次号</el-button>
              <el-popover
                placement="left"
                title="提示"
                :width="200"
                trigger="hover"
                :disabled="form.warehouseId"
                content="请先选择仓库！"
              >
                <template #reference>
                  <el-button type="primary" plain="plain" size="default" @click="showAddItem" icon="Plus" :disabled="!form.warehouseId">添加商品</el-button>
                </template>
              </el-popover>
            </div>
          </div>
          <el-table :data="form.details" border empty-text="暂无商品明细" ref="detailsTableRef" @selection-change="handleSelectionChange">
            <el-table-column type="selection" width="55" />
            <el-table-column label="商品信息" prop="itemSku.itemName">
              <template #default="{ row }">
                <div>{{ row.item.itemName + (row.item.itemCode ? ('(' + row.item.itemCode + ')') : '') }}</div>
                <div v-if="row.item.itemBrand">品牌：{{ useWmsStore().itemBrandMap.get(row.item.itemBrand).brandName }}</div>
              </template>
            </el-table-column>
            <el-table-column label="规格信息">
              <template #default="{ row }">
                <div>{{ row.itemSku.skuName }}</div>
                <div v-if="row.itemSku.barcode">条码：{{row.itemSku.barcode}}</div>
              </template>
            </el-table-column>
            <el-table-column label="数量" prop="quantity" width="180">
              <template #default="scope">
                <el-input-number
                  v-model="scope.row.quantity"
                  placeholder="数量"
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
            <el-table-column label="批次号" width="180">
              <template #default="scope">
                <el-input v-model="scope.row.batchNo" placeholder="批次号"></el-input>
              </template>
            </el-table-column>
            <el-table-column label="生产日期" width="180">
              <template #default="scope">
                <el-date-picker
                  v-model="scope.row.productionDate"
                  type="date"
                  value-format="YYYY-MM-DD"
                  placeholder="选择生产日期"
                  style="width: 100%"
                ></el-date-picker>
              </template>
            </el-table-column>
            <el-table-column label="过期日期" width="180">
              <template #default="scope">
                <el-date-picker
                  v-model="scope.row.expiryDate"
                  type="date"
                  value-format="YYYY-MM-DD"
                  placeholder="选择过期日期"
                  style="width: 100%"
                ></el-date-picker>
              </template>
            </el-table-column>
            <el-table-column label="容器号" width="200">
              <template #default="scope">
                <div style="display: flex; align-items: center">
                  <el-input v-model="scope.row.containerNo" placeholder="自动生成" :disabled="true" style="flex: 1"></el-input>
                  <el-button link type="primary" @click="handleGenerateContainerNo(scope.$index)" style="margin-left: 4px">生成</el-button>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="起点库位" width="160">
              <template #default="scope">
                <el-select v-model="scope.row.sourceLocation" placeholder="请选择起点" clearable filterable>
                  <el-option v-for="item in receiptStartList" :key="item.locationCode" :label="item.locationCode" :value="item.locationCode"/>
                </el-select>
              </template>
            </el-table-column>
            <el-table-column label="目标库位号" width="160">
              <template #default="scope">
                <el-select v-model="scope.row.targetLocation" placeholder="请选择目标库位" clearable filterable :disabled="autoTarget">
                  <el-option v-for="item in sortedEmptyStorageList" :key="item.locationCode" :label="item.locationCode" :value="item.locationCode"/>
                </el-select>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="100" align="right" fixed="right">
              <template #default="scope">
                <el-button icon="Delete" type="danger" plain size="small" @click="handleDeleteDetail(scope.row, scope.$index)" link>删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-card>
      <SkuSelect
        ref="skuSelectRef"
        :model-value="skuSelectShow"
        :selected-sku="selectedSku"
        @handleOkClick="handleOkClick"
        @handleCancelClick="skuSelectShow = false"
        :size="'80%'"
      />
      <el-dialog v-model="batchLocationVisible" title="批量修改库位" width="440px" append-to-body>
        <el-form label-width="90px">
          <el-form-item label="库位类型">
            <el-radio-group v-model="batchLocationType">
              <el-radio-button label="source">起点库位</el-radio-button>
              <el-radio-button label="target" :disabled="autoTarget">目标库位号</el-radio-button>
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
          <el-button @click="doWarehousing" type="primary" class="ml10">完成入库</el-button>
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

<script setup name="ReceiptOrderEdit">
import {computed, getCurrentInstance, onMounted, reactive, ref, toRef, toRefs, watch} from "vue";
import {addReceiptOrder, getReceiptOrder, updateReceiptOrder, warehousing} from "@/api/wms/receiptOrder";
import {ElMessage, ElMessageBox} from "element-plus";
import SkuSelect from "../../../components/SkuSelect.vue";
import {useRoute} from "vue-router";
import {useWmsStore} from '@/store/modules/wms'
import { numSub, generateNo } from '@/utils/ruoyi'
import { delReceiptOrderDetail } from '@/api/wms/receiptOrderDetail'
import {getWarehouseAndSkuKey} from "@/utils/wmsUtil";
import { listReceiptStart, listEmptyStorage, generateContainerNo } from "@/api/wms/location";
import { cancelTaskByOrder } from "@/api/wms/rcs";

const {proxy} = getCurrentInstance();
const { wms_receipt_type } = proxy.useDict("wms_receipt_type");
const selectedSku = ref([])
const mode = ref(false)
const autoTarget = ref(false)
const loading = ref(false)
const skuSelectRef = ref(null)
const receiptStartList = ref([])
const emptyStorageList = ref([])
const detailsTableRef = ref(null)
const selectedRows = ref([])

// 排序后的空库位列表（按编号数字升序：A1 < A2 < A10）
const sortedEmptyStorageList = computed(() => {
  return [...emptyStorageList.value].sort((a, b) => {
    const na = parseInt((a.locationCode || '').replace(/[^0-9]/g, '')) || 0
    const nb = parseInt((b.locationCode || '').replace(/[^0-9]/g, '')) || 0
    return na - nb
  })
})

// 获取下一个可用的最小空闲库位（排除已分配给当前单据其他明细的库位）
const getNextEmptyLocation = () => {
  const usedLocations = new Set(
    form.value.details
      .map(d => d.targetLocation)
      .filter(Boolean)
  )
  const sorted = sortedEmptyStorageList.value
  for (const loc of sorted) {
    if (!usedLocations.has(loc.locationCode)) {
      return loc.locationCode
    }
  }
  return undefined
}

// 自动选位开关切换处理
const handleAutoTargetChange = (val) => {
  if (val) {
    // 开启自动选位：为所有未分配目标库位的明细自动分配
    form.value.details.forEach((detail, index) => {
      if (!detail.targetLocation) {
        const loc = getNextEmptyLocation()
        if (loc) {
          detail.targetLocation = loc
        }
      }
    })
    if (sortedEmptyStorageList.value.length === 0) {
      ElMessage.warning('当前无空闲库位')
    }
  }
}
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
const data = reactive({
  form: {...initFormData},
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    orderNo: undefined,
    optType: undefined,
    bizOrderNo: undefined,
    totalAmount: undefined,
    orderStatus: undefined,
  },
  rules: {
    orderNo: [
      {required: true, message: "入库单号不能为空", trigger: "blur"}
    ],
    warehouseId: [
      {required: true, message: "请选择仓库", trigger: ['blur', 'change']}
    ]
  }
});
const { form, rules} = toRefs(data);

const cancel = async () => {
  await proxy?.$modal.confirm('确认取消编辑入库单吗？');
  close()
}
const close = () => {
  const obj = {path: "/receiptOrder"};
  proxy?.$tab.closeOpenPage(obj);
}
const skuSelectShow = ref(false)

// 选择商品 start
const showAddItem = () => {
  skuSelectRef.value.getList()
  skuSelectShow.value = true
}
// 选择成功
const handleOkClick = (item) => {
  skuSelectShow.value = false
  selectedSku.value = [...item]
  item.forEach((it) => {
    if (!form.value.details.find(detail => detail.itemSku.id === it.id)) {
      // 如果开启了自动选位，自动分配最小空闲库位
      const autoLoc = autoTarget.value ? getNextEmptyLocation() : undefined
      form.value.details.push(
        {
          itemSku: it.itemSku,
          item: it.item,
          amount: undefined,
          quantity: it.quantity,
          warehouseId: form.value.warehouseId,
          containerNo: undefined, // 容器号待自动生成
          targetLocation: autoLoc, // 自动选位时分配目标库位
          batchNo: undefined,
          productionDate: undefined,
          expiryDate: undefined,
        }
      )
      // 自动为新添加的明细生成容器号
      const newIndex = form.value.details.length - 1
      generateContainerNoForDetail(newIndex)
    }
  })
}

// 为指定明细行生成唯一容器号
const generateContainerNoForDetail = (index) => {
  generateContainerNo().then(res => {
    if (res.code === 200 && form.value.details[index]) {
      form.value.details[index].containerNo = res.data
    }
  })
}

// 手动点击生成按钮
const handleGenerateContainerNo = (index) => {
  generateContainerNoForDetail(index)
}
// 选择商品 end

// 自动生成批次号（格式：BATCH + YYYYMMDD + 三位序号）
const handleGenerateBatchNo = () => {
  if (!form.value.details?.length) {
    return ElMessage.warning('请先添加商品明细')
  }
  const today = new Date()
  const dateStr = today.getFullYear().toString() +
    String(today.getMonth() + 1).padStart(2, '0') +
    String(today.getDate()).padStart(2, '0')
  form.value.details.forEach((detail, index) => {
    const seq = String(index + 1).padStart(3, '0')
    detail.batchNo = 'BATCH' + dateStr + seq
  })
  ElMessage.success('批次号生成成功')
}

// 初始化receipt-order-form ref
const receiptForm = ref()

const save = async () => {
  await proxy?.$modal.confirm('确认暂存入库单吗？');
  doSave()
}

const getParamsBeforeSave = (orderStatus) => {
  let details = []
  if (form.value.details?.length) {
    details = form.value.details.map(it => {
      return {
        id: it.id,
        skuId: it.itemSku.id,
        amount: it.amount,
        quantity: it.quantity,
        warehouseId: form.value.warehouseId,
        containerNo: it.containerNo,
        sourceLocation: it.sourceLocation,
        targetLocation: it.targetLocation,
        batchNo: it.batchNo,
        productionDate: it.productionDate,
        expiryDate: it.expiryDate,
      }
    })
  }

  return {
    id: form.value.id,
    orderNo: form.value.orderNo,
    orderStatus,
    optType: form.value.optType,
    merchantId: form.value.merchantId,
    bizOrderNo: form.value.bizOrderNo,
    remark: form.value.remark,
    totalAmount: form.value.totalAmount,
    totalQuantity: form.value.totalQuantity,
    warehouseId: form.value.warehouseId,
    details: details
  }
}

const doSave = async (orderStatus = 0) => {
  //验证receiptForm表单
  receiptForm.value?.validate((valid) => {
    // 校验
    if (!valid) {
      return ElMessage.error('请填写必填项')
    }
    const params = getParamsBeforeSave(orderStatus)
    loading.value = true
    if (params.id) {
      updateReceiptOrder(params).then((res) => {
        if (res.code === 200) {
          ElMessage.success(res.msg)
          close()
        } else {
          ElMessage.error(res.msg)
        }
      }).finally(() => {
        loading.value = false
      })
    } else {
      addReceiptOrder(params).then((res) => {
        if (res.code === 200) {
          ElMessage.success(res.msg)
          close()
        } else {
          ElMessage.error(res.msg)
        }
      }).finally(() => {
        loading.value = false
      })
    }
  })
}

const doWarehousing = async () => {
  await proxy?.$modal.confirm('确认入库吗？');
  receiptForm.value?.validate((valid) => {
    // 校验
    if (!valid) {
      return ElMessage.error('请填写必填项')
    }

    if (!form.value.details?.length) {
      return ElMessage.error('请选择商品')
    }
    if (form.value.details?.length) {
      const invalidQuantityList = form.value.details.filter(it => !it.quantity)
      if (invalidQuantityList?.length) {
        return ElMessage.error('请选择数量')
      }
    }
    const params = getParamsBeforeSave(2);
    loading.value = true
    warehousing(params).then((res) => {
      if (res.code === 200) {
        ElMessage.success('入库中，等待AGV回调完成')
        close()
      } else {
        ElMessage.error(res.msg)
      }
    }).finally(() => {
      loading.value = false
    })
  })
}

const updateToInvalid = async () => {
  await proxy?.$modal.confirm('确认作废入库单吗？');
  doSave(-1)
}

const handleCancelRcsTask = async () => {
  await proxy?.$modal.confirm('确认撤销该入库单关联的 RCS 任务吗？');
  const details = (form.value.details || []).map(it => ({
    containerNo: it.containerNo,
    sourceLocation: it.sourceLocation,
    targetLocation: it.targetLocation,
  }))
  loading.value = true
  cancelTaskByOrder({
    orderNo: form.value.orderNo,
    taskType: 0,
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
    form.value.orderNo = 'RK' + generateNo()
  }
  loadLocationOptions()
})

const loadLocationOptions = () => {
  listReceiptStart().then(res => {
    if (res.code === 200) {
      receiptStartList.value = res.data || []
    }
  })
  listEmptyStorage().then(res => {
    if (res.code === 200) {
      emptyStorageList.value = res.data || []
    }
  })
}


// 获取入库单详情
const loadDetail = (id) => {
  loading.value = true
  getReceiptOrder(id).then((response) => {
    form.value = {...response.data}
    if (response.data.details?.length) {
      selectedSku.value = response.data.details.map(it => {
        return {
          id: it.skuId
        }
      })
    }
    Promise.resolve();
  }).then(() => {
  }).finally(() => {
    loading.value = false
  })
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

const handleDeleteDetail = (row, index) => {
  if (row.id) {
    proxy.$modal.confirm('确认删除本条商品明细吗？如确认会立即执行！').then(function () {
      loading.value = true
      return delReceiptOrderDetail(row.id);
    }).then(() => {
      form.value.details.splice(index, 1)
      proxy.$modal.msgSuccess("删除成功");
    }).finally(() => {
      loading.value = false
    });
  } else {
    form.value.details.splice(index, 1)
  }
  const indexOfSelected = selectedSku.value.findIndex(it => row.itemSku.id=== it.id)
  selectedSku.value.splice(indexOfSelected, 1)
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
    const tasks = idsToDelete.length ? idsToDelete.map(id => delReceiptOrderDetail(id)) : [Promise.resolve()]
    return Promise.all(tasks)
  }).then(() => {
    const toDeleteSet = new Set(rowsToDelete)
    form.value.details = form.value.details.filter(d => !toDeleteSet.has(d))
    const deletedSkuIds = new Set(rowsToDelete.map(r => r.itemSku?.id).filter(Boolean))
    selectedSku.value = selectedSku.value.filter(s => !deletedSkuIds.has(s.id))
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
const batchLocationType = ref('target')
const batchLocationValue = ref('')
const batchLocationOptions = computed(() => {
  return batchLocationType.value === 'source' ? receiptStartList.value : sortedEmptyStorageList.value
})
const openBatchLocationDialog = () => {
  if (!selectedRows.value.length) {
    return ElMessage.warning('请先选择要修改库位的商品明细')
  }
  batchLocationValue.value = ''
  if (autoTarget.value && batchLocationType.value === 'target') {
    batchLocationType.value = 'source'
  }
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
</style>
