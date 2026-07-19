const DIALOG_CANCELLATIONS = new Set(['cancel', 'close'])

export const isDialogCancellation = (reason: unknown): boolean =>
  typeof reason === 'string' && DIALOG_CANCELLATIONS.has(reason)

export const resolveDialogAction = async <T>(action: Promise<T>): Promise<T | undefined> => {
  try {
    return await action
  } catch (reason) {
    if (isDialogCancellation(reason)) return undefined
    throw reason
  }
}
