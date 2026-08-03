import request from '@/utils/request'

// 获取数据库状态信息
export function getDatabaseStatus() {
  return request({
    url: '/system/database/status',
    method: 'get'
  })
}

// 初始化数据库（重置为初始状态）
export function initDatabase(data) {
  return request({
    url: '/system/database/init',
    method: 'post',
    data: data
  })
}
