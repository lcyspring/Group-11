export const DISPATCH_MODE = {
  UNASSIGNED: 0,
  MANUAL: 1,
  AUTO: 2,
  CLAIM: 3,
  REASSIGN: 4
} as const

export const canClaimWorkOrder = (
  status?: number,
  handlerUserId?: number,
  groupId?: number,
  userId?: number,
  memberUserIds: number[] = []
) => status === 10 && !handlerUserId && Boolean(groupId) && Boolean(userId && memberUserIds.includes(userId))

export const normalizeCcUserIds = (ids?: Array<number | null | undefined>, max = 20) =>
  [...new Set((ids || []).filter((id): id is number => typeof id === 'number' && id > 0))].slice(0, max)

export const candidateLabel = (nickname: string, openCount: number) => `${nickname} · ${openCount}`

/** 只有处理组和处理人都未改变时才是无效改派；同一人员跨组改派属于有效业务操作。 */
export const isUnchangedAssignment = (
  currentGroupId?: number,
  currentHandlerUserId?: number,
  targetGroupId?: number,
  targetHandlerUserId?: number
) => currentGroupId === targetGroupId && currentHandlerUserId === targetHandlerUserId
