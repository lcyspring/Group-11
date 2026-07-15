import request from '@/config/axios'

export interface ExpenseCategoryVO {
  id?: number
  code: string
  name: string
  status: number
  sort: number
  description?: string
}

export interface ReimbursementItemVO {
  id?: number
  categoryId: number
  categoryName?: string
  occurredDate: string
  amount: number
  description: string
  invoiceNo?: string
  attachmentUrls?: string[]
  sort?: number
}

export interface ReimbursementVO {
  id?: number
  no?: string
  applicantUserId?: number
  applicantUserName?: string
  departmentId?: number
  customerId?: number
  customerName?: string
  contractId?: number
  contractNo?: string
  contractName?: string
  currency?: string
  totalAmount?: number
  expenseStartDate: string
  expenseEndDate: string
  reason: string
  remark?: string
  processInstanceId?: string
  auditStatus?: number
  version?: number
  createTime?: string
  updateTime?: string
  items: ReimbursementItemVO[]
}

export interface ReimbursementActionVO {
  id: number
  actionType: number
  fromStatus?: number
  toStatus?: number
  amountSnapshot: number
  operatorUserId?: number
  operatorUserName?: string
  actionTime: string
  processInstanceId?: string
  remark?: string
}

export const getReimbursementPage = (params: any) =>
  request.get({ url: '/crm/reimbursement/page', params })
export const getReimbursement = (id: number) =>
  request.get<ReimbursementVO>({ url: '/crm/reimbursement/get', params: { id } })
export const createReimbursement = (data: ReimbursementVO) =>
  request.post<number>({ url: '/crm/reimbursement/create', data })
export const updateReimbursement = (data: ReimbursementVO) =>
  request.put({ url: '/crm/reimbursement/update', data })
export const deleteReimbursement = (id: number) =>
  request.delete({ url: '/crm/reimbursement/delete', params: { id } })
export const submitReimbursement = (id: number) =>
  request.put({ url: '/crm/reimbursement/submit', params: { id } })
export const getActionRecords = (reimbursementId: number) =>
  request.get<ReimbursementActionVO[]>({
    url: '/crm/reimbursement/action-records',
    params: { reimbursementId }
  })
export const uploadAttachment = (reimbursementId: number, file: File) => {
  const data = new FormData()
  data.append('file', file)
  return request.upload({
    url: '/crm/reimbursement/attachment/upload',
    params: { reimbursementId },
    data
  }).then((response: any) => response.data as string)
}

export const getExpenseCategoryList = (status?: number) =>
  request.get<ExpenseCategoryVO[]>({ url: '/crm/reimbursement/category/list', params: { status } })
export const createExpenseCategory = (data: ExpenseCategoryVO) =>
  request.post<number>({ url: '/crm/reimbursement/category/create', data })
export const updateExpenseCategory = (data: ExpenseCategoryVO) =>
  request.put({ url: '/crm/reimbursement/category/update', data })
export const deleteExpenseCategory = (id: number) =>
  request.delete({ url: '/crm/reimbursement/category/delete', params: { id } })
