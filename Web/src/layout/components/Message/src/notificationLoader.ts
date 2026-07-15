export interface NotificationLoader<T> {
  peek: () => T | undefined
  load: (refresh?: boolean) => Promise<T>
  clear: () => void
}

/**
 * Small stale-while-refresh loader for the header notification popover.
 * It keeps the last successful value for immediate rendering and coalesces rapid repeated clicks.
 */
export const createNotificationLoader = <T>(fetcher: () => Promise<T>): NotificationLoader<T> => {
  let cached: T | undefined
  let inFlight: Promise<T> | undefined

  const load = (refresh = false): Promise<T> => {
    if (inFlight) {
      return inFlight
    }
    if (!refresh && cached !== undefined) {
      return Promise.resolve(cached)
    }

    const request = fetcher()
      .then((value) => {
        cached = value
        return value
      })
      .finally(() => {
        if (inFlight === request) {
          inFlight = undefined
        }
      })
    inFlight = request
    return request
  }

  return {
    peek: () => cached,
    load,
    clear: () => {
      cached = undefined
    }
  }
}
