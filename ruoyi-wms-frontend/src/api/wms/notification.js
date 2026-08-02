import request from '@/utils/request'

// 查询消息通知列表
export function listNotification(query) {
  return request({
    url: '/wms/notification/list',
    method: 'get',
    params: query
  })
}

// 查询未读消息数量
export function unreadCount() {
  return request({
    url: '/wms/notification/unread/count',
    method: 'get'
  })
}

// 标记单条消息为已读
export function markAsRead(id) {
  return request({
    url: '/wms/notification/read/' + id,
    method: 'put'
  })
}

// 标记全部消息为已读
export function markAllAsRead() {
  return request({
    url: '/wms/notification/readAll',
    method: 'put'
  })
}

// 删除消息
export function delNotification(ids) {
  return request({
    url: '/wms/notification/' + ids,
    method: 'delete'
  })
}
