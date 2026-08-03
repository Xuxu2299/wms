<template>
  <div class="app-container home">
    <div class="station-top">
      <el-row :gutter="12" class="mt5">
        <el-col :span="6">
          <div class="top-item-box item-box-one" style="display: flex;">
            <div style="flex:2;height:100%;">
              <div>入库</div>
              <div style="text-align:center;margin-top:30px;"><span style="font-size:26px;font-weight:bold;">{{ summary.todayInboundQuantity ?? 0 }}</span>
              </div>
            </div>
            <div style="flex:3;display: flex;flex-direction:column;justify-content:space-evenly">
              <div>入库单数：{{ summary.todayInboundCount ?? 0 }}</div>
              <div>待入库：0</div>
              <div>待质检：0</div>
              <div>待上架：0</div>
            </div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="top-item-box item-box-two" style="display: flex;">
            <div style="flex:2;height:100%;">
              <div>出库</div>
              <div style="text-align:center;margin-top:30px;"><span style="font-size:26px;font-weight:bold;"
              >{{ summary.todayOutboundQuantity ?? 0 }}</span></div>
            </div>
            <div style="flex:3;display: flex;flex-direction:column;justify-content:space-evenly">
              <div>出库单数：{{ summary.todayOutboundCount ?? 0 }}</div>
              <div>待配货：0</div>
              <div>待拣货：0</div>
              <div>待出库：0</div>
            </div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="top-item-box item-box-three" style="display: flex;">
            <div style="flex:2;height:100%;">
              <div>库位</div>
              <div style="text-align:center;margin-top:30px;"><span style="font-size:26px;font-weight:bold;"
              >{{ summary.totalLocations ?? 0 }}</span>
              </div>
            </div>
            <div style="flex:3;display: flex;flex-direction:column;justify-content:space-evenly">
              <div>已占用：{{ summary.occupiedLocations ?? 0 }}</div>
              <div>空闲：{{ freeLocations }}</div>
              <div>利用率：{{ locationUsageRate }}%</div>
            </div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="top-item-box item-box-four" style="display: flex;">
            <div style="flex:2;height:100%;">
              <div>库存预警</div>
              <div style="text-align:center;margin-top:30px;"><span style="font-size:26px;font-weight:bold;">{{ summary.stockWarningCount ?? 0 }}</span>
              </div>
            </div>
            <div style="flex:3;display: flex;flex-direction:column;justify-content:space-evenly">
              <div>松陵仓：0</div>
              <div>盛泽仓：0</div>
              <div>园区仓：0</div>
            </div>
          </div>
        </el-col>
      </el-row>
    </div>
    <div class="station-middle">
      <el-row :gutter="12">
        <el-col :span="6">
          <el-card class="box-card" shadow="never">
            <div class="card-title">库位利用率</div>
            <div style="height: calc(100% - 30px);">
              <StationPie height="100%" :pieData="pieData"/>
              <div></div>
            </div>
          </el-card>
        </el-col>
        <el-col :span="18">
          <el-card class="box-card" shadow="never">
            <div style="display:flex;justify-content: space-between;align-items: center;">
              <div class="card-title">近7日入库趋势</div>
            </div>
            <div style="height: calc(100% - 30px);">
              <StationBar height="100%" :chartData="barChartData" :xName="'日'" :setting="{seriesName: '入库数量', yName: '件'}"/>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </div>
    <div class="station-bottom">
      <el-row :gutter="12">
        <el-col :span="12">
          <el-card class="box-card" shadow="never">
            <div class="card-title">近7日入库趋势</div>
            <div style="height: calc(100% - 30px);">
              <StationLine height="100%" itemColor="#5470c6" yName="件" :chartData="lineDataInbound" seriesName="入库"/>
            </div>
          </el-card>
        </el-col>
        <el-col :span="12">
          <el-card class="box-card" shadow="never">
            <div class="card-title">近7日出库趋势</div>
            <div style="height: calc(100% - 30px);">
              <StationLine height="100%" :chartData="lineDataOutbound" yName="件" itemColor="#ee4368" seriesName="出库"/>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </div>
  </div>
