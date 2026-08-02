<template>
  <div class="app-container home">
    <!-- ===== 数据大屏增强 ===== -->
    <!-- 今日出入库汇总卡片 -->
    <el-row class="pl20 pr20 pt20" :gutter="16">
      <el-col :span="6">
        <el-card shadow="hover" class="summary-card-wrap">
          <div class="summary-card">
            <div class="summary-icon inbound-bg">
              <el-icon size="28"><Download/></el-icon>
            </div>
            <div class="summary-info">
              <div class="summary-value">{{ summary.todayInboundCount || 0 }}</div>
              <div class="summary-label">今日入库单数</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="summary-card-wrap">
          <div class="summary-card">
            <div class="summary-icon outbound-bg">
              <el-icon size="28"><Upload/></el-icon>
            </div>
            <div class="summary-info">
              <div class="summary-value">{{ summary.todayOutboundCount || 0 }}</div>
              <div class="summary-label">今日出库单数</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="summary-card-wrap">
          <div class="summary-card">
            <div class="summary-icon inbound-qty-bg">
              <el-icon size="28"><Box/></el-icon>
            </div>
            <div class="summary-info">
              <div class="summary-value">{{ summary.todayInboundQuantity || 0 }}</div>
              <div class="summary-label">今日入库数量</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="summary-card-wrap">
          <div class="summary-card">
            <div class="summary-icon outbound-qty-bg">
              <el-icon size="28"><Van/></el-icon>
            </div>
            <div class="summary-info">
              <div class="summary-value">{{ summary.todayOutboundQuantity || 0 }}</div>
              <div class="summary-label">今日出库数量</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 近7天出入库趋势 + 库位利用率 -->
    <el-row class="pl20 pr20 pt20" :gutter="16">
      <el-col :span="16">
        <el-card shadow="hover">
          <template #header>
            <div class="card-header-title">
              <el-icon><TrendCharts/></el-icon>
              <span>近7天出入库趋势</span>
            </div>
          </template>
          <div ref="trendChartRef" style="height: 320px; width: 100%"></div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover">
          <template #header>
            <div class="card-header-title">
              <el-icon><LocationFilled/></el-icon>
              <span>库位利用率</span>
            </div>
          </template>
          <div class="location-util-wrap">
            <el-progress
              type="dashboard"
              :percentage="locationPercentage"
              :color="locationColors"
              :width="160"
            >
              <template #default="{ percentage }">
                <span class="location-percentage-text">{{ percentage }}%</span>
              </template>
            </el-progress>
            <div class="location-detail">
              <div class="location-row">
                <span class="dot occupied-dot"></span>
                <span>已占用：<b>{{ summary.occupiedLocations || 0 }}</b> 个</span>
              </div>
              <div class="location-row">
                <span class="dot empty-dot"></span>
                <span>空闲：<b>{{ emptyLocations }}</b> 个</span>
              </div>
              <div class="location-row">
                <span class="dot total-dot"></span>
                <span>总计：<b>{{ summary.totalLocations || 0 }}</b> 个</span>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 库存预警 + 快捷操作 -->
    <el-row class="pl20 pr20 pt20" :gutter="16">
      <el-col :span="12">
        <el-card shadow="hover" :class="{ 'warning-card': (summary.stockWarningCount || 0) > 0 }">
          <template #header>
            <div class="card-header-title">
              <el-icon><Warning/></el-icon>
              <span>库存预警</span>
              <el-badge :value="summary.stockWarningCount || 0" :hidden="(summary.stockWarningCount || 0) === 0" class="warning-badge" />
            </div>
          </template>
          <div v-if="(summary.stockWarningCount || 0) === 0" class="warning-empty">
            <el-icon size="40" color="#67c23a"><CircleCheckFilled/></el-icon>
            <p>所有商品库存正常</p>
          </div>
          <div v-else class="warning-list">
            <el-button type="danger" plain size="small" @click="goInventoryWarning">查看预警详情</el-button>
          </div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="hover">
          <template #header>
            <div class="card-header-title">
              <el-icon><Camera/></el-icon>
              <span>库存快照</span>
            </div>
          </template>
          <div class="snapshot-actions">
            <el-button type="primary" plain size="small" @click="createSnapshot" :loading="snapshotLoading">立即创建快照</el-button>
            <el-button type="info" plain size="small" @click="goInventorySnapshot">查看历史快照</el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- ===== 原有内容 ===== -->
    <el-row class="pl20 pr20 pb20 pt20" :gutter="10">
      <el-col :span="12">
        <el-card shadow="always" style="padding-bottom: 20px;font-size: 14px" >
          <div slot="header">
            <span style="font-size: large;font-weight: bold">SaaS版已上线，如需体验，请在公众号内回复：saas</span>
          </div>
          <div style="display: flex;align-items: center">
            <div class="first" style="font-size:20px;line-height: 50px;background: linear-gradient(to right, red, blue);-webkit-background-clip: text;color: transparent;">
              轻量级库存管理工具，不用安装，自动升级，让仓库效率提高5倍，让出错概率降低5倍。 集中入库、出库、扫描、一物一码、商品、库存、供应商、结算等优质功能于一体，为商家提供更全面库存处理解决方案。
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card style="font-size: 14px">
          <div slot="header">
            <span style="font-size: large;font-weight: bold">更多内容</span>
          </div>
          <div>
            <div style="font-size:20px;line-height: 50px;background: linear-gradient(to right, red, blue);-webkit-background-clip: text;color: transparent;">
              <div>
                <span style="font-size: large;font-weight: bold">v2 advance预览：</span><a href="http://kucun.ichengle.top/" target="_blank">http://kucun.ichengle.top/</a>
              </div>
              <div>
                <span style="font-size: large;font-weight: bold">v1.0预览：</span><a href="http://wms.ichengle.top/" target="_blank">http://wms.ichengle.top/</a>
              </div>
              <div>
                <span style="font-size: large;font-weight: bold">讲解视频：</span><a href="https://www.bilibili.com/video/BV1ys4y1q7uG/" target="_blank">https://www.bilibili.com/video/BV1ys4y1q7uG/</a>
              </div>
              <div>
                <span style="font-size: large;font-weight: bold">若依实战视频：</span><a href="https://www.bilibili.com/video/BV1Fi4y1q74p/" target="_blank">https://www.bilibili.com/video/BV1Fi4y1q74p/</a>
              </div>
            </div>
          </div>

        </el-card>

      </el-col>
    </el-row>
    <el-row class="pl20 pr20" :gutter="10">
      <el-col :span="12">
        <el-card shadow="always" style="padding-bottom: 20px;font-size: 14px;margin-bottom: 20px; margin-top: 20px" >
          <div slot="header">
            <span style="font-size: large;font-weight: bold">招聘全栈开发</span>
          </div>
          <div style="display: flex;align-items: center">
            <div class="first" style="font-size:20px;line-height: 50px;background: linear-gradient(to right, red, blue);-webkit-background-clip: text;color: transparent;">
              参与开发基于jdk17和vue3的ruoyi-mall、ruoyi-erp-进销存。<br>
              要求：对若依框架和ruoyi-wms、ruoyi-mall 有一定的认知。并且有一定的空余时间。<br>
              全职、兼职、实习都可。我们在苏州，远程或现场参与开发都可。<br>
              有兴趣的可以在公众号内回复：应聘。<br>
            </div>
          </div>
        </el-card>
        <el-card shadow="always" style="padding-bottom: 20px;font-size: 14px;margin-bottom: 20px;" >
          <div slot="header">
            <span style="font-size: large;font-weight: bold">招聘自媒体运营</span>
          </div>
          <div style="display: flex;align-items: center">
            <div class="first" style="font-size:20px;line-height: 50px;background: linear-gradient(to right, red, blue);-webkit-background-clip: text;color: transparent;">
              参与ruoyi-wms、ruoyi-mall、ruoyi-erp-进销存项目的自媒体运营。<br>
              要求做过短视频编辑或公众号文章编辑，并且对我们的开源项目有一定的了解。<br>
              全职、兼职、实习都可。我们在苏州，远程或现场参与开发都可。<br>
              有兴趣的可以在公众号内回复：应聘。<br>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card style="padding-bottom: 20px; font-size: 14px">
          <div slot="header">
            <span style="font-size: large;font-weight: bold">发展历程</span>
          </div>
          <div style="padding-top: 20px">
            <el-timeline >
              <el-timeline-item placement="top" timestamp="2018年">
                <el-card>
                  <h4>参与京东服务市场商品分析应用开发，参与京东服务市场会员积分应用开发</h4>
                </el-card>
              </el-timeline-item>
              <el-timeline-item placement="top" timestamp="2019年">
                <el-card>
                  <h4>参与京东服务市场商品搬家应用开发，参与京东服务市场商品搬家应用开发，参与拼多多服务市场订单应用开发</h4>
                </el-card>
              </el-timeline-item>
              <el-timeline-item placement="top" timestamp="2020年">
                <el-card>
                  <h4>所参与开发的拼多多订单应用排名服务市场类目第一，开始快手服务市场订单应用开发</h4>
                </el-card>
              </el-timeline-item>
              <el-timeline-item placement="top" timestamp="2021年">
                <el-card>
                  <h4>日处理拼多多订单200万条，开始美团、饿了么应用市场应用开发</h4>
                </el-card>
              </el-timeline-item>
              <el-timeline-item placement="top" timestamp="2022年">
                <el-card>
                  <h4>累计服务10万+电商平台店铺、5万+外卖店铺。开始抖音、淘宝服务市场订单应用开发，开源ruoyi-wms</h4>
                </el-card>
              </el-timeline-item>
              <el-timeline-item placement="top" timestamp="2023年">
                <el-card>
                  <h4>B站播放量破万，开源ruoyi-mall，公众号粉丝破万，wms-saas火热研发中</h4>
                </el-card>
              </el-timeline-item>
            </el-timeline>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup name="Index">
