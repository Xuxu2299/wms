<template>
  <div class="alarmInfo-container">
    <div v-if="alarmList.length === 0" class="alarm-empty">暂无库存预警</div>
    <Vue3SeamlessScroll v-else :list="alarmList" :hover="true" :step="0.4" :wheel="true" :isWatch="true" :limitScrollNum="4" style="height: 100%;overflow:hidden;">
      <div class="alarm-item" v-for="(item, index) in alarmList" :key="index">
        <div class="flex-between mb5">
          <span class="alarm-name">{{ item.itemName }}（{{ item.skuName }}）</span>
          <div class="item-level" :class="changeAlarmBg(item.warningType)">{{ getLevel(item.warningType) }}</div>
        </div>
        <div class="flex-between">
          <span>当前库存：{{ item.totalQuantity }}</span>
          <span class="alarm-stock">下限：{{ item.minStock }} / 上限：{{ item.maxStock }}</span>
        </div>
      </div>
    </Vue3SeamlessScroll>
  </div>
</template>

<script setup>
import { Vue3SeamlessScroll } from 'vue3-seamless-scroll'
import { onMounted, ref } from 'vue'
import { listInventoryWarning } from '@/api/wms/inventory'

const alarmList = ref([])

async function getAlarmList() {
  try {
    const res = await listInventoryWarning()
    alarmList.value = res.data || []
  } catch (e) {
    alarmList.value = []
  }
}

function changeAlarmBg(warningType) {
  switch (warningType) {
    case 'LOW':
      return 'alarm-danger'
    case 'HIGH':
      return 'alarm-warning'
    default:
      return 'alarm-normal'
  }
}
function getLevel(warningType) {
  switch (warningType) {
    case 'LOW':
      return '库存不足'
    case 'HIGH':
      return '库存超限'
    default:
      return '正常'
  }
}

onMounted(() => {
  getAlarmList()
})
</script>

<style lang='scss' scoped>
.alarmInfo-container {
  width: 100%;
  height: 100%;
  padding: 12px 12px 0;
  font-size: 14px;

  .alarm-empty {
    text-align: center;
    color: #7e8ca0;
    padding: 20px 0;
  }

  .alarm-item {
    padding: 12px 0;
    border-bottom: 1px solid;
    border-image: linear-gradient(90deg, #00d0fe 0%, #286be9 50%, #00d0fe 100%) 2 2 2 2;;

    .alarm-name {
      // color: var(--current-color);
      font-weight: bold;
      color: #00d0fe;
    }

    .alarm-stock {
      color: #b3c0d1;
    }

    .item-level {
      padding: 2px 8px;
      font-size: 13px;
      color: #fff;
      // background: #f56c6c;
      border-radius: 10px 0 10px 0;
    }
    .alarm-normal {
      background: #67c23a;
    }
    .alarm-warning {
      background: #e6a23c;
    }
    .alarm-danger {
      background: #f56c6c;
    }
  }
}
</style>
