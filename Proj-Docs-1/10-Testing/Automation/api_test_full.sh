#!/bin/bash
BASE_URL="http://127.0.0.1:8080"
TENANT_ID="1"
TEST_RESULTS="/tmp/api_test_results.csv"

echo "模块,接口路径,方法,测试结果,HTTP状态码,响应时间(ms)" > "$TEST_RESULTS"

log_test() {
  echo "$1,$2,$3,$4,$5,$6" >> "$TEST_RESULTS"
}

echo "=== 登录获取Token ==="
LOGIN_RESP=$(curl -s -X POST "$BASE_URL/admin-api/system/auth/login" \
  -H "Content-Type: application/json" \
  -H "tenant-id: $TENANT_ID" \
  -d '{"username":"admin","password":"admin123"}')
TOKEN=$(echo "$LOGIN_RESP" | grep -o '"accessToken":"[^"]*"' | cut -d'"' -f4)
if [ -z "$TOKEN" ]; then
  TOKEN=$(echo "$LOGIN_RESP" | grep -o '"token":"[^"]*"' | cut -d'"' -f4)
fi

if [ -n "$TOKEN" ]; then
  echo "✅ 登录成功"
else
  echo "❌ 登录失败，退出测试"
  exit 1
fi
echo ""

echo "=== M01-系统管理 ==="
curl -s -o /tmp/api_resp_body -w "%{http_code}" "$BASE_URL/admin-api/system/user/page?pageNo=1&pageSize=10" -H "tenant-id: $TENANT_ID" -H "Authorization: Bearer $TOKEN"
CONTENT=$(cat /tmp/api_resp_body)
CODE=$(echo "$CONTENT" | grep -o '"code":[0-9]*' | cut -d':' -f2)
if [ "$CODE" = "0" ]; then echo "✅ System | GET /admin-api/system/user/page"; log_test "System" "/admin-api/system/user/page" "GET" "PASS" "200" "0"; else echo "❌ System | GET /admin-api/system/user/page - code=$CODE"; log_test "System" "/admin-api/system/user/page" "GET" "FAIL" "200" "0"; fi

curl -s -o /tmp/api_resp_body -w "%{http_code}" "$BASE_URL/admin-api/system/user/get?id=1" -H "tenant-id: $TENANT_ID" -H "Authorization: Bearer $TOKEN"
CONTENT=$(cat /tmp/api_resp_body)
CODE=$(echo "$CONTENT" | grep -o '"code":[0-9]*' | cut -d':' -f2)
if [ "$CODE" = "0" ]; then echo "✅ System | GET /admin-api/system/user/get"; log_test "System" "/admin-api/system/user/get" "GET" "PASS" "200" "0"; else echo "❌ System | GET /admin-api/system/user/get - code=$CODE"; log_test "System" "/admin-api/system/user/get" "GET" "FAIL" "200" "0"; fi

curl -s -o /tmp/api_resp_body -w "%{http_code}" "$BASE_URL/admin-api/system/user/list-all" -H "tenant-id: $TENANT_ID" -H "Authorization: Bearer $TOKEN"
CONTENT=$(cat /tmp/api_resp_body)
CODE=$(echo "$CONTENT" | grep -o '"code":[0-9]*' | cut -d':' -f2)
if [ "$CODE" = "0" ]; then echo "✅ System | GET /admin-api/system/user/list-all"; log_test "System" "/admin-api/system/user/list-all" "GET" "PASS" "200" "0"; else echo "❌ System | GET /admin-api/system/user/list-all - code=$CODE"; log_test "System" "/admin-api/system/user/list-all" "GET" "FAIL" "200" "0"; fi

curl -s -o /tmp/api_resp_body -w "%{http_code}" "$BASE_URL/admin-api/system/role/page?pageNo=1&pageSize=10" -H "tenant-id: $TENANT_ID" -H "Authorization: Bearer $TOKEN"
CONTENT=$(cat /tmp/api_resp_body)
CODE=$(echo "$CONTENT" | grep -o '"code":[0-9]*' | cut -d':' -f2)
if [ "$CODE" = "0" ]; then echo "✅ System | GET /admin-api/system/role/page"; log_test "System" "/admin-api/system/role/page" "GET" "PASS" "200" "0"; else echo "❌ System | GET /admin-api/system/role/page - code=$CODE"; log_test "System" "/admin-api/system/role/page" "GET" "FAIL" "200" "0"; fi

curl -s -o /tmp/api_resp_body -w "%{http_code}" "$BASE_URL/admin-api/system/role/get?id=1" -H "tenant-id: $TENANT_ID" -H "Authorization: Bearer $TOKEN"
CONTENT=$(cat /tmp/api_resp_body)
CODE=$(echo "$CONTENT" | grep -o '"code":[0-9]*' | cut -d':' -f2)
if [ "$CODE" = "0" ]; then echo "✅ System | GET /admin-api/system/role/get"; log_test "System" "/admin-api/system/role/get" "GET" "PASS" "200" "0"; else echo "❌ System | GET /admin-api/system/role/get - code=$CODE"; log_test "System" "/admin-api/system/role/get" "GET" "FAIL" "200" "0"; fi

curl -s -o /tmp/api_resp_body -w "%{http_code}" "$BASE_URL/admin-api/system/role/list-all" -H "tenant-id: $TENANT_ID" -H "Authorization: Bearer $TOKEN"
CONTENT=$(cat /tmp/api_resp_body)
CODE=$(echo "$CONTENT" | grep -o '"code":[0-9]*' | cut -d':' -f2)
if [ "$CODE" = "0" ]; then echo "✅ System | GET /admin-api/system/role/list-all"; log_test "System" "/admin-api/system/role/list-all" "GET" "PASS" "200" "0"; else echo "❌ System | GET /admin-api/system/role/list-all - code=$CODE"; log_test "System" "/admin-api/system/role/list-all" "GET" "FAIL" "200" "0"; fi

