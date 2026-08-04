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

// 查询所有库位（管理页面用）
export function listAllLocations() {
  return request({
    url: '/wms/location/listAll',
    method: 'get'
  })
}

// 释放库位（重置为空位，清除容器号）
export function releaseLocations(locationCodes) {
  return request({
    url: '/wms/location/release',
    method: 'put',
    data: { locationCodes }
  })
}

// 新增库位
export function addLocation(data) {
  return request({
    url: '/wms/location/save',
    method: 'post',
    data
  })
}

// 修改库位
export function updateLocation(data) {
  return request({
    url: '/wms/location/update',
    method: 'put',
    data
  })
}

// 删除库位
export function deleteLocation(id) {
  return request({
    url: '/wms/location/' + id,
    method: 'delete'
  })
}
