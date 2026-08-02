<template>
   <div class="app-container">
      <el-form :model="queryParams" ref="queryRef" :inline="true" v-show="showSearch" label-width="80px">
        <el-form-item label="任务号" prop="taskId">
            <el-input
               v-model="queryParams.taskId"
               placeholder="请输入任务号"
               clearable
               style="width: 200px;"
               @keyup.enter="handleQuery"
            />
         </el-form-item>
         <el-form-item label="回调类型" prop="callbackType">
            <el-select
               v-model="queryParams.callbackType"
               placeholder="回调类型"
               clearable
               style="width: 200px"
            >
               <el-option label="任务状态上报" value="TaskAction" />
               <el-option label="安全信号" value="AgvSign" />
            </el-select>
         </el-form-item>
         <el-form-item label="任务状态" prop="taskStatus">
            <el-select
               v-model="queryParams.taskStatus"
               placeholder="任务状态"
               clearable
               style="width: 200px"
            >
               <el-option label="开始执行" :value="0" />
               <el-option label="完成" :value="4" />
            </el-select>
         </el-form-item>
         <el-form-item label="站点编码" prop="siteCode">
            <el-input
               v-model="queryParams.siteCode"
               placeholder="请输入站点编码"
               clearable
               style="width: 200px;"
               @keyup.enter="handleQuery"
            />
         </el-form-item>
         <el-form-item label="容器编号" prop="barCode">
            <el-input
               v-model="queryParams.barCode"
               placeholder="请输入容器编号"
               clearable
               style="width: 200px;"
               @keyup.enter="handleQuery"
            />
         </el-form-item>
         <el-form-item label="创建时间" style="width: 308px">
            <el-date-picker
               v-model="dateRange"
               value-format="YYYY-MM-DD HH:mm:ss"
               type="daterange"
               range-separator="-"
               start-placeholder="开始日期"
               end-placeholder="结束日期"
               :default-time="[new Date(2000, 1, 1, 0, 0, 0), new Date(2000, 1, 1, 23, 59, 59)]"
            ></el-date-picker>
         </el-form-item>
         <el-form-item>
            <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
            <el-button icon="Refresh" @click="resetQuery">重置</el-button>
         </el-form-item>
      </el-form>

      <el-row :gutter="10" class="mb8">
         <el-col :span="1.5">
            <el-button
               type="danger"
               plain
               icon="Delete"
               :disabled="multiple"
               @click="handleDelete"
               v-hasPermi="['wms:agvLog:all']"
            >删除</el-button>
         </el-col>
         <el-col :span="1.5">
            <el-button
               type="danger"
               plain
               icon="Delete"
               @click="handleClean"
               v-hasPermi="['wms:agvLog:all']"
            >清空</el-button>
         </el-col>
         <el-col :span="1.5">
            <el-button
               type="warning"
               plain
               icon="Download"
               @click="handleExport"
               v-hasPermi="['wms:agvLog:all']"
            >导出</el-button>
         </el-col>
         <right-toolbar v-model:showSearch="showSearch" @queryTable="getList"></right-toolbar>
      </el-row>

      <el-table ref="agvLogRef" v-loading="loading" :data="agvLogList" @selection-change="handleSelectionChange">
         <el-table-column type="selection" width="55" align="center" />
         <el-table-column label="日志ID" align="center" prop="id" width="80" />
         <el-table-column label="任务号" align="center" prop="taskId" :show-overflow-tooltip="true" width="160" />
         <el-table-column label="回调类型" align="center" prop="callbackType" width="120">
            <template #default="scope">
               <el-tag v-if="scope.row.callbackType === 'TaskAction'" type="primary">任务状态上报</el-tag>
               <el-tag v-else-if="scope.row.callbackType === 'AgvSign'" type="warning">安全信号</el-tag>
               <span v-else>{{ scope.row.callbackType }}</span>
            </template>
         </el-table-column>
         <el-table-column label="任务状态" align="center" prop="taskStatus" width="100">
            <template #default="scope">
               <el-tag v-if="scope.row.taskStatus === 0" type="info">开始执行</el-tag>
               <el-tag v-else-if="scope.row.taskStatus === 4" type="success">完成</el-tag>
               <span v-else>{{ scope.row.taskStatus }}</span>
            </template>
         </el-table-column>
         <el-table-column label="AGV编号" align="center" prop="vid" width="90" />
         <el-table-column label="站点编码" align="center" prop="siteCode" width="100" />
         <el-table-column label="货架号" align="center" prop="rackNo" width="90" />
         <el-table-column label="容器编号" align="center" prop="barCode" width="120" :show-overflow-tooltip="true" />
         <el-table-column label="返回信息" align="center" prop="returnInfo" :show-overflow-tooltip="true" min-width="120" />
         <el-table-column label="错误码" align="center" prop="errCode" width="90" />
         <el-table-column label="创建时间" align="center" prop="createTime" width="180">
            <template #default="scope">
               <span>{{ parseTime(scope.row.createTime) }}</span>
            </template>
         </el-table-column>
         <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="80">
            <template #default="scope">
               <el-button link type="primary" icon="View" @click="handleView(scope.row)">详细</el-button>
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

      <!-- AGV日志详细 -->
      <el-dialog title="AGV回调日志详细" v-model="open" width="800px" append-to-body>
         <el-form :model="form" label-width="100px">
            <el-row>
               <el-col :span="12">
                  <el-form-item label="日志ID：">{{ form.id }}</el-form-item>
               </el-col>
               <el-col :span="12">
                  <el-form-item label="任务号：">{{ form.taskId }}</el-form-item>
               </el-col>
               <el-col :span="12">
                  <el-form-item label="回调类型：">
                     <el-tag v-if="form.callbackType === 'TaskAction'" type="primary">任务状态上报</el-tag>
                     <el-tag v-else-if="form.callbackType === 'AgvSign'" type="warning">安全信号</el-tag>
                     <span v-else>{{ form.callbackType }}</span>
                  </el-form-item>
               </el-col>
               <el-col :span="12">
                  <el-form-item label="任务状态：">
                     <el-tag v-if="form.taskStatus === 0" type="info">开始执行</el-tag>
                     <el-tag v-else-if="form.taskStatus === 4" type="success">完成</el-tag>
                     <span v-else>{{ form.taskStatus }}</span>
                  </el-form-item>
               </el-col>
               <el-col :span="12">
                  <el-form-item label="AGV编号：">{{ form.vid }}</el-form-item>
               </el-col>
               <el-col :span="12">
                  <el-form-item label="站点编码：">{{ form.siteCode }}</el-form-item>
               </el-col>
               <el-col :span="12">
                  <el-form-item label="货架号：">{{ form.rackNo }}</el-form-item>
               </el-col>
               <el-col :span="12">
                  <el-form-item label="货架高度：">{{ form.height }}</el-form-item>
               </el-col>
               <el-col :span="12">
                  <el-form-item label="容器编号：">{{ form.barCode }}</el-form-item>
               </el-col>
               <el-col :span="12">
                  <el-form-item label="动作类型：">
                     <span v-if="form.actionType === '1'">放货</span>
                     <span v-else-if="form.actionType === '2'">取货</span>
                     <span v-else>{{ form.actionType }}</span>
                  </el-form-item>
               </el-col>
               <el-col :span="12">
                  <el-form-item label="库位：">{{ form.location }}</el-form-item>
               </el-col>
               <el-col :span="12">
                  <el-form-item label="错误码：">{{ form.errCode }}</el-form-item>
               </el-col>
               <el-col :span="12">
                  <el-form-item label="错误类型：">{{ form.errType }}</el-form-item>
               </el-col>
               <el-col :span="24">
                  <el-form-item label="返回信息：">{{ form.returnInfo }}</el-form-item>
               </el-col>
               <el-col :span="12">
                  <el-form-item label="上报时间戳：">{{ form.reportTime }}</el-form-item>
               </el-col>
               <el-col :span="12">
                  <el-form-item label="创建时间：">{{ parseTime(form.createTime) }}</el-form-item>
               </el-col>
               <el-col :span="24">
                  <el-form-item label="请求报文：">
                     <el-input
                        type="textarea"
                        :model-value="formatJson(form.requestBody)"
                        :rows="10"
                        readonly
                        style="font-family: monospace;"
                     />
                  </el-form-item>
               </el-col>
            </el-row>
         </el-form>
         <template #footer>
            <div class="dialog-footer">
               <el-button @click="open = false">关 闭</el-button>
            </div>
         </template>
      </el-dialog>
   </div>