</template>
<script setup>
import StationPie from './components/StationPie.vue'
import StationLine from './components/StationLine.vue'
import StationBar from './components/StationBar.vue'
import { computed, onMounted, ref } from 'vue'
import { getDashboardSummary, getDashboardTrend } from '@/api/wms/dashboard'

// 汇总数据（来自 /wms/dashboard/summary）
const summary = ref({})
// 近7天趋势数据（来自 /wms/dashboard/trend）
const trend = ref({ dates: [], inboundQuantities: [], outboundQuantities: [] })

// 空闲库位数
const freeLocations = computed(() => {
  const total = Number(summary.value.totalLocations) || 0
  const occupied = Number(summary.value.occupiedLocations) || 0
  return Math.max(total - occupied, 0)
})
// 库位利用率
const locationUsageRate = computed(() => {
  const total = Number(summary.value.totalLocations) || 0
  const occupied = Number(summary.value.occupiedLocations) || 0
  if (!total) return 0
  return ((occupied / total) * 100).toFixed(1)
})
// 库位利用率饼图数据
const pieData = computed(() => {
  const total = Number(summary.value.totalLocations) || 0
  const occupied = Number(summary.value.occupiedLocations) || 0
  return [
    { value: occupied, name: '已占用', itemStyle: { color: '#3671e8' } },
    { value: Math.max(total - occupied, 0), name: '空闲', itemStyle: { color: '#9fc5ff' } }
  ]
})
// 入库趋势柱状图数据
const barChartData = computed(() => ({
  xData: trend.value.dates || [],
  yData: trend.value.inboundQuantities || []
}))
// 入库趋势折线图数据
const lineDataInbound = computed(() => ({
  xData: trend.value.dates || [],
  yData: trend.value.inboundQuantities || []
}))
// 出库趋势折线图数据
const lineDataOutbound = computed(() => ({
  xData: trend.value.dates || [],
  yData: trend.value.outboundQuantities || []
}))

// 获取汇总数据
const fetchSummary = async () => {
  try {
    const res = await getDashboardSummary()
    summary.value = res.data || {}
  } catch (e) {
    summary.value = {}
  }
}
// 获取趋势数据
const fetchTrend = async () => {
  try {
    const res = await getDashboardTrend()
    trend.value = res.data || { dates: [], inboundQuantities: [], outboundQuantities: [] }
  } catch (e) {
    trend.value = { dates: [], inboundQuantities: [], outboundQuantities: [] }
  }
}

onMounted(() => {
  fetchSummary()
  fetchTrend()
})
</script>


<style scoped>
.app-container {
  min-height: calc(100vh - 84px);
  padding: 12px 12px 0 12px;
}

.top-item-box {
  height: 160px;
  background: #fff;
  margin-bottom: 12px;
  border-radius: 12px;
  color: #fff;
  padding: 16px;
}

.item-box-one {
  background: linear-gradient(30deg, #1a94db, #4db1eb, #7acaf9);
  box-shadow: 0 4px 12px #8ed2fa;
}

.item-box-two {
  background: linear-gradient(30deg, #c7a327, #d5ba47, #e3cf65);
  box-shadow: 0 4px 12px #ece7cd;
}

.item-box-three {
  background: linear-gradient(30deg, #6365f7, #9177f1, #cd8ee9);
  box-shadow: 0 4px 12px #dcc9e6;
}

.item-box-four {
  background: linear-gradient(30deg, #ed3a60, #f1557a, #f67da0);
  box-shadow: 0 4px 12px #e7cfd6;
}

.box-card {
  height: 400px;
  margin-bottom: 12px;
  background-color: #fff;
  border-color: #ebe6f5;
}

.box-card >>> .el-card__body {
  height: 100%;
}

.card-title {
  font-weight: bold;
  height: 30px;
  display: flex;
  align-items: center;
}

.card-title::before {
  content: '';
  height: 70%;
  width: 5px;
  background: #3671e8;
  margin-right: 8px;
}

</style>