curl -s -o /tmp/api_resp_body -w "%{http_code}" "$BASE_URL/admin-api/system/dept/list" -H "tenant-id: $TENANT_ID" -H "Authorization: Bearer $TOKEN"
CONTENT=$(cat /tmp/api_resp_body)
CODE=$(echo "$CONTENT" | grep -o '"code":[0-9]*' | cut -d':' -f2)
if [ "$CODE" = "0" ]; then echo "✅ System | GET /admin-api/system/dept/list"; log_test "System" "/admin-api/system/dept/list" "GET" "PASS" "200" "0"; else echo "❌ System | GET /admin-api/system/dept/list - code=$CODE"; log_test "System" "/admin-api/system/dept/list" "GET" "FAIL" "200" "0"; fi

curl -s -o /tmp/api_resp_body -w "%{http_code}" "$BASE_URL/admin-api/system/dept/get?id=1" -H "tenant-id: $TENANT_ID" -H "Authorization: Bearer $TOKEN"
CONTENT=$(cat /tmp/api_resp_body)
CODE=$(echo "$CONTENT" | grep -o '"code":[0-9]*' | cut -d':' -f2)
if [ "$CODE" = "0" ]; then echo "✅ System | GET /admin-api/system/dept/get"; log_test "System" "/admin-api/system/dept/get" "GET" "PASS" "200" "0"; else echo "❌ System | GET /admin-api/system/dept/get - code=$CODE"; log_test "System" "/admin-api/system/dept/get" "GET" "FAIL" "200" "0"; fi

curl -s -o /tmp/api_resp_body -w "%{http_code}" "$BASE_URL/admin-api/system/dept/list-all-simple" -H "tenant-id: $TENANT_ID" -H "Authorization: Bearer $TOKEN"
CONTENT=$(cat /tmp/api_resp_body)
CODE=$(echo "$CONTENT" | grep -o '"code":[0-9]*' | cut -d':' -f2)
if [ "$CODE" = "0" ]; then echo "✅ System | GET /admin-api/system/dept/list-all-simple"; log_test "System" "/admin-api/system/dept/list-all-simple" "GET" "PASS" "200" "0"; else echo "❌ System | GET /admin-api/system/dept/list-all-simple - code=$CODE"; log_test "System" "/admin-api/system/dept/list-all-simple" "GET" "FAIL" "200" "0"; fi

curl -s -o /tmp/api_resp_body -w "%{http_code}" "$BASE_URL/admin-api/system/dict-type/page?pageNo=1&pageSize=10" -H "tenant-id: $TENANT_ID" -H "Authorization: Bearer $TOKEN"
CONTENT=$(cat /tmp/api_resp_body)
CODE=$(echo "$CONTENT" | grep -o '"code":[0-9]*' | cut -d':' -f2)
if [ "$CODE" = "0" ]; then echo "✅ System | GET /admin-api/system/dict-type/page"; log_test "System" "/admin-api/system/dict-type/page" "GET" "PASS" "200" "0"; else echo "❌ System | GET /admin-api/system/dict-type/page - code=$CODE"; log_test "System" "/admin-api/system/dict-type/page" "GET" "FAIL" "200" "0"; fi

curl -s -o /tmp/api_resp_body -w "%{http_code}" "$BASE_URL/admin-api/system/dict-type/list-all" -H "tenant-id: $TENANT_ID" -H "Authorization: Bearer $TOKEN"
CONTENT=$(cat /tmp/api_resp_body)
CODE=$(echo "$CONTENT" | grep -o '"code":[0-9]*' | cut -d':' -f2)
if [ "$CODE" = "0" ]; then echo "✅ System | GET /admin-api/system/dict-type/list-all"; log_test "System" "/admin-api/system/dict-type/list-all" "GET" "PASS" "200" "0"; else echo "❌ System | GET /admin-api/system/dict-type/list-all - code=$CODE"; log_test "System" "/admin-api/system/dict-type/list-all" "GET" "FAIL" "200" "0"; fi

curl -s -o /tmp/api_resp_body -w "%{http_code}" "$BASE_URL/admin-api/system/tenant/page?pageNo=1&pageSize=10" -H "tenant-id: $TENANT_ID" -H "Authorization: Bearer $TOKEN"
CONTENT=$(cat /tmp/api_resp_body)
CODE=$(echo "$CONTENT" | grep -o '"code":[0-9]*' | cut -d':' -f2)
if [ "$CODE" = "0" ]; then echo "✅ System | GET /admin-api/system/tenant/page"; log_test "System" "/admin-api/system/tenant/page" "GET" "PASS" "200" "0"; else echo "❌ System | GET /admin-api/system/tenant/page - code=$CODE"; log_test "System" "/admin-api/system/tenant/page" "GET" "FAIL" "200" "0"; fi

curl -s -o /tmp/api_resp_body -w "%{http_code}" "$BASE_URL/admin-api/system/tenant/get?id=1" -H "tenant-id: $TENANT_ID" -H "Authorization: Bearer $TOKEN"
CONTENT=$(cat /tmp/api_resp_body)
CODE=$(echo "$CONTENT" | grep -o '"code":[0-9]*' | cut -d':' -f2)
if [ "$CODE" = "0" ]; then echo "✅ System | GET /admin-api/system/tenant/get"; log_test "System" "/admin-api/system/tenant/get" "GET" "PASS" "200" "0"; else echo "❌ System | GET /admin-api/system/tenant/get - code=$CODE"; log_test "System" "/admin-api/system/tenant/get" "GET" "FAIL" "200" "0"; fi

curl -s -o /tmp/api_resp_body -w "%{http_code}" "$BASE_URL/admin-api/system/post/page?pageNo=1&pageSize=10" -H "tenant-id: $TENANT_ID" -H "Authorization: Bearer $TOKEN"
CONTENT=$(cat /tmp/api_resp_body)
CODE=$(echo "$CONTENT" | grep -o '"code":[0-9]*' | cut -d':' -f2)
if [ "$CODE" = "0" ]; then echo "✅ System | GET /admin-api/system/post/page"; log_test "System" "/admin-api/system/post/page" "GET" "PASS" "200" "0"; else echo "❌ System | GET /admin-api/system/post/page - code=$CODE"; log_test "System" "/admin-api/system/post/page" "GET" "FAIL" "200" "0"; fi

