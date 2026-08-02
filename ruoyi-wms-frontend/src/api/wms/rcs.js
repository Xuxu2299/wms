import request from '@/utils/request'

// 按订单号批量撤销 RCS 任务
// taskType: 0=入库, 1=出库, 2=移库
export function cancelTaskByOrder(data) {
  return request({
    url: '/wms/rcs/cancelTaskByOrder',
    method: 'post',
    data: data
  })
}
