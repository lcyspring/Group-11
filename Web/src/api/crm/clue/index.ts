import request from '@/config/axios'
import { TransferReqVO } from '@/api/crm/permission'

export interface ClueVO {
  id: number // 编号
  name: string // 线索名称
  followUpStatus: boolean // 跟进状态
  contactLastTime: Date // 最后跟进时间
  contactLastContent: string // 最后跟进内容
  contactNextTime: Date // 下次联系时间
  ownerUserId: number // 负责人的用户编号
  ownerUserName?: string // 负责人的用户名称
  ownerUserDept?: string // 负责人的部门名称
  poolStatus: number // 池状态：0 在管，1 公共线索
  poolEntryTime?: Date // 本次进入公共池时间
  poolPreviousOwnerUserId?: number // 前负责人
  poolPreviousOwnerUserName?: string // 前负责人名称
  poolReason?: string // 入池原因编码
  poolReasonDetail?: string // 入池原因详情
  poolCycleCount: number // 累计入池次数
  transformStatus: boolean // 转化状态
  customerId: number // 客户编号
  customerName?: string // 客户名称
  mobile: string // 手机号
  telephone: string // 电话
  qq: string // QQ
  wechat: string // wechat
  email: string // email
  areaId: number // 所在地
  areaName?: string // 所在地名称
  detailAddress: string // 详细地址
  industryId: number // 所属行业
  level: number // 客户等级
  source: number // 客户来源
  remark: string // 备注
  creator: string // 创建人
  creatorName?: string // 创建人名称
  createTime: Date // 创建时间
  updateTime: Date // 更新时间
}

export interface ClueTransformReqVO {
  id: number
  contactName: string
  contactMobile: string
}

// 查询线索列表
export const getCluePage = async (params: any) => {
  return await request.get({ url: `/crm/clue/page`, params })
}

// 查询线索详情
export const getClue = async (id: number) => {
  return await request.get({ url: `/crm/clue/get?id=` + id })
}

// 新增线索
export const createClue = async (data: ClueVO) => {
  return await request.post({ url: `/crm/clue/create`, data })
}

// 修改线索
export const updateClue = async (data: ClueVO) => {
  return await request.put({ url: `/crm/clue/update`, data })
}

// 删除线索
export const deleteClue = async (id: number) => {
  return await request.delete({ url: `/crm/clue/delete?id=` + id })
}

// 导出线索 Excel
export const exportClue = async (params) => {
  return await request.download({ url: `/crm/clue/export-excel`, params })
}

// 线索转移
export const transferClue = async (data: TransferReqVO) => {
  return await request.put({ url: '/crm/clue/transfer', data })
}

// 线索转化为客户
export const transformClue = async (data: ClueTransformReqVO) => {
  return await request.put({ url: '/crm/clue/transform', data })
}

// 获得分配给我的、待跟进的线索数量
export const getFollowClueCount = async () => {
  return await request.get({ url: '/crm/clue/follow-count' })
}

// 查询公共线索列表
export const getPublicCluePage = async (params: any) => {
  return await request.get({ url: '/crm/clue/public-page', params })
}

// 将负责的线索放入公共线索池
export const putCluePublic = async (data: { clueId: number; reason: string }) => {
  return await request.put({ url: '/crm/clue/put-public', data })
}

// 自助领取公共线索
export const claimPublicClues = async (clueIds: number[]) => {
  return await request.put({ url: '/crm/clue/claim-public', data: { clueIds } })
}

// 主管分配公共线索
export const assignPublicClues = async (clueIds: number[], ownerUserId: number) => {
  return await request.put({ url: '/crm/clue/assign-public', data: { clueIds, ownerUserId } })
}