import * as echarts from 'echarts'
import { getDashboardTrend, getDashboardSummary } from '@/api/wms/dashboard'
import request from '@/utils/request'

const version = ref('5.2.0')
const { proxy } = getCurrentInstance()

function goTarget(url) {
  window.open(url, '__blank')
}

// ===== 库存快照 =====
const snapshotLoading = ref(false)

async function createSnapshot() {
  proxy.$modal.confirm('确认立即创建库存快照？').then(async () => {
    snapshotLoading.value = true
    try {
      await request({ url: '/wms/inventorySnapshot/snapshot', method: 'post' })
      proxy.$modal.msgSuccess('快照创建成功')
    } catch (e) {
      proxy.$modal.msgError('快照创建失败')
    } finally {
      snapshotLoading.value = false
    }
  }).catch(() => {})
}

function goInventoryWarning() {
  proxy.$router.push('/wms/inventory')
}

function goInventorySnapshot() {
  proxy.$router.push('/wms/inventorySnapshot');
}

// ===== 数据大屏增强 =====
const trendChartRef = ref(null)
let trendChart = null
const trendData = ref({ dates: [], inboundQuantities: [], outboundQuantities: [] })
const summary = ref({})

const locationPercentage = computed(() => {
  const total = summary.value.totalLocations || 0
  if (!total) return 0
  return Math.round((summary.value.occupiedLocations || 0) / total * 100)
})

