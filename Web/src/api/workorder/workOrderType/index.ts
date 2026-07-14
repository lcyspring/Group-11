import request from '@/config/axios'

export interface WorkOrderTypeVO {
  id?: number
  name: string
  code: string
  description?: string
  sort?: number
  status: number
  createTime?: Date
  updateTime?: Date
}

// 查询工单类型分页
export const getWorkOrderTypePage = (params: PageParam) => {
  return request.get({ url: '/workorder/work-order-type/page', params })
}

// 获得工单类型详情
export const getWorkOrderType = (id: number) => {
  return request.get({ url: '/workorder/work-order-type/get?id=' + id })
}

// 获得所有启用的工单类型列表
export const getEnableWorkOrderTypeList = () => {
  return request.get({ url: '/workorder/work-order-type/list-all' })
}

// 新增工单类型
export const createWorkOrderType = (data: WorkOrderTypeVO) => {
  return request.post({ url: '/workorder/work-order-type/create', data })
}

// 修改工单类型
export const updateWorkOrderType = (data: WorkOrderTypeVO) => {
  return request.put({ url: '/workorder/work-order-type/update', data })
}

// 删除工单类型
export const deleteWorkOrderType = (id: number) => {
  return request.delete({ url: '/workorder/work-order-type/delete?id=' + id })
}
