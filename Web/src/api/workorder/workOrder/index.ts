import request from '@/config/axios'

export interface WorkOrderVO {
  id?: number
  title: string
  content?: string
  typeId?: number
  typeName?: string
  priority?: number
  priorityName?: string
  status?: number
  statusName?: string
  handlerUserId?: number
  handlerUserName?: string
  submitterUserId?: number
  submitterUserName?: string
  result?: string
  handleTime?: Date
  expectedFinishTime?: Date
  finishTime?: Date
  customerId?: number
  businessId?: number
  remark?: string
  creator?: string
  createTime?: Date
  updateTime?: Date
}

export interface WorkOrderUpdateStatusVO {
  id: number
  status: number
  result?: string
}

// 查询工单分页
export const getWorkOrderPage = (params: PageParam) => {
  return request.get({ url: '/workorder/work-order/page', params })
}

// 获得工单详情
export const getWorkOrder = (id: number) => {
  return request.get({ url: '/workorder/work-order/get?id=' + id })
}

// 新增工单
export const createWorkOrder = (data: WorkOrderVO) => {
  return request.post({ url: '/workorder/work-order/create', data })
}

// 修改工单
export const updateWorkOrder = (data: WorkOrderVO) => {
  return request.put({ url: '/workorder/work-order/update', data })
}

// 更新工单状态
export const updateWorkOrderStatus = (data: WorkOrderUpdateStatusVO) => {
  return request.put({ url: '/workorder/work-order/update-status', data })
}

// 删除工单
export const deleteWorkOrder = (id: number) => {
  return request.delete({ url: '/workorder/work-order/delete?id=' + id })
}

// 导出工单 Excel
export const exportWorkOrderExcel = (params: PageParam) => {
  return request.download({ url: '/workorder/work-order/export-excel', params })
}
