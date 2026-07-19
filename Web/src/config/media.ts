import {
  normalizeLegacyMediaPayload,
  normalizeLegacyMediaUrl,
  parseLegacyMediaOrigins
} from '@/utils/legacyMedia'

const legacyMediaOrigins = parseLegacyMediaOrigins(
  import.meta.env.VITE_APP_LEGACY_MEDIA_ORIGINS
)

export const normalizeRuntimeMediaUrl = (value?: string | null) =>
  normalizeLegacyMediaUrl(value, legacyMediaOrigins)

export const normalizeRuntimeMediaPayload = <T>(value: T): T =>
  normalizeLegacyMediaPayload(value, legacyMediaOrigins)
