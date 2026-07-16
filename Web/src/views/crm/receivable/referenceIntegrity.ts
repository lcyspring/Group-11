export enum ReceivableReferenceStatus {
  VALID = 0,
  CUSTOMER_MISSING = 10,
  CONTRACT_INVALID = 20,
  BOTH_INVALID = 30
}

export const hasReferenceIssue = (status?: number) =>
  status !== undefined && status !== ReceivableReferenceStatus.VALID

export const isCustomerReferenceMissing = (status?: number) =>
  status === ReceivableReferenceStatus.CUSTOMER_MISSING ||
  status === ReceivableReferenceStatus.BOTH_INVALID

export const isContractReferenceInvalid = (status?: number) =>
  status === ReceivableReferenceStatus.CONTRACT_INVALID ||
  status === ReceivableReferenceStatus.BOTH_INVALID

export const referenceStatusLocaleKey = (status?: number) => {
  switch (status) {
    case ReceivableReferenceStatus.CUSTOMER_MISSING:
      return 'receivable.referenceCustomerMissing'
    case ReceivableReferenceStatus.CONTRACT_INVALID:
      return 'receivable.referenceContractInvalid'
    case ReceivableReferenceStatus.BOTH_INVALID:
      return 'receivable.referenceBothInvalid'
    default:
      return 'receivable.referenceValid'
  }
}
