import { test, expect, Page } from '@playwright/test';

const BASE_URL = process.env.BASE_URL || 'http://localhost:8081';
const API_URL = process.env.API_URL || 'http://localhost:8080';

const ADMIN = {
  username: 'admin',
  password: 'admin123',
};

async function adminLogin(page: Page) {
  await page.goto(`${BASE_URL}/login`);
  await page.fill('input[placeholder*="用户名"]', ADMIN.username);
  await page.fill('input[placeholder*="密码"]', ADMIN.password);
  await page.click('button[type="submit"]');
  await page.waitForURL('**/dashboard**', { timeout: 10000 });
}

async function getToken(): Promise<string> {
  const response = await fetch(`${API_URL}/admin-api/system/auth/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', 'tenant-id': '1' },
    body: JSON.stringify({ username: ADMIN.username, password: ADMIN.password }),
  });
  const data = await response.json();
  return data.data.token;
}

test.describe('E2E-001 登录认证', () => {
  test('TC-E2E-001-01 正常登录进入首页', async ({ page }) => {
    await page.goto(`${BASE_URL}/login`);
    await expect(page.locator('text=登录')).toBeVisible();

    await page.fill('input[placeholder*="用户名"]', ADMIN.username);
    await page.fill('input[placeholder*="密码"]', ADMIN.password);
    await page.click('button[type="submit"]');

    await expect(page).toHaveURL(/.*dashboard/);
    await expect(page.locator('text=管理员')).toBeVisible({ timeout: 5000 });
  });

  test('TC-E2E-001-02 错误密码登录提示', async ({ page }) => {
    await page.goto(`${BASE_URL}/login`);
    await page.fill('input[placeholder*="用户名"]', ADMIN.username);
    await page.fill('input[placeholder*="密码"]', 'wrong_password');
    await page.click('button[type="submit"]');

    await expect(page.locator('.el-message--error')).toBeVisible({ timeout: 5000 });
  });

  test('TC-E2E-001-03 退出登录', async ({ page }) => {
    await adminLogin(page);
    await page.click('.navbar-user, .user-avatar');
    await page.click('text=退出登录');
    await expect(page).toHaveURL(/.*login/);
  });

  test('TC-E2E-001-04 未登录路由守卫拦截', async ({ page }) => {
    await page.goto(`${BASE_URL}/dashboard`);
    await expect(page).toHaveURL(/.*login/);
  });
});

test.describe('E2E-002 用户管理', () => {
  test.beforeEach(async ({ page }) => { await adminLogin(page); });

  test('TC-E2E-002-01 查看用户列表', async ({ page }) => {
    await page.click('text=系统管理');
    await page.click('text=用户管理');
    await page.waitForSelector('.el-table');
    await expect(page.locator('.el-table')).toBeVisible();
  });

  test('TC-E2E-002-02 搜索用户', async ({ page }) => {
    await page.click('text=系统管理');
    await page.click('text=用户管理');
    await page.waitForSelector('.el-table');

    await page.fill('input[placeholder*="用户名"]', 'admin');
    await page.click('button:has-text("搜索")');
    await page.waitForResponse(response =>
      response.url().includes('/admin-api/system/user/page') && response.status() === 200
    );
  });
});

test.describe('E2E-003 角色权限管理', () => {
  test.beforeEach(async ({ page }) => { await adminLogin(page); });

  test('TC-E2E-003-01 查看角色列表', async ({ page }) => {
    await page.click('text=系统管理');
    await page.click('text=角色管理');
    await page.waitForSelector('.el-table');
    await expect(page.locator('.el-table')).toBeVisible();
  });
});

test.describe('E2E-007 CRM客户管理', () => {
  test.beforeEach(async ({ page }) => { await adminLogin(page); });

  test('TC-E2E-007-01 查看客户列表', async ({ page }) => {
    await page.click('text=客户管理');
    await page.click('text=客户列表');
    await page.waitForSelector('.el-table');
    await expect(page.locator('.el-table')).toBeVisible();
  });

  test('TC-E2E-007-02 新增客户', async ({ page }) => {
    await page.click('text=客户管理');
    await page.click('text=客户列表');
    await page.waitForSelector('.el-table');

    await page.click('button:has-text("新增")');
    await page.waitForSelector('.el-dialog');

    await page.fill('input[placeholder*="客户名称"]', `E2E客户_${Date.now()}`);
    await page.fill('input[placeholder*="行业"]', '信息技术');
    await page.fill('input[placeholder*="联系人"]', '张经理');
    await page.fill('input[placeholder*="联系电话"]', '13900000002');

    await page.click('.el-dialog button:has-text("确定")');
    await expect(page.locator('.el-message--success')).toBeVisible({ timeout: 5000 });
  });
});

test.describe('E2E-008 ERP进销存', () => {
  test.beforeEach(async ({ page }) => { await adminLogin(page); });

  test('TC-E2E-008-01 查看产品列表', async ({ page }) => {
    await page.click('text=进销存');
    await page.click('text=产品管理');
    await page.waitForSelector('.el-table');
    await expect(page.locator('.el-table')).toBeVisible();
  });

  test('TC-E2E-008-02 查看采购订单', async ({ page }) => {
    await page.click('text=进销存');
    await page.click('text=采购管理');
    await page.waitForSelector('.el-table');
    await expect(page.locator('.el-table')).toBeVisible();
  });
});

test.describe('E2E-009 BPM审批流程', () => {
  test.beforeEach(async ({ page }) => { await adminLogin(page); });

  test('TC-E2E-009-01 查看待办任务', async ({ page }) => {
    await page.click('text=审批中心');
    await page.click('text=待办任务');
    await page.waitForSelector('.el-table');
    await expect(page.locator('.el-table')).toBeVisible();
  });

  test('TC-E2E-009-02 查看已办任务', async ({ page }) => {
    await page.click('text=审批中心');
    await page.click('text=已办任务');
    await page.waitForSelector('.el-table');
    await expect(page.locator('.el-table')).toBeVisible();
  });
});

test.describe('E2E-010 Mall商城', () => {
  test.beforeEach(async ({ page }) => { await adminLogin(page); });

  test('TC-E2E-010-01 查看商品列表', async ({ page }) => {
    await page.click('text=商城管理');
    await page.click('text=商品管理');
    await page.waitForSelector('.el-table');
    await expect(page.locator('.el-table')).toBeVisible();
  });
});

test.describe('E2E-018 响应式与兼容性', () => {
  test('TC-E2E-018-01 移动端375视口', async ({ page }) => {
    await page.setViewportSize({ width: 375, height: 812 });
    await page.goto(`${BASE_URL}/login`);
    await expect(page.locator('text=登录')).toBeVisible();
  });

  test('TC-E2E-018-02 桌面1920视口', async ({ page }) => {
    await page.setViewportSize({ width: 1920, height: 1080 });
    await page.goto(`${BASE_URL}/login`);
    await expect(page.locator('text=登录')).toBeVisible();
  });
});