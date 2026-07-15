#!/bin/bash
# MySQL 初始化脚本
# 按指定顺序执行 database 目录下的所有 SQL 文件

set -e

mysql_utf8() {
    command mysql --default-character-set=utf8mb4 "$@"
}

echo "Starting MySQL initialization..."

# 等待 MySQL 完全启动
until mysql_utf8 -uroot -p"${MYSQL_ROOT_PASSWORD}" -e "SELECT 1"; do
    echo "Waiting for MySQL to be ready..."
    sleep 2
done

echo "MySQL is ready, executing SQL files..."

# SQL 文件目录
SQL_BASE_DIR="/podman-init-sql/base"
SQL_NEW_DIR="/podman-init-sql/new"

# 1. 先执行 quartz.sql
echo "Executing: quartz.sql"
mysql_utf8 -uroot -p"${MYSQL_ROOT_PASSWORD}" "${MYSQL_DATABASE}" < "${SQL_BASE_DIR}/quartz.sql"

# 2. 再执行 ruoyi-vue-pro.sql
echo "Executing: ruoyi-vue-pro.sql"
mysql_utf8 -uroot -p"${MYSQL_ROOT_PASSWORD}" "${MYSQL_DATABASE}" < "${SQL_BASE_DIR}/ruoyi-vue-pro.sql"

