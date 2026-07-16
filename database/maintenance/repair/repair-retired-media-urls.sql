-- Remove retired cross-origin avatar URLs from local database state.
-- Frontend normalization remains as a defense in depth; this repair makes new sessions safe for all clients.
SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;
UPDATE system_users
SET avatar = ''
WHERE avatar LIKE 'http://test.yudao.iocoder.cn/%'
   OR avatar LIKE 'https://test.yudao.iocoder.cn/%';
UPDATE member_user
SET avatar = ''
WHERE avatar LIKE 'http://test.yudao.iocoder.cn/%'
   OR avatar LIKE 'https://test.yudao.iocoder.cn/%';
