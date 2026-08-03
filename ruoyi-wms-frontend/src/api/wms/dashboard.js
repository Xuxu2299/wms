import request from '@/utils/request'

// 获取仪表盘汇总数据
export function getDashboardSummary() {
  return request({
    url: '/wms/dashboard/summary',
    method: 'get'
  })
}

// 获取出入库趋势数据
export function getDashboardTrend() {
  return request({
    url: '/wms/dashboard/trend',
    method: 'get'
  })
}