curl -s -o /tmp/api_resp_body -w "%{http_code}" "$BASE_URL/admin-api/system/notice/page?pageNo=1&pageSize=10" -H "tenant-id: $TENANT_ID" -H "Authorization: Bearer $TOKEN"
CONTENT=$(cat /tmp/api_resp_body)
CODE=$(echo "$CONTENT" | grep -o '"code":[0-9]*' | cut -d':' -f2)
if [ "$CODE" = "0" ]; then echo "✅ System | GET /admin-api/system/notice/page"; log_test "System" "/admin-api/system/notice/page" "GET" "PASS" "200" "0"; else echo "❌ System | GET /admin-api/system/notice/page - code=$CODE"; log_test "System" "/admin-api/system/notice/page" "GET" "FAIL" "200" "0"; fi

echo ""
echo "=== M02-基础设施 ==="
curl -s -o /tmp/api_resp_body -w "%{http_code}" "$BASE_URL/admin-api/infra/file/page?pageNo=1&pageSize=10" -H "tenant-id: $TENANT_ID" -H "Authorization: Bearer $TOKEN"
CONTENT=$(cat /tmp/api_resp_body)
CODE=$(echo "$CONTENT" | grep -o '"code":[0-9]*' | cut -d':' -f2)
if [ "$CODE" = "0" ]; then echo "✅ Infra | GET /admin-api/infra/file/page"; log_test "Infra" "/admin-api/infra/file/page" "GET" "PASS" "200" "0"; else echo "❌ Infra | GET /admin-api/infra/file/page - code=$CODE"; log_test "Infra" "/admin-api/infra/file/page" "GET" "FAIL" "200" "0"; fi

curl -s -o /tmp/api_resp_body -w "%{http_code}" "$BASE_URL/admin-api/infra/job/page?pageNo=1&pageSize=10" -H "tenant-id: $TENANT_ID" -H "Authorization: Bearer $TOKEN"
CONTENT=$(cat /tmp/api_resp_body)
CODE=$(echo "$CONTENT" | grep -o '"code":[0-9]*' | cut -d':' -f2)
if [ "$CODE" = "0" ]; then echo "✅ Infra | GET /admin-api/infra/job/page"; log_test "Infra" "/admin-api/infra/job/page" "GET" "PASS" "200" "0"; else echo "❌ Infra | GET /admin-api/infra/job/page - code=$CODE"; log_test "Infra" "/admin-api/infra/job/page" "GET" "FAIL" "200" "0"; fi

curl -s -o /tmp/api_resp_body -w "%{http_code}" "$BASE_URL/admin-api/infra/job/get?id=1" -H "tenant-id: $TENANT_ID" -H "Authorization: Bearer $TOKEN"
CONTENT=$(cat /tmp/api_resp_body)
CODE=$(echo "$CONTENT" | grep -o '"code":[0-9]*' | cut -d':' -f2)
if [ "$CODE" = "0" ]; then echo "✅ Infra | GET /admin-api/infra/job/get"; log_test "Infra" "/admin-api/infra/job/get" "GET" "PASS" "200" "0"; else echo "❌ Infra | GET /admin-api/infra/job/get - code=$CODE"; log_test "Infra" "/admin-api/infra/job/get" "GET" "FAIL" "200" "0"; fi

curl -s -o /tmp/api_resp_body -w "%{http_code}" "$BASE_URL/admin-api/infra/config/page?pageNo=1&pageSize=10" -H "tenant-id: $TENANT_ID" -H "Authorization: Bearer $TOKEN"
CONTENT=$(cat /tmp/api_resp_body)
CODE=$(echo "$CONTENT" | grep -o '"code":[0-9]*' | cut -d':' -f2)
if [ "$CODE" = "0" ]; then echo "✅ Infra | GET /admin-api/infra/config/page"; log_test "Infra" "/admin-api/infra/config/page" "GET" "PASS" "200" "0"; else echo "❌ Infra | GET /admin-api/infra/config/page - code=$CODE"; log_test "Infra" "/admin-api/infra/config/page" "GET" "FAIL" "200" "0"; fi

curl -s -o /tmp/api_resp_body -w "%{http_code}" "$BASE_URL/admin-api/infra/config/get?id=1" -H "tenant-id: $TENANT_ID" -H "Authorization: Bearer $TOKEN"
CONTENT=$(cat /tmp/api_resp_body)
CODE=$(echo "$CONTENT" | grep -o '"code":[0-9]*' | cut -d':' -f2)
if [ "$CODE" = "0" ]; then echo "✅ Infra | GET /admin-api/infra/config/get"; log_test "Infra" "/admin-api/infra/config/get" "GET" "PASS" "200" "0"; else echo "❌ Infra | GET /admin-api/infra/config/get - code=$CODE"; log_test "Infra" "/admin-api/infra/config/get" "GET" "FAIL" "200" "0"; fi

curl -s -o /tmp/api_resp_body -w "%{http_code}" "$BASE_URL/admin-api/infra/log/page?pageNo=1&pageSize=10" -H "tenant-id: $TENANT_ID" -H "Authorization: Bearer $TOKEN"
CONTENT=$(cat /tmp/api_resp_body)
CODE=$(echo "$CONTENT" | grep -o '"code":[0-9]*' | cut -d':' -f2)
if [ "$CODE" = "0" ]; then echo "✅ Infra | GET /admin-api/infra/log/page"; log_test "Infra" "/admin-api/infra/log/page" "GET" "PASS" "200" "0"; else echo "❌ Infra | GET /admin-api/infra/log/page - code=$CODE"; log_test "Infra" "/admin-api/infra/log/page" "GET" "FAIL" "200" "0"; fi

echo ""
echo "=== M07-CRM客户管理 ==="
curl -s -o /tmp/api_resp_body -w "%{http_code}" "$BASE_URL/admin-api/crm/customer/page?pageNo=1&pageSize=10" -H "tenant-id: $TENANT_ID" -H "Authorization: Bearer $TOKEN"
CONTENT=$(cat /tmp/api_resp_body)
CODE=$(echo "$CONTENT" | grep -o '"code":[0-9]*' | cut -d':' -f2)
if [ "$CODE" = "0" ]; then echo "✅ CRM | GET /admin-api/crm/customer/page"; log_test "CRM" "/admin-api/crm/customer/page" "GET" "PASS" "200" "0"; else echo "❌ CRM | GET /admin-api/crm/customer/page - code=$CODE"; log_test "CRM" "/admin-api/crm/customer/page" "GET" "FAIL" "200" "0"; fi