const emptyLocations = computed(() => {
  return (summary.value.totalLocations || 0) - (summary.value.occupiedLocations || 0)
})

const locationColors = [
  { color: '#67c23a', percentage: 30 },
  { color: '#e6a23c', percentage: 60 },
  { color: '#f56c6c', percentage: 80 },
  { color: '#f56c6c', percentage: 100 }
]

function initTrendChart() {
  if (!trendChartRef.value) return
  trendChart = echarts.init(trendChartRef.value)
  updateTrendChart()
}

function updateTrendChart() {
  if (!trendChart) return
  const data = trendData.value
  trendChart.setOption({
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'cross' }
    },
    legend: {
      data: ['入库数量', '出库数量'],
      bottom: 0
    },
    grid: {
      top: '8%',
      left: '3%',
      right: '4%',
      bottom: '12%',
      containLabel: true
    },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: data.dates,
      axisLabel: { color: '#606266' }
    },
    yAxis: {
      type: 'value',
      name: '数量',
      axisLabel: { color: '#606266' },
      splitLine: { lineStyle: { color: '#ebeef5' } }
    },
    series: [
      {
        name: '入库数量',
        type: 'line',
        smooth: true,
        data: data.inboundQuantities,
        itemStyle: { color: '#409eff' },
        lineStyle: { width: 3 },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(64, 158, 255, 0.3)' },
            { offset: 1, color: 'rgba(64, 158, 255, 0.02)' }
          ])
        }
      },
      {
        name: '出库数量',
        type: 'line',
        smooth: true,
        data: data.outboundQuantities,
        itemStyle: { color: '#67c23a' },
        lineStyle: { width: 3 },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(103, 194, 58, 0.3)' },
            { offset: 1, color: 'rgba(103, 194, 58, 0.02)' }
          ])
        }
      }
    ]
  })
}

