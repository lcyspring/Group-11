import request from '@/config/axios'

export interface LoanVO {
  id?: number
  type: string
  amount: number
  reason: string
  tripId?: number
  employeeLevel?: string
  approvalLimit?: number
  escalatedApproval?: boolean
  outstandingAmount?: number
  repaymentStatus?: number
  status?: number
  processInstanceId?: string
  approvalTime?: string
  repaidTime?: string
  createTime?: string
  startUserSelectAssignees?: Record<string, number[]>
}

export interface LoanRepaymentVO {
  id?: number
  loanId: number
  amount: number
  repaidAt?: string | number
  referenceNo?: string
  remark?: string
  createTime?: string
}
export interface LoanLimitVO { employeeLevel: string; approvalLimit: number }

export const createLoan = (data: LoanVO) => request.post<number>({ url: '/bpm/oa/loan/create', data })
export const getLoan = (id: number) => request.get<LoanVO>({ url: '/bpm/oa/loan/get', params: { id } })
export const getLoanPage = (params: PageParam) => request.get({ url: '/bpm/oa/loan/page', params })
export const createRepayment = (data: LoanRepaymentVO) => request.post<number>({ url: '/bpm/oa/loan/repayment/create', data })
export const getRepayments = (loanId: number) => request.get<LoanRepaymentVO[]>({ url: '/bpm/oa/loan/repayment/list', params: { loanId } })
export const getMyLimit = () => request.get<LoanLimitVO>({ url: '/bpm/oa/loan/my-limit' })