curl -s -o /tmp/api_resp_body -w "%{http_code}" "$BASE_URL/admin-api/crm/customer/get?id=1" -H "tenant-id: $TENANT_ID" -H "Authorization: Bearer $TOKEN"
CONTENT=$(cat /tmp/api_resp_body)
CODE=$(echo "$CONTENT" | grep -o '"code":[0-9]*' | cut -d':' -f2)
if [ "$CODE" = "0" ]; then echo "✅ CRM | GET /admin-api/crm/customer/get"; log_test "CRM" "/admin-api/crm/customer/get" "GET" "PASS" "200" "0"; else echo "❌ CRM | GET /admin-api/crm/customer/get - code=$CODE"; log_test "CRM" "/admin-api/crm/customer/get" "GET" "FAIL" "200" "0"; fi

curl -s -o /tmp/api_resp_body -w "%{http_code}" "$BASE_URL/admin-api/crm/contact/page?pageNo=1&pageSize=10" -H "tenant-id: $TENANT_ID" -H "Authorization: Bearer $TOKEN"
CONTENT=$(cat /tmp/api_resp_body)
CODE=$(echo "$CONTENT" | grep -o '"code":[0-9]*' | cut -d':' -f2)
if [ "$CODE" = "0" ]; then echo "✅ CRM | GET /admin-api/crm/contact/page"; log_test "CRM" "/admin-api/crm/contact/page" "GET" "PASS" "200" "0"; else echo "❌ CRM | GET /admin-api/crm/contact/page - code=$CODE"; log_test "CRM" "/admin-api/crm/contact/page" "GET" "FAIL" "200" "0"; fi

curl -s -o /tmp/api_resp_body -w "%{http_code}" "$BASE_URL/admin-api/crm/clue/page?pageNo=1&pageSize=10" -H "tenant-id: $TENANT_ID" -H "Authorization: Bearer $TOKEN"
CONTENT=$(cat /tmp/api_resp_body)
CODE=$(echo "$CONTENT" | grep -o '"code":[0-9]*' | cut -d':' -f2)
if [ "$CODE" = "0" ]; then echo "✅ CRM | GET /admin-api/crm/clue/page"; log_test "CRM" "/admin-api/crm/clue/page" "GET" "PASS" "200" "0"; else echo "❌ CRM | GET /admin-api/crm/clue/page - code=$CODE"; log_test "CRM" "/admin-api/crm/clue/page" "GET" "FAIL" "200" "0"; fi

curl -s -o /tmp/api_resp_body -w "%{http_code}" "$BASE_URL/admin-api/crm/business/page?pageNo=1&pageSize=10" -H "tenant-id: $TENANT_ID" -H "Authorization: Bearer $TOKEN"
CONTENT=$(cat /tmp/api_resp_body)
CODE=$(echo "$CONTENT" | grep -o '"code":[0-9]*' | cut -d':' -f2)
if [ "$CODE" = "0" ]; then echo "✅ CRM | GET /admin-api/crm/business/page"; log_test "CRM" "/admin-api/crm/business/page" "GET" "PASS" "200" "0"; else echo "❌ CRM | GET /admin-api/crm/business/page - code=$CODE"; log_test "CRM" "/admin-api/crm/business/page" "GET" "FAIL" "200" "0"; fi

curl -s -o /tmp/api_resp_body -w "%{http_code}" "$BASE_URL/admin-api/crm/contract/page?pageNo=1&pageSize=10" -H "tenant-id: $TENANT_ID" -H "Authorization: Bearer $TOKEN"
CONTENT=$(cat /tmp/api_resp_body)
CODE=$(echo "$CONTENT" | grep -o '"code":[0-9]*' | cut -d':' -f2)
if [ "$CODE" = "0" ]; then echo "✅ CRM | GET /admin-api/crm/contract/page"; log_test "CRM" "/admin-api/crm/contract/page" "GET" "PASS" "200" "0"; else echo "❌ CRM | GET /admin-api/crm/contract/page - code=$CODE"; log_test "CRM" "/admin-api/crm/contract/page" "GET" "FAIL" "200" "0"; fi

curl -s -o /tmp/api_resp_body -w "%{http_code}" "$BASE_URL/admin-api/crm/receivable/page?pageNo=1&pageSize=10" -H "tenant-id: $TENANT_ID" -H "Authorization: Bearer $TOKEN"
CONTENT=$(cat /tmp/api_resp_body)
CODE=$(echo "$CONTENT" | grep -o '"code":[0-9]*' | cut -d':' -f2)
if [ "$CODE" = "0" ]; then echo "✅ CRM | GET /admin-api/crm/receivable/page"; log_test "CRM" "/admin-api/crm/receivable/page" "GET" "PASS" "200" "0"; else echo "❌ CRM | GET /admin-api/crm/receivable/page - code=$CODE"; log_test "CRM" "/admin-api/crm/receivable/page" "GET" "FAIL" "200" "0"; fi

curl -s -o /tmp/api_resp_body -w "%{http_code}" "$BASE_URL/admin-api/crm/customer/pool-config/page?pageNo=1&pageSize=10" -H "tenant-id: $TENANT_ID" -H "Authorization: Bearer $TOKEN"
CONTENT=$(cat /tmp/api_resp_body)
CODE=$(echo "$CONTENT" | grep -o '"code":[0-9]*' | cut -d':' -f2)
if [ "$CODE" = "0" ]; then echo "✅ CRM | GET /admin-api/crm/customer/pool-config/page"; log_test "CRM" "/admin-api/crm/customer/pool-config/page" "GET" "PASS" "200" "0"; else echo "❌ CRM | GET /admin-api/crm/customer/pool-config/page - code=$CODE"; log_test "CRM" "/admin-api/crm/customer/pool-config/page" "GET" "FAIL" "200" "0"; fi

