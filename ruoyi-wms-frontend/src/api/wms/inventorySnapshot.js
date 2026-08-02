import request from '@/utils/request'

// 查询库存快照列表
export function listInventorySnapshot(date) {
  return request({
    url: '/wms/inventorySnapshot/list',
    method: 'get',
    params: { date }
  })
}

// 查询可用快照日期列表
export function listSnapshotDates() {
  return request({
    url: '/wms/inventorySnapshot/dates',
    method: 'get'
  })
}

// 手动创建快照
export function createSnapshot() {
  return request({
    url: '/wms/inventorySnapshot/snapshot',
    method: 'post'
  })
}
