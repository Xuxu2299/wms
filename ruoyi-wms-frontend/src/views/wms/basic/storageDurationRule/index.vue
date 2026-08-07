<template>
  <div class="app-container">
    <el-card>
      <el-form :model="queryParams" ref="queryFormRef" :inline="true" label-width="68px">
        <el-form-item label="商品规格" prop="skuId">
          <el-select
            v-model="queryParams.skuId"
            placeholder="请选择规格"
            clearable
            filterable
            style="width: 200px"
          >
            <el-option
              v-for="item in skuList"
              :key="item.skuId"
              :label="skuLabel(item)"
              :value="item.skuId"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="源仓库" prop="fromWarehouseId">
          <el-select v-model="queryParams.fromWarehouseId" placeholder="请选择源仓库" clearable filterable style="width: 180px">
            <el-option v-for="w in warehouseList" :key="w.id" :label="w.warehouseName" :value="w.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态" prop="enableStatus">
          <el-select v-model="queryParams.enableStatus" placeholder="请选择状态" clearable style="width: 120px">
            <el-option label="启用" :value="1" />
            <el-option label="停用" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
          <el-button icon="Refresh" @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>

      <el-row :gutter="10" class="mb8" type="flex" justify="space-between">
        <el-col :span="6"><span style="font-size: large">存储时长自动移库规则</span></el-col>
        <el-col :span="1.5">
          <el-button type="primary" plain icon="Plus" @click="handleAdd" v-hasPermi="['wms:storageDurationRule:edit']">新增</el-button>
        </el-col>
      </el-row>

      <el-table v-loading="loading" :data="ruleList" border class="mt20" empty-text="暂无规则">
        <el-table-column label="商品/规格" min-width="180">
          <template #default="scope">
            <span v-if="scope.row.skuId">{{ scope.row.itemName }} / {{ scope.row.skuName }}</span>
            <el-tag v-else type="info">全部规格</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="入库时长超过(天)" prop="thresholdDays" width="140" align="center" />
        <el-table-column label="源仓库" width="120">
          <template #default="scope">{{ warehouseName(scope.row.fromWarehouseId) }}</template>
        </el-table-column>
        <el-table-column label="目标仓库" width="120">
          <template #default="scope">{{ warehouseName(scope.row.toWarehouseId) }}</template>
        </el-table-column>
        <el-table-column label="启用状态" width="100" align="center">
          <template #default="scope">
            <el-switch
              v-model="scope.row.enableStatus"
              :active-value="1"
              :inactive-value="0"
              :disabled="!checkPermi(['wms:storageDurationRule:edit'])"
              @change="handleStatusChange(scope.row)"
            />
          </template>
        </el-table-column>
        <el-table-column label="创建时间" prop="createTime" width="180" />
        <el-table-column label="备注" prop="remark" min-width="120" show-overflow-tooltip />
        <el-table-column label="操作" align="right" class-name="small-padding fixed-width" width="220">
          <template #default="scope">
            <el-button link type="primary" icon="View" @click="handlePreview(scope.row)" v-hasPermi="['wms:storageDurationRule:list']">预览</el-button>
            <el-button link type="primary" icon="Edit" @click="handleUpdate(scope.row)" v-hasPermi="['wms:storageDurationRule:edit']">修改</el-button>
            <el-button link type="primary" icon="Delete" @click="handleDelete(scope.row)" v-hasPermi="['wms:storageDurationRule:edit']">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <pagination
        v-show="total > 0"
        :total="total"
        v-model:page="queryParams.pageNum"
        v-model:limit="queryParams.pageSize"
        @pagination="getList"
      />
    </el-card>

    <!-- 添加或修改规则对话框 -->
    <el-dialog :title="dialog.title" v-model="dialog.visible" width="560px" append-to-body :close-on-click-modal="false">
      <el-form ref="ruleFormRef" :model="form" :rules="rules" label-width="120px">
        <el-form-item label="商品规格" prop="skuId">
          <el-select
            v-model="form.skuId"
            placeholder="不选表示匹配全部规格"
            clearable
            filterable
            style="width: 100%"
          >
            <el-option
              v-for="item in skuList"
              :key="item.skuId"
              :label="skuLabel(item)"
              :value="item.skuId"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="入库时长超过" prop="thresholdDays">
          <el-input-number v-model="form.thresholdDays" :min="1" :precision="0" controls-position="right" style="width: 180px" />
          <span style="margin-left: 8px">天</span>
        </el-form-item>
        <el-form-item label="源仓库" prop="fromWarehouseId">
          <el-select v-model="form.fromWarehouseId" placeholder="请选择源仓库" filterable style="width: 100%">
            <el-option v-for="w in warehouseList" :key="w.id" :label="w.warehouseName" :value="w.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="目标仓库" prop="toWarehouseId">
          <el-select v-model="form.toWarehouseId" placeholder="请选择目标仓库" filterable style="width: 100%">
            <el-option v-for="w in warehouseList" :key="w.id" :label="w.warehouseName" :value="w.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="启用状态" prop="enableStatus">
          <el-switch v-model="form.enableStatus" :active-value="1" :inactive-value="0" />
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" placeholder="请输入备注" />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button :loading="buttonLoading" type="primary" @click="submitForm">确 定</el-button>
          <el-button @click="cancel">取 消</el-button>
        </div>
      </template>
    </el-dialog>

    <!-- 预览超期库存对话框 -->
    <el-dialog title="超期库存预览（将自动移库）" v-model="preview.visible" width="780px" append-to-body>
      <el-table v-loading="preview.loading" :data="preview.list" border empty-text="暂无超期库存">
        <el-table-column label="商品" prop="itemName" min-width="140" show-overflow-tooltip />
        <el-table-column label="规格" prop="skuName" min-width="120" show-overflow-tooltip />
        <el-table-column label="库存数量" prop="quantity" width="110" align="center" />
        <el-table-column label="入库时间" prop="inboundTime" width="170" align="center" />
        <el-table-column label="超期天数" prop="overdueDays" width="100" align="center">
          <template #default="scope">
            <span style="color: #f56c6c">{{ scope.row.overdueDays }} 天</span>
          </template>
        </el-table-column>
      </el-table>
      <template #footer>
        <el-button @click="preview.visible = false">关 闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="StorageDurationRule">