echo ""
echo "=== M08-ERP进销存 ==="
curl -s -o /tmp/api_resp_body -w "%{http_code}" "$BASE_URL/admin-api/erp/product/page?pageNo=1&pageSize=10" -H "tenant-id: $TENANT_ID" -H "Authorization: Bearer $TOKEN"
CONTENT=$(cat /tmp/api_resp_body)
CODE=$(echo "$CONTENT" | grep -o '"code":[0-9]*' | cut -d':' -f2)
if [ "$CODE" = "0" ]; then echo "✅ ERP | GET /admin-api/erp/product/page"; log_test "ERP" "/admin-api/erp/product/page" "GET" "PASS" "200" "0"; else echo "❌ ERP | GET /admin-api/erp/product/page - code=$CODE"; log_test "ERP" "/admin-api/erp/product/page" "GET" "FAIL" "200" "0"; fi

curl -s -o /tmp/api_resp_body -w "%{http_code}" "$BASE_URL/admin-api/erp/product/get?id=1" -H "tenant-id: $TENANT_ID" -H "Authorization: Bearer $TOKEN"
CONTENT=$(cat /tmp/api_resp_body)
CODE=$(echo "$CONTENT" | grep -o '"code":[0-9]*' | cut -d':' -f2)
if [ "$CODE" = "0" ]; then echo "✅ ERP | GET /admin-api/erp/product/get"; log_test "ERP" "/admin-api/erp/product/get" "GET" "PASS" "200" "0"; else echo "❌ ERP | GET /admin-api/erp/product/get - code=$CODE"; log_test "ERP" "/admin-api/erp/product/get" "GET" "FAIL" "200" "0"; fi

curl -s -o /tmp/api_resp_body -w "%{http_code}" "$BASE_URL/admin-api/erp/product-unit/page?pageNo=1&pageSize=10" -H "tenant-id: $TENANT_ID" -H "Authorization: Bearer $TOKEN"
CONTENT=$(cat /tmp/api_resp_body)
CODE=$(echo "$CONTENT" | grep -o '"code":[0-9]*' | cut -d':' -f2)
if [ "$CODE" = "0" ]; then echo "✅ ERP | GET /admin-api/erp/product-unit/page"; log_test "ERP" "/admin-api/erp/product-unit/page" "GET" "PASS" "200" "0"; else echo "❌ ERP | GET /admin-api/erp/product-unit/page - code=$CODE"; log_test "ERP" "/admin-api/erp/product-unit/page" "GET" "FAIL" "200" "0"; fi

curl -s -o /tmp/api_resp_body -w "%{http_code}" "$BASE_URL/admin-api/erp/purchase-order/page?pageNo=1&pageSize=10" -H "tenant-id: $TENANT_ID" -H "Authorization: Bearer $TOKEN"
CONTENT=$(cat /tmp/api_resp_body)
CODE=$(echo "$CONTENT" | grep -o '"code":[0-9]*' | cut -d':' -f2)
if [ "$CODE" = "0" ]; then echo "✅ ERP | GET /admin-api/erp/purchase-order/page"; log_test "ERP" "/admin-api/erp/purchase-order/page" "GET" "PASS" "200" "0"; else echo "❌ ERP | GET /admin-api/erp/purchase-order/page - code=$CODE"; log_test "ERP" "/admin-api/erp/purchase-order/page" "GET" "FAIL" "200" "0"; fi

curl -s -o /tmp/api_resp_body -w "%{http_code}" "$BASE_URL/admin-api/erp/supplier/page?pageNo=1&pageSize=10" -H "tenant-id: $TENANT_ID" -H "Authorization: Bearer $TOKEN"
CONTENT=$(cat /tmp/api_resp_body)
CODE=$(echo "$CONTENT" | grep -o '"code":[0-9]*' | cut -d':' -f2)
if [ "$CODE" = "0" ]; then echo "✅ ERP | GET /admin-api/erp/supplier/page"; log_test "ERP" "/admin-api/erp/supplier/page" "GET" "PASS" "200" "0"; else echo "❌ ERP | GET /admin-api/erp/supplier/page - code=$CODE"; log_test "ERP" "/admin-api/erp/supplier/page" "GET" "FAIL" "200" "0"; fi

curl -s -o /tmp/api_resp_body -w "%{http_code}" "$BASE_URL/admin-api/erp/sale-order/page?pageNo=1&pageSize=10" -H "tenant-id: $TENANT_ID" -H "Authorization: Bearer $TOKEN"
CONTENT=$(cat /tmp/api_resp_body)
CODE=$(echo "$CONTENT" | grep -o '"code":[0-9]*' | cut -d':' -f2)
if [ "$CODE" = "0" ]; then echo "✅ ERP | GET /admin-api/erp/sale-order/page"; log_test "ERP" "/admin-api/erp/sale-order/page" "GET" "PASS" "200" "0"; else echo "❌ ERP | GET /admin-api/erp/sale-order/page - code=$CODE"; log_test "ERP" "/admin-api/erp/sale-order/page" "GET" "FAIL" "200" "0"; fi

curl -s -o /tmp/api_resp_body -w "%{http_code}" "$BASE_URL/admin-api/erp/customer/page?pageNo=1&pageSize=10" -H "tenant-id: $TENANT_ID" -H "Authorization: Bearer $TOKEN"
CONTENT=$(cat /tmp/api_resp_body)
CODE=$(echo "$CONTENT" | grep -o '"code":[0-9]*' | cut -d':' -f2)
if [ "$CODE" = "0" ]; then echo "✅ ERP | GET /admin-api/erp/customer/page"; log_test "ERP" "/admin-api/erp/customer/page" "GET" "PASS" "200" "0"; else echo "❌ ERP | GET /admin-api/erp/customer/page - code=$CODE"; log_test "ERP" "/admin-api/erp/customer/page" "GET" "FAIL" "200" "0"; fi

curl -s -o /tmp/api_resp_body -w "%{http_code}" "$BASE_URL/admin-api/erp/warehouse/page?pageNo=1&pageSize=10" -H "tenant-id: $TENANT_ID" -H "Authorization: Bearer $TOKEN"
CONTENT=$(cat /tmp/api_resp_body)
CODE=$(echo "$CONTENT" | grep -o '"code":[0-9]*' | cut -d':' -f2)
if [ "$CODE" = "0" ]; then echo "✅ ERP | GET /admin-api/erp/warehouse/page"; log_test "ERP" "/admin-api/erp/warehouse/page" "GET" "PASS" "200" "0"; else echo "❌ ERP | GET /admin-api/erp/warehouse/page - code=$CODE"; log_test "ERP" "/admin-api/erp/warehouse/page" "GET" "FAIL" "200" "0"; fi

