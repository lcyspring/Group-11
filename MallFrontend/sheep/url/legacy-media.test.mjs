import assert from 'node:assert/strict';
import test from 'node:test';

import { normalizeLegacyMediaUrl } from './legacy-media.mjs';

const config = {
  staticMode: 'local',
  legacyOrigins: 'http://test.yudao.iocoder.cn, https://retired.example.test/',
  fallbackUrl: '/static/goods-empty.png',
};

test('localizes bundled static assets from a configured legacy origin', () => {
  assert.equal(
    normalizeLegacyMediaUrl('http://test.yudao.iocoder.cn/static/img/diy/banner-01.png', config),
    '/static/img/diy/banner-01.png',
  );
});

test('uses the configured placeholder for unavailable uploaded demo media', () => {
  assert.equal(
    normalizeLegacyMediaUrl('http://test.yudao.iocoder.cn/obsolete-product.jpg', config),
    '/static/goods-empty.png',
  );
});

test('preserves active remote media and non-local deployments', () => {
  const activeUrl = 'https://static.iocoder.cn/mall/banner-01.jpg';
  assert.equal(normalizeLegacyMediaUrl(activeUrl, config), activeUrl);
  assert.equal(
    normalizeLegacyMediaUrl('http://test.yudao.iocoder.cn/static/img/diy/banner-01.png', {
      ...config,
      staticMode: 'https://cdn.example.test',
    }),
    'http://test.yudao.iocoder.cn/static/img/diy/banner-01.png',
  );
});

test('does not confuse look-alike host names with the configured origin', () => {
  const lookAlike = 'http://test.yudao.iocoder.cn.evil.example/static/img/diy/banner-01.png';
  assert.equal(normalizeLegacyMediaUrl(lookAlike, config), lookAlike);
});

test('keeps empty and unconfigured values safe', () => {
  assert.equal(normalizeLegacyMediaUrl('', config), '');
  assert.equal(normalizeLegacyMediaUrl(undefined, config), '');
  assert.equal(
    normalizeLegacyMediaUrl('http://test.yudao.iocoder.cn', config),
    '/static/goods-empty.png',
  );
  assert.equal(
    normalizeLegacyMediaUrl('http://test.yudao.iocoder.cn/obsolete-product.jpg', {
      staticMode: 'local',
      legacyOrigins: 'http://test.yudao.iocoder.cn',
    }),
    'http://test.yudao.iocoder.cn/obsolete-product.jpg',
  );
  assert.equal(
    normalizeLegacyMediaUrl('https://active.example.test/image.png', {
      staticMode: 'local',
      legacyOrigins: '',
      fallbackUrl: '/static/goods-empty.png',
    }),
    'https://active.example.test/image.png',
  );
});
