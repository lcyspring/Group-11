const normalizeOrigin = (origin) => origin.trim().replace(/\/+$/, '');

const parseOrigins = (origins) =>
  String(origins || '')
    .split(',')
    .map(normalizeOrigin)
    .filter(Boolean);

/**
 * Convert media URLs from retired demo file domains into deployable URLs.
 *
 * In local static mode, bundled `/static/**` assets are served by the current
 * Mall origin. Other files from a retired origin cannot be recovered from the
 * repository, so a configured local placeholder is used instead.
 */
export const normalizeLegacyMediaUrl = (
  url,
  { staticMode = '', legacyOrigins = '', fallbackUrl = '' } = {},
) => {
  if (!url || staticMode !== 'local') return url || '';

  const origin = parseOrigins(legacyOrigins).find(
    (candidate) => url === candidate || url.startsWith(`${candidate}/`),
  );
  if (!origin) return url;

  const path = url.slice(origin.length) || '/';
  if (path.startsWith('/static/')) return path;
  return fallbackUrl || url;
};