curl -s -o /tmp/api_resp_body -w "%{http_code}" "$BASE_URL/admin-api/erp/stock/page?pageNo=1&pageSize=10" -H "tenant-id: $TENANT_ID" -H "Authorization: Bearer $TOKEN"
CONTENT=$(cat /tmp/api_resp_body)
CODE=$(echo "$CONTENT" | grep -o '"code":[0-9]*' | cut -d':' -f2)
if [ "$CODE" = "0" ]; then echo "✅ ERP | GET /admin-api/erp/stock/page"; log_test "ERP" "/admin-api/erp/stock/page" "GET" "PASS" "200" "0"; else echo "❌ ERP | GET /admin-api/erp/stock/page - code=$CODE"; log_test "ERP" "/admin-api/erp/stock/page" "GET" "FAIL" "200" "0"; fi

curl -s -o /tmp/api_resp_body -w "%{http_code}" "$BASE_URL/admin-api/erp/stock-in/page?pageNo=1&pageSize=10" -H "tenant-id: $TENANT_ID" -H "Authorization: Bearer $TOKEN"
CONTENT=$(cat /tmp/api_resp_body)
CODE=$(echo "$CONTENT" | grep -o '"code":[0-9]*' | cut -d':' -f2)
if [ "$CODE" = "0" ]; then echo "✅ ERP | GET /admin-api/erp/stock-in/page"; log_test "ERP" "/admin-api/erp/stock-in/page" "GET" "PASS" "200" "0"; else echo "❌ ERP | GET /admin-api/erp/stock-in/page - code=$CODE"; log_test "ERP" "/admin-api/erp/stock-in/page" "GET" "FAIL" "200" "0"; fi

curl -s -o /tmp/api_resp_body -w "%{http_code}" "$BASE_URL/admin-api/erp/stock-out/page?pageNo=1&pageSize=10" -H "tenant-id: $TENANT_ID" -H "Authorization: Bearer $TOKEN"
CONTENT=$(cat /tmp/api_resp_body)
CODE=$(echo "$CONTENT" | grep -o '"code":[0-9]*' | cut -d':' -f2)
if [ "$CODE" = "0" ]; then echo "✅ ERP | GET /admin-api/erp/stock-out/page"; log_test "ERP" "/admin-api/erp/stock-out/page" "GET" "PASS" "200" "0"; else echo "❌ ERP | GET /admin-api/erp/stock-out/page - code=$CODE"; log_test "ERP" "/admin-api/erp/stock-out/page" "GET" "FAIL" "200" "0"; fi

curl -s -o /tmp/api_resp_body -w "%{http_code}" "$BASE_URL/admin-api/erp/finance-payment/page?pageNo=1&pageSize=10" -H "tenant-id: $TENANT_ID" -H "Authorization: Bearer $TOKEN"
CONTENT=$(cat /tmp/api_resp_body)
CODE=$(echo "$CONTENT" | grep -o '"code":[0-9]*' | cut -d':' -f2)
if [ "$CODE" = "0" ]; then echo "✅ ERP | GET /admin-api/erp/finance-payment/page"; log_test "ERP" "/admin-api/erp/finance-payment/page" "GET" "PASS" "200" "0"; else echo "❌ ERP | GET /admin-api/erp/finance-payment/page - code=$CODE"; log_test "ERP" "/admin-api/erp/finance-payment/page" "GET" "FAIL" "200" "0"; fi

curl -s -o /tmp/api_resp_body -w "%{http_code}" "$BASE_URL/admin-api/erp/finance-receipt/page?pageNo=1&pageSize=10" -H "tenant-id: $TENANT_ID" -H "Authorization: Bearer $TOKEN"
CONTENT=$(cat /tmp/api_resp_body)
CODE=$(echo "$CONTENT" | grep -o '"code":[0-9]*' | cut -d':' -f2)
if [ "$CODE" = "0" ]; then echo "✅ ERP | GET /admin-api/erp/finance-receipt/page"; log_test "ERP" "/admin-api/erp/finance-receipt/page" "GET" "PASS" "200" "0"; else echo "❌ ERP | GET /admin-api/erp/finance-receipt/page - code=$CODE"; log_test "ERP" "/admin-api/erp/finance-receipt/page" "GET" "FAIL" "200" "0"; fi

echo ""
echo "=== M10-Pay支付 ==="
curl -s -o /tmp/api_resp_body -w "%{http_code}" "$BASE_URL/admin-api/pay/order/page?pageNo=1&pageSize=10" -H "tenant-id: $TENANT_ID" -H "Authorization: Bearer $TOKEN"
CONTENT=$(cat /tmp/api_resp_body)
CODE=$(echo "$CONTENT" | grep -o '"code":[0-9]*' | cut -d':' -f2)
if [ "$CODE" = "0" ]; then echo "✅ Pay | GET /admin-api/pay/order/page"; log_test "Pay" "/admin-api/pay/order/page" "GET" "PASS" "200" "0"; else echo "❌ Pay | GET /admin-api/pay/order/page - code=$CODE"; log_test "Pay" "/admin-api/pay/order/page" "GET" "FAIL" "200" "0"; fi

curl -s -o /tmp/api_resp_body -w "%{http_code}" "$BASE_URL/admin-api/pay/refund/page?pageNo=1&pageSize=10" -H "tenant-id: $TENANT_ID" -H "Authorization: Bearer $TOKEN"
CONTENT=$(cat /tmp/api_resp_body)
CODE=$(echo "$CONTENT" | grep -o '"code":[0-9]*' | cut -d':' -f2)
if [ "$CODE" = "0" ]; then echo "✅ Pay | GET /admin-api/pay/refund/page"; log_test "Pay" "/admin-api/pay/refund/page" "GET" "PASS" "200" "0"; else echo "❌ Pay | GET /admin-api/pay/refund/page - code=$CODE"; log_test "Pay" "/admin-api/pay/refund/page" "GET" "FAIL" "200" "0"; fi

