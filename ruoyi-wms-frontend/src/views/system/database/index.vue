<template>
  <div class="app-container">
    <!-- 数据库状态信息 -->
    <el-card class="mb20" shadow="hover">
      <template #header>
        <div class="card-header">
          <span><el-icon><Coin /></el-icon> 数据库状态</span>
          <el-button type="primary" plain icon="Refresh" @click="loadStatus" :loading="statusLoading">刷新</el-button>
        </div>
      </template>

      <el-descriptions :column="3" border v-loading="statusLoading">
        <el-descriptions-item label="数据库名称">{{ status.databaseName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="数据库类型">{{ status.databaseProduct || '-' }}</el-descriptions-item>
        <el-descriptions-item label="数据库版本">{{ status.databaseVersion || '-' }}</el-descriptions-item>
        <el-descriptions-item label="表总数">{{ status.tableCount || 0 }}</el-descriptions-item>
      </el-descriptions>

      <!-- 业务表数据量统计 -->
      <el-table :data="tableCountList" border style="width: 100%; margin-top: 15px" v-if="tableCountList.length > 0">
        <el-table-column label="表名" prop="table" align="center" />
        <el-table-column label="数据量" prop="count" align="center">
          <template #default="scope">
            <el-tag :type="scope.row.count > 0 ? 'success' : 'info'">
              {{ scope.row.count >= 0 ? scope.row.count : '表不存在' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="表名" prop="table2" align="center" />
        <el-table-column label="数据量" prop="count2" align="center">
          <template #default="scope">
            <el-tag :type="scope.row.count2 > 0 ? 'success' : 'info'">
              {{ scope.row.count2 !== '' ? (scope.row.count2 >= 0 ? scope.row.count2 : '表不存在') : '' }}
            </el-tag>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 数据库初始化操作 -->
    <el-card shadow="hover">
      <template #header>
        <div class="card-header">
          <span><el-icon><WarningFilled /></el-icon> 数据库初始化</span>
        </div>
      </template>

      <el-alert
        title="危险操作"
        type="error"
        :closable="false"
        show-icon
        style="margin-bottom: 20px"
      >
        <template #default>
          <p style="margin: 0">此操作将<strong>清空所有业务数据</strong>，并将数据库重置为初始状态（仅保留初始菜单、角色、用户等基础数据）。</p>
          <p style="margin: 5px 0 0 0">操作不可逆，请谨慎执行！建议在操作前备份重要数据。</p>
        </template>
      </el-alert>

      <el-form ref="initFormRef" :model="initForm" :rules="initRules" label-width="120px">
        <el-form-item label="SQL 脚本列表" prop="scripts">
          <el-tag v-for="script in sqlScripts" :key="script" class="mr10 mb5" type="info">
            {{ script }}
          </el-tag>
          <div class="tip-text">以上脚本将按顺序执行，重建所有表结构和初始数据</div>
        </el-form-item>

        <el-form-item label="确认文字" prop="confirmText">
          <el-input
            v-model="initForm.confirmText"
            placeholder="请输入「确认初始化」以继续"
            style="width: 300px"
            clearable
          />
          <div class="tip-text">请准确输入 <strong>确认初始化</strong> 四个字</div>
        </el-form-item>

        <el-form-item>
          <el-button
            type="danger"
            icon="Delete"
            @click="handleInit"
            :loading="initLoading"
            :disabled="initForm.confirmText !== '确认初始化'"
          >执行数据库初始化</el-button>
          <el-button icon="RefreshLeft" @click="resetForm">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 初始化结果对话框 -->
    <el-dialog title="初始化结果" v-model="resultVisible" width="600px" append-to-body>
      <el-alert
        :title="initResult.message"
        :type="initResult.success ? 'success' : 'warning'"
        :closable="false"
        show-icon
        style="margin-bottom: 15px"
      />
      <el-descriptions :column="1" border>
        <el-descriptions-item label="执行状态">
          <el-tag :type="initResult.success ? 'success' : 'warning'">
            {{ initResult.success ? '全部成功' : (initResult.totalStatements === 0 ? '未执行' : '部分失败') }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="执行SQL语句数" v-if="initResult.totalStatements !== undefined">
          <el-tag :type="initResult.totalStatements > 0 ? 'success' : 'danger'">
            {{ initResult.totalStatements }} 条
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="已执行脚本">
          <el-tag v-for="f in initResult.executedFiles" :key="f" class="mr5 mb5" type="success">{{ f }}</el-tag>
          <span v-if="!initResult.executedFiles || initResult.executedFiles.length === 0">无</span>
        </el-descriptions-item>
        <el-descriptions-item label="错误信息" v-if="initResult.errors && initResult.errors.length > 0">
          <div v-for="err in initResult.errors" :key="err" class="error-msg">{{ err }}</div>
        </el-descriptions-item>
      </el-descriptions>
      <template #footer>
        <el-button type="primary" @click="resultVisible = false">确 定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup name="DatabaseInit">
import { getDatabaseStatus, initDatabase } from "@/api/system/database";

const { proxy } = getCurrentInstance();

const statusLoading = ref(false);
const initLoading = ref(false);
const resultVisible = ref(false);
const status = ref({});
const initResult = ref({});

const sqlScripts = [
  'wms.sql',
  'inventory_snapshot.sql',
  'stock_warning.sql',
  'wave_pick.sql',
  'wms_notification.sql',
  'database_menu.sql'
];

const initForm = reactive({
  confirmText: ''
});

const initRules = {
  confirmText: [
    { required: true, message: '请输入确认文字', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (value !== '确认初始化') {
          callback(new Error('请输入正确的确认文字「确认初始化」'));
        } else {
          callback();
        }
      },
      trigger: 'blur'
    }
  ]
};

/** 表数据量列表（将 Map 转为数组用于表格展示） */
const tableCountList = computed(() => {
  if (!status.value.tableDataCounts) return [];
  const entries = Object.entries(status.value.tableDataCounts);
  // 两列展示，将数组分成两半
  const half = Math.ceil(entries.length / 2);
  const left = entries.slice(0, half);
  const right = entries.slice(half);
  const result = [];
  for (let i = 0; i < half; i++) {
    result.push({
      table: left[i] ? left[i][0] : '',
      count: left[i] ? left[i][1] : '',
      table2: right[i] ? right[i][0] : '',
      count2: right[i] ? right[i][1] : ''
    });
  }
  return result;
});

/** 加载数据库状态 */
function loadStatus() {
  statusLoading.value = true;
  getDatabaseStatus().then(res => {
    status.value = res.data || {};
  }).finally(() => {
    statusLoading.value = false;
  });
}

/** 执行初始化 */
function handleInit() {
  proxy.$refs["initFormRef"].validate(valid => {
    if (!valid) return;

    proxy.$modal.confirm(
      '确定要执行数据库初始化吗？此操作将清空所有业务数据并重置为初始状态，不可恢复！'
    ).then(() => {
      initLoading.value = true;
      initDatabase({ confirm: initForm.confirmText }).then(res => {
        initResult.value = res.data || {};
        resultVisible.value = true;
        // 刷新状态
        loadStatus();
        // 重置确认文字
        initForm.confirmText = '';
      }).finally(() => {
        initLoading.value = false;
      });
    }).catch(() => {});
  });
}

/** 重置表单 */
function resetForm() {
  initForm.confirmText = '';
  proxy.resetForm("initFormRef");
}

// 页面加载时获取状态
loadStatus();
</script>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.mb20 {
  margin-bottom: 20px;
}

.mb5 {
  margin-bottom: 5px;
}

.mr5 {
  margin-right: 5px;
}

.mr10 {
  margin-right: 10px;
}

.tip-text {
  color: #909399;
  font-size: 12px;
  margin-top: 5px;
}

.error-msg {
  color: #f56c6c;
  font-size: 13px;
  margin-bottom: 5px;
}
</style>
