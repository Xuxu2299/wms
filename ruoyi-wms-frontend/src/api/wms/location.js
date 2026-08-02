import request from '@/utils/request'

// 查询入库起点库位（R1/R2）
export function listReceiptStart() {
  return request({
    url: '/wms/location/receiptStart',
    method: 'get'
  })
}

// 查询 A 区空库位（入库终点可选）
export function listEmptyStorage() {
  return request({
    url: '/wms/location/empty',
    method: 'get'
  })
}

// 查询 A 区有货库位（出库起点可选）
export function listOccupiedStorage() {
  return request({
    url: '/wms/location/occupied',
    method: 'get'
  })
}

// 查询出库终点库位（C1/C2）
export function listShipmentEnd() {
  return request({
    url: '/wms/location/shipmentEnd',
    method: 'get'
  })
}

// 生成唯一容器号
export function generateContainerNo() {
  return request({
    url: '/wms/location/generateContainerNo',
    method: 'get'
  })
}

// 根据库位编码查询容器号
export function getContainerNoByLocation(locationCode) {
  return request({
    url: '/wms/location/containerNo',
    method: 'get',
    params: { locationCode }
  })
}

// 查询指定SKU在各库位的剩余库存（出库自动拆分用）
export function listInventoryBySku(skuId) {
  return request({
    url: '/wms/location/inventoryBySku',
    method: 'get',
    params: { skuId }
  })
}