import {
  listStorageDurationRule,
  getStorageDurationRule,
  addStorageDurationRule,
  updateStorageDurationRule,
  delStorageDurationRule,
  previewStorageDurationRule
} from '@/api/wms/storageDurationRule';
import { listItemSkuPage } from '@/api/wms/itemSku';
import { getCurrentInstance, nextTick, onMounted, reactive, ref, toRefs } from 'vue';
import { ElForm } from 'element-plus';
import { useWmsStore } from '@/store/modules/wms';
import { checkPermi } from '@/utils/permission';

const { proxy } = getCurrentInstance();
const wmsStore = useWmsStore();

const ruleList = ref([]);
const skuList = ref([]);
const buttonLoading = ref(false);
const loading = ref(false);
const total = ref(0);
const queryFormRef = ref(ElForm);
const ruleFormRef = ref(ElForm);
const dialog = reactive({ visible: false, title: '' });
const preview = reactive({ visible: false, loading: false, list: [] });

const warehouseList = ref([]);
const warehouseMap = ref(new Map());

const initFormData = {
  id: undefined,
  skuId: undefined,
  thresholdDays: 30,
  fromWarehouseId: undefined,
  toWarehouseId: undefined,
  enableStatus: 1,
  remark: undefined
};

const data = reactive({
  form: { ...initFormData },
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    skuId: undefined,
    fromWarehouseId: undefined,
    enableStatus: undefined
  },
  rules: {
    thresholdDays: [{ required: true, message: '存储时长阈值不能为空', trigger: 'blur' }],
    fromWarehouseId: [{ required: true, message: '源仓库不能为空', trigger: 'change' }],
    toWarehouseId: [{ required: true, message: '目标仓库不能为空', trigger: 'change' }]
  }
});

