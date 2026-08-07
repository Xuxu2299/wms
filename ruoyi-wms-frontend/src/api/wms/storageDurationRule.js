import request from '@/utils/request';

/**
 * 查询存储时长自动移库规则列表
 * @param query
 */
export function listStorageDurationRule(query) {
  return request({
    url: '/wms/storageDurationRule/list',
    method: 'get',
    params: query
  });
}

/**
 * 查询存储时长自动移库规则列表（不分页）
 * @param query
 */
export function listStorageDurationRuleNoPage(query) {
  return request({
    url: '/wms/storageDurationRule/listNoPage',
    method: 'get',
    params: query
  });
}

/**
 * 查询存储时长自动移库规则详细
 * @param id
 */
export function getStorageDurationRule(id) {
  return request({
    url: '/wms/storageDurationRule/' + id,
    method: 'get'
  });
}

/**
 * 新增存储时长自动移库规则
 * @param data
 */
export function addStorageDurationRule(data) {
  return request({
    url: '/wms/storageDurationRule',
    method: 'post',
    data: data
  });
}

/**
 * 修改存储时长自动移库规则
 * @param data
 */
export function updateStorageDurationRule(data) {
  return request({
    url: '/wms/storageDurationRule',
    method: 'put',
    data: data
  });
}

/**
 * 删除存储时长自动移库规则
 * @param id
 */
export function delStorageDurationRule(id) {
  return request({
    url: '/wms/storageDurationRule/' + id,
    method: 'delete'
  });
}

/**
 * 预览：查询命中某规则的超期库存
 * @param id 规则ID
 */
export function previewStorageDurationRule(id) {
  return request({
    url: '/wms/storageDurationRule/preview/' + id,
    method: 'get'
  });
}
