# 构建工具配置

## 1. Maven

### 根 pom.xml 关键配置
```xml
<groupId>com.meession.etm</groupId>
<artifactId>mitedtsm</artifactId>
<version>${revision}</version>
<packaging>pom</packaging>

<properties>
    <revision>2026.01-SNAPSHOT</revision>
    <java.version>17</java.version>
    <maven.compiler.source>17</maven.compiler.source>
    <maven.compiler.target>17</maven.compiler.target>
    <spring.boot.version>3.5.9</spring.boot.version>
</properties>
```

### 构建命令
```bash
# 全量构建 (跳过测试)
mvn clean package -DskipTests

# 单模块构建
mvn clean package -pl mitedtsm-module-crm/mitedtsm-module-crm-biz -am -DskipTests

# 运行测试
mvn test
mvn test -pl mitedtsm-module-system/mitedtsm-module-system-biz -Dtest=AdminAuthServiceImplTest
```

## 2. Vite (Web Admin)

### vite.config.ts 关键配置
```typescript
export default defineConfig({
  server: {
    port: 3000,
    proxy: {
      '/admin-api': 'http://localhost:8080',
      '/app-api': 'http://localhost:8080',
    }
  }
})
```

### 构建模式
```bash
npm run build:local    # VITE_PORT=3000, dist/
npm run build:dev      # Dev环境
npm run build:test     # Test环境
npm run build:stage    # Staging环境
npm run build:prod     # 生产环境 dist-prod/
```

### 环境文件
```
.env            # 默认配置 (VITE_PORT, 加密密钥, 默认账户)
.env.local      # 本地开发 (VITE_CAPTCHA_ENABLE=false)
.env.dev        # 开发环境 (连接Docker后端)
.env.test       # 测试环境
.env.prod       # 生产环境 (VITE_BASE_URL='https://www.meession.com.cn:8080')
```

## 3. pnpm (AdminMobile)

### package.json 配置
```json
{
  "packageManager": "pnpm@10.10.0",
  "engines": {
    "node": ">=20",
    "pnpm": ">=9"
  }
}
```

### 构建命令
```bash
pnpm dev:h5             # H5 开发
pnpm dev:mp-weixin      # 微信小程序开发
pnpm dev:app            # App 开发
pnpm build:h5:prod      # H5 生产构建
```

## 4. 代码质量工具

### Web Admin
```bash
npm run lint:eslint     # ESLint 检查+自动修复
npm run lint:format     # Prettier 格式化
npm run lint:style      # Stylelint 修复
npm run ts:check        # TypeScript 类型检查
```
