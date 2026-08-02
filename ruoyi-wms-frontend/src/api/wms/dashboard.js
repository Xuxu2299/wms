import request from '@/utils/request'

// 查询近7天出入库趋势
export function getDashboardTrend() {
  return request({
    url: '/wms/dashboard/trend',
    method: 'get'
  })
}

// 查询库位利用率与今日出入库汇总
export function getDashboardSummary() {
  return request({
    url: '/wms/dashboard/summary',
    method: 'get'
  })
}
