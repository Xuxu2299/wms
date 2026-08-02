import request from '@/utils/request'

// 创建波次
export function createWave(data) {
  return request({
    url: '/wms/wavePick/create',
    method: 'post',
    data
  })
}

// 查询波次详情
export function getWavePick(id) {
  return request({
    url: '/wms/wavePick/' + id,
    method: 'get'
  })
}

// 查询波次列表
export function listWavePick(query) {
  return request({
    url: '/wms/wavePick/list',
    method: 'get',
    params: query
  })
}

// 标记明细已拣
export function markPicked(detailId) {
  return request({
    url: '/wms/wavePick/pick/' + detailId,
    method: 'put'
  })
}

// 取消波次
export function cancelWavePick(id) {
  return request({
    url: '/wms/wavePick/cancel/' + id,
    method: 'put'
  })
}

// 删除波次
export function delWavePick(id) {
  return request({
    url: '/wms/wavePick/' + id,
    method: 'delete'
  })
}