const { queryParams, form, rules } = toRefs(data);

const skuLabel = (item) => {
  const itemName = item.item?.itemName || '';
  const skuName = item.itemSku?.skuName || '';
  return itemName ? `${itemName} / ${skuName}` : (skuName || item.skuName || '');
};

const warehouseName = (id) => {
  const w = warehouseMap.value.get(id);
  return w ? w.warehouseName : id;
};

/** 查询规则列表 */
const getList = async () => {
  loading.value = true;
  try {
    const res = await listStorageDurationRule(queryParams.value);
    ruleList.value = res.rows;
    total.value = res.total;
  } finally {
    loading.value = false;
  }
};

/** 加载商品规格列表（含商品名称） */
const loadSkuList = async () => {
  const res = await listItemSkuPage({ pageNum: 1, pageSize: 1000 });
  skuList.value = res.rows || [];
};

/** 加载仓库列表 */
const loadWarehouseList = async () => {
  await wmsStore.getWarehouseList();
  warehouseList.value = wmsStore.warehouseList;
  warehouseMap.value = wmsStore.warehouseMap;
};

/** 搜索 */
const handleQuery = () => {
  queryParams.value.pageNum = 1;
  getList();
};

/** 重置 */
const resetQuery = () => {
  queryFormRef.value.resetFields();
  handleQuery();
};

/** 取消 */
const cancel = () => {
  reset();
  dialog.visible = false;
};

/** 表单重置 */
const reset = () => {
  form.value = { ...initFormData };
  ruleFormRef.value.resetFields();
};

/** 新增 */
const handleAdd = () => {
  dialog.visible = true;
  dialog.title = '新增存储时长自动移库规则';
  nextTick(() => reset());
};

/** 修改 */
const handleUpdate = (row) => {
  dialog.visible = true;
  dialog.title = '修改存储时长自动移库规则';
  nextTick(async () => {
    reset();
    const res = await getStorageDurationRule(row.id);
    Object.assign(form.value, res.data);
  });
};

/** 启用状态切换 */
const handleStatusChange = async (row) => {
  const text = row.enableStatus === 1 ? '启用' : '停用';
  try {
    await updateStorageDurationRule(row);
    proxy?.$modal.msgSuccess(`${text}成功`);
  } catch (e) {
    row.enableStatus = row.enableStatus === 1 ? 0 : 1;
  }
};

/** 提交 */
const submitForm = () => {
  ruleFormRef.value.validate(async (valid) => {
    if (!valid) return;
    if (form.value.fromWarehouseId === form.value.toWarehouseId) {
      proxy?.$modal.msgError('源仓库与目标仓库不能相同');
      return;
    }
    buttonLoading.value = true;
    try {
      if (form.value.id) {
        await updateStorageDurationRule(form.value);
      } else {
        await addStorageDurationRule(form.value);
      }
      proxy?.$modal.msgSuccess(form.value.id ? '修改成功' : '新增成功');
      dialog.visible = false;
      await getList();
    } finally {
      buttonLoading.value = false;
    }
  });
};

/** 删除 */
const handleDelete = async (row) => {
  await proxy?.$modal.confirm('确认删除该存储时长自动移库规则吗？').finally(() => {});
  await delStorageDurationRule(row.id);
  proxy?.$modal.msgSuccess('删除成功');
  await getList();
};

/** 预览超期库存 */
const handlePreview = async (row) => {
  preview.visible = true;
  preview.loading = true;
  try {
    const res = await previewStorageDurationRule(row.id);
    preview.list = res.data || [];
  } finally {
    preview.loading = false;
  }
};

onMounted(async () => {
  await Promise.all([loadWarehouseList(), loadSkuList()]);
  await getList();
});
</script>