</template>

<script setup name="AgvLog">
import { listAgvLog, delAgvLog, cleanAgvLog } from "@/api/wms/agvLog";

const { proxy } = getCurrentInstance();

const agvLogList = ref([]);
const open = ref(false);
const loading = ref(true);
const showSearch = ref(true);
const ids = ref([]);
const single = ref(true);
const multiple = ref(true);
const total = ref(0);
const dateRange = ref([]);

const data = reactive({
  form: {},
  queryParams: {
    pageNum: 1,
    pageSize: 10,
    taskId: undefined,
    callbackType: undefined,
    taskStatus: undefined,
    siteCode: undefined,
    barCode: undefined
  }
});

const { queryParams, form } = toRefs(data);

/** 查询AGV日志列表 */
function getList() {
  loading.value = true;
  listAgvLog(proxy.addDateRange(queryParams.value, dateRange.value)).then(response => {
    agvLogList.value = response.rows;
    total.value = response.total;
    loading.value = false;
  });
}
/** 搜索按钮操作 */
function handleQuery() {
  queryParams.value.pageNum = 1;
  getList();
}
/** 重置按钮操作 */
function resetQuery() {
  dateRange.value = [];
  proxy.resetForm("queryRef");
  queryParams.value.pageNum = 1;
  getList();
}
/** 多选框选中数据 */
function handleSelectionChange(selection) {
  ids.value = selection.map(item => item.id);
  multiple.value = !selection.length;
  single.value = selection.length !== 1;
}
/** 详细按钮操作 */
function handleView(row) {
  open.value = true;
  form.value = row;
}
/** 格式化JSON */
function formatJson(str) {
  if (!str) return '';
  try {
    return JSON.stringify(JSON.parse(str), null, 2);
  } catch (e) {
    return str;
  }
}
/** 删除按钮操作 */
function handleDelete(row) {
  const logIds = row.id || ids.value;
  proxy.$modal.confirm('是否确认删除日志编号为"' + logIds + '"的数据项?').then(function () {
    return delAgvLog(logIds);
  }).then(() => {
    getList();
    proxy.$modal.msgSuccess("删除成功");
  }).catch(() => {});
}
/** 清空按钮操作 */
function handleClean() {
  proxy.$modal.confirm("是否确认清空所有AGV日志数据项?").then(function () {
    return cleanAgvLog();
  }).then(() => {
    getList();
    proxy.$modal.msgSuccess("清空成功");
  }).catch(() => {});
}
/** 导出按钮操作 */
function handleExport() {
  proxy.download("wms/agvLog/export", {
    ...queryParams.value,
  }, `agv_log_${new Date().getTime()}.xlsx`);
}

getList();
</script>