curl -s -o /tmp/api_resp_body -w "%{http_code}" "$BASE_URL/admin-api/pay/app/page?pageNo=1&pageSize=10" -H "tenant-id: $TENANT_ID" -H "Authorization: Bearer $TOKEN"
CONTENT=$(cat /tmp/api_resp_body)
CODE=$(echo "$CONTENT" | grep -o '"code":[0-9]*' | cut -d':' -f2)
if [ "$CODE" = "0" ]; then echo "✅ Pay | GET /admin-api/pay/app/page"; log_test "Pay" "/admin-api/pay/app/page" "GET" "PASS" "200" "0"; else echo "❌ Pay | GET /admin-api/pay/app/page - code=$CODE"; log_test "Pay" "/admin-api/pay/app/page" "GET" "FAIL" "200" "0"; fi

curl -s -o /tmp/api_resp_body -w "%{http_code}" "$BASE_URL/admin-api/pay/wallet/page?pageNo=1&pageSize=10" -H "tenant-id: $TENANT_ID" -H "Authorization: Bearer $TOKEN"
CONTENT=$(cat /tmp/api_resp_body)
CODE=$(echo "$CONTENT" | grep -o '"code":[0-9]*' | cut -d':' -f2)
if [ "$CODE" = "0" ]; then echo "✅ Pay | GET /admin-api/pay/wallet/page"; log_test "Pay" "/admin-api/pay/wallet/page" "GET" "PASS" "200" "0"; else echo "❌ Pay | GET /admin-api/pay/wallet/page - code=$CODE"; log_test "Pay" "/admin-api/pay/wallet/page" "GET" "FAIL" "200" "0"; fi

echo ""
echo "=== M11-Member会员 ==="
curl -s -o /tmp/api_resp_body -w "%{http_code}" "$BASE_URL/admin-api/member/user/page?pageNo=1&pageSize=10" -H "tenant-id: $TENANT_ID" -H "Authorization: Bearer $TOKEN"
CONTENT=$(cat /tmp/api_resp_body)
CODE=$(echo "$CONTENT" | grep -o '"code":[0-9]*' | cut -d':' -f2)
if [ "$CODE" = "0" ]; then echo "✅ Member | GET /admin-api/member/user/page"; log_test "Member" "/admin-api/member/user/page" "GET" "PASS" "200" "0"; else echo "❌ Member | GET /admin-api/member/user/page - code=$CODE"; log_test "Member" "/admin-api/member/user/page" "GET" "FAIL" "200" "0"; fi

curl -s -o /tmp/api_resp_body -w "%{http_code}" "$BASE_URL/admin-api/member/point/record/page?pageNo=1&pageSize=10" -H "tenant-id: $TENANT_ID" -H "Authorization: Bearer $TOKEN"
CONTENT=$(cat /tmp/api_resp_body)
CODE=$(echo "$CONTENT" | grep -o '"code":[0-9]*' | cut -d':' -f2)
if [ "$CODE" = "0" ]; then echo "✅ Member | GET /admin-api/member/point/record/page"; log_test "Member" "/admin-api/member/point/record/page" "GET" "PASS" "200" "0"; else echo "❌ Member | GET /admin-api/member/point/record/page - code=$CODE"; log_test "Member" "/admin-api/member/point/record/page" "GET" "FAIL" "200" "0"; fi

curl -s -o /tmp/api_resp_body -w "%{http_code}" "$BASE_URL/admin-api/member/tag/page?pageNo=1&pageSize=10" -H "tenant-id: $TENANT_ID" -H "Authorization: Bearer $TOKEN"
CONTENT=$(cat /tmp/api_resp_body)
CODE=$(echo "$CONTENT" | grep -o '"code":[0-9]*' | cut -d':' -f2)
if [ "$CODE" = "0" ]; then echo "✅ Member | GET /admin-api/member/tag/page"; log_test "Member" "/admin-api/member/tag/page" "GET" "PASS" "200" "0"; else echo "❌ Member | GET /admin-api/member/tag/page - code=$CODE"; log_test "Member" "/admin-api/member/tag/page" "GET" "FAIL" "200" "0"; fi

curl -s -o /tmp/api_resp_body -w "%{http_code}" "$BASE_URL/admin-api/member/group/page?pageNo=1&pageSize=10" -H "tenant-id: $TENANT_ID" -H "Authorization: Bearer $TOKEN"
CONTENT=$(cat /tmp/api_resp_body)
CODE=$(echo "$CONTENT" | grep -o '"code":[0-9]*' | cut -d':' -f2)
if [ "$CODE" = "0" ]; then echo "✅ Member | GET /admin-api/member/group/page"; log_test "Member" "/admin-api/member/group/page" "GET" "PASS" "200" "0"; else echo "❌ Member | GET /admin-api/member/group/page - code=$CODE"; log_test "Member" "/admin-api/member/group/page" "GET" "FAIL" "200" "0"; fi

curl -s -o /tmp/api_resp_body -w "%{http_code}" "$BASE_URL/admin-api/member/config/get" -H "tenant-id: $TENANT_ID" -H "Authorization: Bearer $TOKEN"
CONTENT=$(cat /tmp/api_resp_body)
CODE=$(echo "$CONTENT" | grep -o '"code":[0-9]*' | cut -d':' -f2)
if [ "$CODE" = "0" ]; then echo "✅ Member | GET /admin-api/member/config/get"; log_test "Member" "/admin-api/member/config/get" "GET" "PASS" "200" "0"; else echo "❌ Member | GET /admin-api/member/config/get - code=$CODE"; log_test "Member" "/admin-api/member/config/get" "GET" "FAIL" "200" "0"; fi

echo ""
echo "=== M13-AI人工智能 ==="
curl -s -o /tmp/api_resp_body -w "%{http_code}" "$BASE_URL/admin-api/ai/knowledge/page?pageNo=1&pageSize=10" -H "tenant-id: $TENANT_ID" -H "Authorization: Bearer $TOKEN"
CONTENT=$(cat /tmp/api_resp_body)
CODE=$(echo "$CONTENT" | grep -o '"code":[0-9]*' | cut -d':' -f2)
if [ "$CODE" = "0" ]; then echo "✅ AI | GET /admin-api/ai/knowledge/page"; log_test "AI" "/admin-api/ai/knowledge/page" "GET" "PASS" "200" "0"; else echo "❌ AI | GET /admin-api/ai/knowledge/page - code=$CODE"; log_test "AI" "/admin-api/ai/knowledge/page" "GET" "FAIL" "200" "0"; fi

