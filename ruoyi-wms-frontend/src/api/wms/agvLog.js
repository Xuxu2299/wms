import request from '@/utils/request'

// 查询AGV日志列表
export function listAgvLog(query) {
  return request({
    url: '/wms/agvLog/list',
    method: 'get',
    params: query
  })
}

// 查询AGV日志详细
export function getAgvLog(id) {
  return request({
    url: '/wms/agvLog/' + id,
    method: 'get'
  })
}

// 删除AGV日志
export function delAgvLog(id) {
  return request({
    url: '/wms/agvLog/' + id,
    method: 'delete'
  })
}

// 清空AGV日志
export function cleanAgvLog() {
  return request({
    url: '/wms/agvLog/clean',
    method: 'delete'
  })
}