function handleResize() {
  if (trendChart) {
    trendChart.resize()
  }
}

async function loadDashboardData() {
  try {
    const [trendRes, summaryRes] = await Promise.all([
      getDashboardTrend(),
      getDashboardSummary()
    ])
    if (trendRes.code === 200) {
      trendData.value = trendRes.data
      updateTrendChart()
    }
    if (summaryRes.code === 200) {
      summary.value = summaryRes.data
    }
  } catch (e) {
    // 接口未就绪时静默处理，不影响页面其余内容
    console.warn('Dashboard data load failed', e)
  }
}

onMounted(() => {
  loadDashboardData()
  nextTick(() => {
    initTrendChart()
  })
  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  if (trendChart) {
    trendChart.dispose()
    trendChart = null
  }
})
</script>

<style scoped lang="scss">
.home {
  blockquote {
    padding: 10px 20px;
    margin: 0 0 20px;
    font-size: 17.5px;
    border-left: 5px solid #eee;
  }
  hr {
    margin-top: 20px;
    margin-bottom: 20px;
    border: 0;
    border-top: 1px solid #eee;
  }
  .col-item {
    margin-bottom: 20px;
  }

  ul {
    padding: 0;
    margin: 0;
  }

  font-family: "open sans", "Helvetica Neue", Helvetica, Arial, sans-serif;
  font-size: 13px;
  color: #676a6c;
  overflow-x: hidden;

  ul {
    list-style-type: none;
  }

  h4 {
    margin-top: 0px;
  }

  h2 {
    margin-top: 10px;
    font-size: 26px;
    font-weight: 100;
  }

  p {
    margin-top: 10px;

    b {
      font-weight: 700;
    }
  }

  .update-log {
    ol {
      display: block;
      list-style-type: decimal;
      margin-block-start: 1em;
      margin-block-end: 1em;
      margin-inline-start: 0;
      margin-inline-end: 0;
      padding-inline-start: 40px;
    }
  }
}

/* ===== 数据大屏增强样式 ===== */
.summary-card-wrap {
  :deep(.el-card__body) {
    padding: 20px;
  }
}

.summary-card {
  display: flex;
  align-items: center;
  gap: 16px;
}

.summary-icon {
  width: 56px;
  height: 56px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  flex-shrink: 0;
}

.inbound-bg {
  background: linear-gradient(135deg, #409eff, #66b1ff);
}

.outbound-bg {
  background: linear-gradient(135deg, #67c23a, #85ce61);
}

.inbound-qty-bg {
  background: linear-gradient(135deg, #e6a23c, #f0c78a);
}

.outbound-qty-bg {
  background: linear-gradient(135deg, #f56c6c, #f89898);
}

.summary-info {
  flex: 1;
  min-width: 0;
}

.summary-value {
  font-size: 28px;
  font-weight: bold;
  color: #303133;
  line-height: 1.2;
}

.summary-label {
  font-size: 13px;
  color: #909399;
  margin-top: 4px;
}

.card-header-title {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 16px;
  font-weight: bold;
  color: #303133;
}

.location-util-wrap {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 20px 0;
  gap: 20px;
}

.location-percentage-text {
  font-size: 22px;
  font-weight: bold;
  color: #303133;
}

.location-detail {
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.location-row {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  color: #606266;
}

.location-row b {
  color: #303133;
  font-size: 16px;
}

.dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  flex-shrink: 0;
}

.occupied-dot {
  background: #f56c6c;
}

.empty-dot {
  background: #67c23a;
}

.total-dot {
  background: #409eff;
}

/* ===== 库存预警 & 快照样式 ===== */
.warning-card {
  :deep(.el-card__header) {
    background: #fef0f0;
  }
}

.warning-badge {
  margin-left: 8px;
}

.warning-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 20px 0;
  gap: 8px;
  color: #67c23a;
}

.warning-empty p {
  font-size: 14px;
  color: #909399;
  margin: 0;
}

.warning-list {
  padding: 10px 0;
}

.snapshot-actions {
  display: flex;
  gap: 12px;
  padding: 10px 0;
}
</style>