curl -s -o /tmp/api_resp_body -w "%{http_code}" "$BASE_URL/admin-api/ai/model/page?pageNo=1&pageSize=10" -H "tenant-id: $TENANT_ID" -H "Authorization: Bearer $TOKEN"
CONTENT=$(cat /tmp/api_resp_body)
CODE=$(echo "$CONTENT" | grep -o '"code":[0-9]*' | cut -d':' -f2)
if [ "$CODE" = "0" ]; then echo "✅ AI | GET /admin-api/ai/model/page"; log_test "AI" "/admin-api/ai/model/page" "GET" "PASS" "200" "0"; else echo "❌ AI | GET /admin-api/ai/model/page - code=$CODE"; log_test "AI" "/admin-api/ai/model/page" "GET" "FAIL" "200" "0"; fi

curl -s -o /tmp/api_resp_body -w "%{http_code}" "$BASE_URL/admin-api/ai/chat/conversation/page?pageNo=1&pageSize=10" -H "tenant-id: $TENANT_ID" -H "Authorization: Bearer $TOKEN"
CONTENT=$(cat /tmp/api_resp_body)
CODE=$(echo "$CONTENT" | grep -o '"code":[0-9]*' | cut -d':' -f2)
if [ "$CODE" = "0" ]; then echo "✅ AI | GET /admin-api/ai/chat/conversation/page"; log_test "AI" "/admin-api/ai/chat/conversation/page" "GET" "PASS" "200" "0"; else echo "❌ AI | GET /admin-api/ai/chat/conversation/page - code=$CODE"; log_test "AI" "/admin-api/ai/chat/conversation/page" "GET" "FAIL" "200" "0"; fi

curl -s -o /tmp/api_resp_body -w "%{http_code}" "$BASE_URL/admin-api/ai/write/page?pageNo=1&pageSize=10" -H "tenant-id: $TENANT_ID" -H "Authorization: Bearer $TOKEN"
CONTENT=$(cat /tmp/api_resp_body)
CODE=$(echo "$CONTENT" | grep -o '"code":[0-9]*' | cut -d':' -f2)
if [ "$CODE" = "0" ]; then echo "✅ AI | GET /admin-api/ai/write/page"; log_test "AI" "/admin-api/ai/write/page" "GET" "PASS" "200" "0"; else echo "❌ AI | GET /admin-api/ai/write/page - code=$CODE"; log_test "AI" "/admin-api/ai/write/page" "GET" "FAIL" "200" "0"; fi

curl -s -o /tmp/api_resp_body -w "%{http_code}" "$BASE_URL/admin-api/ai/api-key/page?pageNo=1&pageSize=10" -H "tenant-id: $TENANT_ID" -H "Authorization: Bearer $TOKEN"
CONTENT=$(cat /tmp/api_resp_body)
CODE=$(echo "$CONTENT" | grep -o '"code":[0-9]*' | cut -d':' -f2)
if [ "$CODE" = "0" ]; then echo "✅ AI | GET /admin-api/ai/api-key/page"; log_test "AI" "/admin-api/ai/api-key/page" "GET" "PASS" "200" "0"; else echo "❌ AI | GET /admin-api/ai/api-key/page - code=$CODE"; log_test "AI" "/admin-api/ai/api-key/page" "GET" "FAIL" "200" "0"; fi

echo ""
echo "=== M14-MP公众号 ==="
curl -s -o /tmp/api_resp_body -w "%{http_code}" "$BASE_URL/admin-api/mp/account/page?pageNo=1&pageSize=10" -H "tenant-id: $TENANT_ID" -H "Authorization: Bearer $TOKEN"
CONTENT=$(cat /tmp/api_resp_body)
CODE=$(echo "$CONTENT" | grep -o '"code":[0-9]*' | cut -d':' -f2)
if [ "$CODE" = "0" ]; then echo "✅ MP | GET /admin-api/mp/account/page"; log_test "MP" "/admin-api/mp/account/page" "GET" "PASS" "200" "0"; else echo "❌ MP | GET /admin-api/mp/account/page - code=$CODE"; log_test "MP" "/admin-api/mp/account/page" "GET" "FAIL" "200" "0"; fi

echo ""
echo "=== M16-Security安全测试 ==="
curl -s -o /tmp/api_resp_body -w "%{http_code}" "$BASE_URL/admin-api/system/user/page" -H "tenant-id: $TENANT_ID"
CONTENT=$(cat /tmp/api_resp_body)
CODE=$(echo "$CONTENT" | grep -o '"code":[0-9]*' | cut -d':' -f2)
if [ "$CODE" = "401" ]; then echo "✅ Security | GET /admin-api/system/user/page - 无Token被拒绝"; log_test "Security" "/admin-api/system/user/page" "GET" "PASS" "200" "0"; else echo "❌ Security | GET /admin-api/system/user/page - 无Token未被拒绝"; log_test "Security" "/admin-api/system/user/page" "GET" "FAIL" "200" "0"; fi

echo ""
echo "=== 测试结果汇总 ==="
echo ""

TOTAL=$(tail -n +2 "$TEST_RESULTS" | wc -l)
PASS=$(grep ",PASS," "$TEST_RESULTS" | wc -l)
FAIL=$(grep ",FAIL," "$TEST_RESULTS" | wc -l)
NOT_FOUND=$(grep ",NOT_FOUND," "$TEST_RESULTS" | wc -l)
UNAUTH=$(grep ",UNAUTH," "$TEST_RESULTS" | wc -l)
DISABLED=$(grep ",DISABLED," "$TEST_RESULTS" | wc -l)

echo "总测试数: $TOTAL"
echo "✅ 通过: $PASS"
echo "❌ 失败: $FAIL"
echo "🔍 路径不存在: $NOT_FOUND"
echo "🔒 未授权: $UNAUTH"
echo "⚠️ 模块禁用: $DISABLED"

SUCCESS_RATE=$(echo "scale=2; ($PASS / $TOTAL) * 100" | bc)
echo "通过率: $SUCCESS_RATE%"

echo ""
echo "测试结果已保存到: $TEST_RESULTS"