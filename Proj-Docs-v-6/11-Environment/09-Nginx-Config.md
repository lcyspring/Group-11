# Nginx 配置指南

> 项目: MITEDTSM (密讯ETM系统)
> 版本: v1.0
> 更新日期: 2026-03-14

---

## 目录结构

```
nginx/
├── conf.d/
│   └── mitedtsm.conf    # 主配置
├── ssl/
│   ├── fullchain.pem    # SSL证书
│   └── privkey.pem      # 私钥
└── logs/
    ├── access.log       # 访问日志
    └── error.log        # 错误日志
```

---

## 完整配置文件：mitedtsm.conf

```nginx
# ============================================
# MITEDTSM 企业管理系统 Nginx 配置
# ============================================

# 配置上游服务器（后端应用）
upstream mitedtsm_backend {
    server 127.0.0.1:8080;
    keepalive 64;
}

# ============================================
# HTTP 服务器（强制重定向 HTTPS）
# ============================================
server {
    listen 80;
    server_name your-domain.com;

    # HTTP 请求重定向到 HTTPS
    return 301 https://$server_name$request_uri;
}

# ============================================
# HTTPS 主站
# ============================================
server {
    listen 443 ssl http2;
    server_name your-domain.com;

    # SSL 配置
    ssl_certificate      /etc/nginx/ssl/fullchain.pem;
    ssl_certificate_key  /etc/nginx/ssl/privkey.pem;

    # SSL 安全优化
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers ECDHE-ECDSA-AES128-GCM-SHA256:ECDHE-RSA-AES128-GCM-SHA256:ECDHE-ECDSA-AES256-GCM-SHA384:ECDHE-RSA-AES256-GCM-SHA384:ECDHE-ECDSA-CHACHA20-POLY1305:ECDHE-RSA-CHACHA20-POLY1305;
    ssl_prefer_server_ciphers on;
    ssl_session_cache shared:SSL:10m;
    ssl_session_timeout 10m;

    # 日志
    access_log  /var/log/nginx/mitedtsm-access.log  main;
    error_log   /var/log/nginx/mitedtsm-error.log;

    # 客户端最大请求体（上传文件限制）
    client_max_body_size 100m;

    # 连接超时设置
    client_body_timeout   60s;
    client_header_timeout 60s;

    # ============================================
    # 静态资源：前端构建产物
    # ============================================
    location / {
        root   /usr/share/nginx/html;
        index  index.html index.htm;
        try_files $uri $uri/ /index.html;
        # Gzip 压缩
        gzip on;
        gzip_types text/plain text/css text/xml text/javascript application/json application/javascript application/x-javascript application/xml+rss;
        gzip_min_length 1024;
    }

    # ============================================
    # 静态资源缓存（css, js, img 等）
    # ============================================
    location ~* \.(js|css|png|jpg|jpeg|gif|ico|svg|woff|woff2|ttf|eot)$ {
        root /usr/share/nginx/html;
        expires 1y;
        add_header Cache-Control "public, immutable";
        access_log off;
    }

    # ============================================
    # 前端页面入口
    # ============================================
    location = /index.html {
        root   /usr/share/nginx/html;
        add_header Cache-Control "no-cache, no-store, must-revalidate";
        expires 0;
    }

    # ============================================
    # Admin API 代理到后端
    # ============================================
    location ^~ /admin-api/ {
        proxy_pass http://mitedtsm_backend/admin-api/;
        proxy_http_version 1.1;
        proxy_set_header Host $http_host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;

        # 连接超时
        proxy_connect_timeout 60s;
        proxy_send_timeout 60s;
        proxy_read_timeout 60s;

        # 启用连接复用
        proxy_set_header Connection '';
        chunked_transfer_encoding off;
        proxy_buffering off;
        proxy_cache off;
    }

    # ============================================
    # App API 代理到后端
    # ============================================
    location ^~ /app-api/ {
        proxy_pass http://mitedtsm_backend/app-api/;
        proxy_http_version 1.1;
        proxy_set_header Host $http_host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;

        proxy_connect_timeout 60s;
        proxy_send_timeout 60s;
        proxy_read_timeout 60s;

        proxy_set_header Connection '';
        chunked_transfer_encoding off;
        proxy_buffering off;
        proxy_cache off;
    }

    # ============================================
    # WebSocket 支持
    # ============================================
    location ^~ /ws/ {
        proxy_pass http://mitedtsm_backend/ws/;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
        proxy_set_header Host $http_host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    }

    # ============================================
    # Actuator 监控端点（仅内网访问）
    # ============================================
    location ^~ /actuator {
        allow 127.0.0.1;
        allow 192.168.0.0/16;
        allow 172.16.0.0/12;
        allow 10.0.0.0/8;
        deny all;
        proxy_pass http://mitedtsm_backend/actuator;
    }

    # ============================================
    # 禁止访问隐藏文件（.git, .env 等）
    # ============================================
    location ~ /\. {
        deny all;
        access_log off;
        log_not_found off;
    }

    # ============================================
    # 禁止访问备份文件
    # ============================================
    location ~* ~$ {
        deny all;
        access_log off;
        log_not_found off;
    }
}
```

---

## Gzip 全局配置（nginx.conf 片段）

```nginx
# /etc/nginx/nginx.conf

worker_processes  auto;
worker_rlimit_nofile 65535;

events {
    worker_connections  10240;
    use epoll;
    multi_accept on;
}

http {
    include       /etc/nginx/mime.types;
    default_type  application/octet-stream;

    # 日志格式
    log_format  main  '$remote_addr - $remote_user [$time_local] "$request" '
                      '$status $body_bytes_sent "$http_referer" '
                      '"$http_user_agent" "$http_x_forwarded_for"';

    # 开启高效传输模式
    sendfile            on;
    tcp_nopush          on;
    tcp_nodelay         on;

    keepalive_timeout   65;
    types_hash_max_size 2048;

    # 开启 Gzip
    gzip on;
    gzip_vary on;
    gzip_min_length 1000;
    gzip_types text/plain text/css text/xml
               text/javascript application/x-javascript application/xml+rss
               application/json application/javascript image/svg+xml;

    # 引入子配置文件
    include /etc/nginx/conf.d/*.conf;
}
```

---

## 本地开发环境简化版

仅用 Nginx 做 API 转发，前端由 Vite 开发服务器处理：

```nginx
# 开发环境配置 mitedtsm-dev.conf
server {
    listen 80;
    server_name dev.local.com;

    location /admin-api/ {
        proxy_pass http://localhost:8080/admin-api/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    }

    location /app-api/ {
        proxy_pass http://localhost:8080/app-api/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    }
}
```

---

## 重载配置验证

每次修改后执行：

```bash
# 检查配置
nginx -t

# 重载配置（不中断服务）
nginx -s reload

# 重启服务（如必须）
sudo systemctl restart nginx
```

---

## 检查日志

```bash
# 实时查看访问日志
tail -f /var/log/nginx/mitedtsm-access.log

# 查看错误日志
tail -f /var/log/nginx/mitedtsm-error.log
```

---

## 功能总结

| 功能 | 说明 |
|------|------|
| HTTP → HTTPS 跳转 | 强制 HTTPS |
| 静态资源缓存 | JS/CSS/图片 长效缓存 |
| index.html 不缓存 | 保证每次拿到最新包 |
| Admin/App API 反向代理 | 透传后端 |
| WebSocket 支持 | 实时通信 |
| Actuator 内网限制 | 安全加固 |
| 禁止访问敏感文件 | .git/.env 等安全防护 |
| Gzip 压缩 | 减少带宽占用 |
| 连接复用 | 减少握手开销 |