# 3. 执行 base 目录下其余 SQL 文件（按字母顺序，排除已执行的）
for sql_file in "${SQL_BASE_DIR}"/*.sql; do
    filename=$(basename "$sql_file")
    if [[ "$filename" != "quartz.sql" && "$filename" != "ruoyi-vue-pro.sql" ]]; then
        echo "Executing: ${filename}"
        mysql_utf8 -uroot -p"${MYSQL_ROOT_PASSWORD}" "${MYSQL_DATABASE}" < "$sql_file"
    fi
done

# 4. 执行 new 目录下的 SQL 文件（按依赖顺序）
if [ -d "$SQL_NEW_DIR" ]; then
    # 4.1 先执行 new-i18n.sql（创建基础表 system_menu_i18n）
    if [ -f "${SQL_NEW_DIR}/new-i18n.sql" ]; then
        echo "Executing: new-i18n.sql"
        mysql_utf8 -uroot -p"${MYSQL_ROOT_PASSWORD}" "${MYSQL_DATABASE}" < "${SQL_NEW_DIR}/new-i18n.sql"
    fi

    # 4.2 再执行 new-mall-i18n.sql（创建 promotion_diy_menu_i18n 表，修改 system_tenant 表添加 currency_code）
    if [ -f "${SQL_NEW_DIR}/new-mall-i18n.sql" ]; then
        echo "Executing: new-mall-i18n.sql"
        mysql_utf8 -uroot -p"${MYSQL_ROOT_PASSWORD}" "${MYSQL_DATABASE}" < "${SQL_NEW_DIR}/new-mall-i18n.sql"
    fi

    # 4.3 执行 new-i18n-ar.sql（插入多语言数据，依赖 system_menu_i18n 表存在）
    if [ -f "${SQL_NEW_DIR}/new-i18n-ar.sql" ]; then
        echo "Executing: new-i18n-ar.sql"
        mysql_utf8 -uroot -p"${MYSQL_ROOT_PASSWORD}" "${MYSQL_DATABASE}" < "${SQL_NEW_DIR}/new-i18n-ar.sql"
    fi

    # 4.4 执行 new-product-category-i18n.sql
    if [ -f "${SQL_NEW_DIR}/new-product-category-i18n.sql" ]; then
        echo "Executing: new-product-category-i18n.sql"
        mysql_utf8 -uroot -p"${MYSQL_ROOT_PASSWORD}" "${MYSQL_DATABASE}" < "${SQL_NEW_DIR}/new-product-category-i18n.sql"
    fi

    # 4.5 执行 new-large-file-upload.sql
    if [ -f "${SQL_NEW_DIR}/new-large-file-upload.sql" ]; then
        echo "Executing: new-large-file-upload.sql"
        mysql_utf8 -uroot -p"${MYSQL_ROOT_PASSWORD}" "${MYSQL_DATABASE}" < "${SQL_NEW_DIR}/new-large-file-upload.sql"
    fi

    # 4.6 执行 CRM 客户公海归属历史表
    if [ -f "${SQL_NEW_DIR}/new-crm-customer-owner-record.sql" ]; then
        echo "Executing: new-crm-customer-owner-record.sql"
        mysql_utf8 -uroot -p"${MYSQL_ROOT_PASSWORD}" "${MYSQL_DATABASE}" < "${SQL_NEW_DIR}/new-crm-customer-owner-record.sql"
    fi

    # 4.7 执行 CRM 首联系人字段和历史数据初始化
    if [ -f "${SQL_NEW_DIR}/new-crm-contact-primary.sql" ]; then
        echo "Executing: new-crm-contact-primary.sql"
        mysql_utf8 -uroot -p"${MYSQL_ROOT_PASSWORD}" "${MYSQL_DATABASE}" < "${SQL_NEW_DIR}/new-crm-contact-primary.sql"
    fi

    # 4.8 执行 CRM 有效联系人手机号唯一约束
    if [ -f "${SQL_NEW_DIR}/new-crm-contact-mobile-unique.sql" ]; then
        echo "Executing: new-crm-contact-mobile-unique.sql"
        mysql_utf8 -uroot -p"${MYSQL_ROOT_PASSWORD}" "${MYSQL_DATABASE}" < "${SQL_NEW_DIR}/new-crm-contact-mobile-unique.sql"
    fi

    # 4.9 执行 CRM 客户上下级关系字段和索引
    if [ -f "${SQL_NEW_DIR}/new-crm-customer-hierarchy.sql" ]; then
        echo "Executing: new-crm-customer-hierarchy.sql"
        mysql_utf8 -uroot -p"${MYSQL_ROOT_PASSWORD}" "${MYSQL_DATABASE}" < "${SQL_NEW_DIR}/new-crm-customer-hierarchy.sql"
    fi

    # 4.10 执行 CRM 业绩目标表和维护权限
    if [ -f "${SQL_NEW_DIR}/new-crm-performance-target.sql" ]; then
        echo "Executing: new-crm-performance-target.sql"
        mysql_utf8 -uroot -p"${MYSQL_ROOT_PASSWORD}" "${MYSQL_DATABASE}" < "${SQL_NEW_DIR}/new-crm-performance-target.sql"
    fi

    # 4.11 执行 CRM 商机赢单转合同幂等来源字段和唯一键
    if [ -f "${SQL_NEW_DIR}/new-crm-business-contract-conversion.sql" ]; then
        echo "Executing: new-crm-business-contract-conversion.sql"
        mysql_utf8 -uroot -p"${MYSQL_ROOT_PASSWORD}" "${MYSQL_DATABASE}" < "${SQL_NEW_DIR}/new-crm-business-contract-conversion.sql"
    fi

    # 4.12 执行 CRM 客服工单最小闭环
    if [ -f "${SQL_NEW_DIR}/new-crm-work-order.sql" ]; then
        echo "Executing: new-crm-work-order.sql"
        mysql_utf8 -uroot -p"${MYSQL_ROOT_PASSWORD}" "${MYSQL_DATABASE}" < "${SQL_NEW_DIR}/new-crm-work-order.sql"
    fi

    # 4.13 执行 CRM 客服工单统计菜单与权限
    if [ -f "${SQL_NEW_DIR}/new-crm-work-order-statistics.sql" ]; then
        echo "Executing: new-crm-work-order-statistics.sql"
        mysql_utf8 -uroot -p"${MYSQL_ROOT_PASSWORD}" "${MYSQL_DATABASE}" < "${SQL_NEW_DIR}/new-crm-work-order-statistics.sql"
    fi

    # 4.14 执行 CRM 合同产品不可变文本快照及历史回填
    if [ -f "${SQL_NEW_DIR}/new-crm-contract-product-snapshot.sql" ]; then
        echo "Executing: new-crm-contract-product-snapshot.sql"
        mysql_utf8 -uroot -p"${MYSQL_ROOT_PASSWORD}" "${MYSQL_DATABASE}" < "${SQL_NEW_DIR}/new-crm-contract-product-snapshot.sql"
    fi

    # 4.15 执行 CRM 发票生命周期、字典、菜单与权限
    if [ -f "${SQL_NEW_DIR}/new-crm-invoice.sql" ]; then
        echo "Executing: new-crm-invoice.sql"
        mysql_utf8 -uroot -p"${MYSQL_ROOT_PASSWORD}" "${MYSQL_DATABASE}" < "${SQL_NEW_DIR}/new-crm-invoice.sql"
    fi

    # 4.16 执行 CRM 合同签署、附件和不可变版本轨迹
    if [ -f "${SQL_NEW_DIR}/new-crm-contract-lifecycle.sql" ]; then
        echo "Executing: new-crm-contract-lifecycle.sql"
        mysql_utf8 -uroot -p"${MYSQL_ROOT_PASSWORD}" "${MYSQL_DATABASE}" < "${SQL_NEW_DIR}/new-crm-contract-lifecycle.sql"
    fi

    # 4.17 执行 CRM 客户四态生命周期和变更历史
    if [ -f "${SQL_NEW_DIR}/new-crm-customer-lifecycle.sql" ]; then
        echo "Executing: new-crm-customer-lifecycle.sql"
        mysql_utf8 -uroot -p"${MYSQL_ROOT_PASSWORD}" "${MYSQL_DATABASE}" < "${SQL_NEW_DIR}/new-crm-customer-lifecycle.sql"
    fi

    # 4.18 执行 CRM 报销、费用分类、明细和审批轨迹
    if [ -f "${SQL_NEW_DIR}/new-crm-reimbursement.sql" ]; then
        echo "Executing: new-crm-reimbursement.sql"
        mysql_utf8 -uroot -p"${MYSQL_ROOT_PASSWORD}" "${MYSQL_DATABASE}" < "${SQL_NEW_DIR}/new-crm-reimbursement.sql"
    fi
fi

echo "MySQL initialization completed successfully!"
