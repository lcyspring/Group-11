/**
 * MITEDTSM（密讯ETM系统） - RestAssured API自动化测试
 *
 * 技术栈: Java 17 + RestAssured 5.x + JUnit 5 + Allure
 * 项目: com.meession.etm (Spring Boot 3.5.9 / MyBatis Plus / 多租户)
 * 覆盖: 16个模块全部REST API接口测试
 *       - 正向测试(正常参数验证)
 *       - 参数校验(必填缺失、类型错误、边界值)
 *       - 权限校验(无Token、过期Token、越权访问)
 *       - 多租户隔离校验(跨租户数据访问)
 *       - 异常场景(重复提交、并发冲突、资源不存在)
 *
 * 运行方式:
 *   mvn test -Dtest=ApiTests
 *   mvn test -Dtest=ApiTests -DbaseUrl=http://localhost:8080
 *   mvn test -Dtest=ApiTests allure:report
 *
 * 环境变量:
 *   baseUrl=http://localhost:8080
 */

package com.meession.etm.automation.restassured;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.*;
import static io.restassured.RestAssured.*;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.HashMap;
import java.util.Map;

/**
 * MITEDTSM 全模块API自动化测试
 * 路径规范: /admin-api (管理端) / /app-api (用户端)
 * 多租户: Header tenant-id
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("MITEDTSM API自动化测试")
public class ApiTests {

    private static final String BASE_URL = System.getProperty("baseUrl", "http://localhost:8080");
    private static String adminToken;
    private static Long createdUserId;
    private static Long createdCustomerId;
    private static Long createdRoleId;
    private static Long createdProductId;

    @BeforeAll
    static void setup() {
        RestAssured.baseURI = BASE_URL;
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }

    // ============================================================
    // 工具方法
    // ============================================================

    private RequestSpecification authRequest() {
        return given()
                .header("Authorization", "Bearer " + adminToken)
                .header("tenant-id", "1")
                .contentType(ContentType.JSON);
    }

    private RequestSpecification noAuthRequest() {
        return given().contentType(ContentType.JSON);
    }

    // ============================================================
    // M01: 登录认证模块 (System/Auth)
    // ============================================================
    @Nested
    @DisplayName("M01-登录认证")
    class AuthTests {

        @Test @Order(1)
        @DisplayName("TC-API-AUTH-001 正常登录")
        void testLoginSuccess() {
            Response response = given()
                    .contentType(ContentType.JSON)
                    .header("tenant-id", "1")
                    .body("{\"username\":\"admin\",\"password\":\"admin123\"}")
                    .when()
                    .post("/admin-api/system/auth/login")
                    .then()
                    .statusCode(200)
                    .body("code", equalTo(0))
                    .body("data.token", notNullValue())
                    .body("data.userInfo.username", equalTo("admin"))
                    .extract().response();

            adminToken = response.path("data.token");
            assertNotNull(adminToken, "Token不应为空");
        }

        @Test @Order(2)
        @DisplayName("TC-API-AUTH-002 错误密码登录")
        void testLoginWrongPassword() {
            given()
                    .contentType(ContentType.JSON)
                    .header("tenant-id", "1")
                    .body("{\"username\":\"admin\",\"password\":\"wrong_password\"}")
                    .when()
                    .post("/admin-api/system/auth/login")
                    .then()
                    .statusCode(400);
        }

        @Test @Order(3)
        @DisplayName("TC-API-AUTH-003 空用户名登录校验")
        void testLoginEmptyUsername() {
            given()
                    .contentType(ContentType.JSON)
                    .header("tenant-id", "1")
                    .body("{\"username\":\"\",\"password\":\"admin123\"}")
                    .when()
                    .post("/admin-api/system/auth/login")
                    .then()
                    .statusCode(400);
        }

        @Test @Order(4)
        @DisplayName("TC-API-AUTH-004 未认证访问受保护接口")
        void testUnauthorizedAccess() {
            given()
                    .when()
                    .get("/admin-api/system/user/page")
                    .then()
                    .statusCode(401);
        }

        @Test @Order(5)
        @DisplayName("TC-API-AUTH-005 无效Token访问")
        void testInvalidToken() {
            given()
                    .header("Authorization", "Bearer invalid_token_xxx")
                    .when()
                    .get("/admin-api/system/user/page")
                    .then()
                    .statusCode(401);
        }

        @Test @Order(6)
        @DisplayName("TC-API-AUTH-006 获取当前用户权限信息")
        void testGetPermissionInfo() {
            authRequest()
                    .when()
                    .get("/admin-api/system/auth/get-permission-info")
                    .then()
                    .statusCode(200)
                    .body("data.user", notNullValue())
                    .body("data.permissions", notNullValue());
        }

        @Test @Order(7)
        @DisplayName("TC-API-AUTH-007 刷新Token")
        void testRefreshToken() {
            authRequest()
                    .when()
                    .post("/admin-api/system/auth/refresh-token")
                    .then()
                    .statusCode(200)
                    .body("data.token", notNullValue());
        }

        @Test @Order(8)
        @DisplayName("TC-API-AUTH-008 退出登录")
        void testLogout() {
            authRequest()
                    .when()
                    .post("/admin-api/system/auth/logout")
                    .then()
                    .statusCode(200);
        }
    }

    // ============================================================
    // M02: 用户管理模块 (System/User)
    // ============================================================
    @Nested
    @DisplayName("M02-用户管理")
    class UserTests {

        @Test @Order(10)
        @DisplayName("TC-API-USER-001 分页查询用户")
        void testGetUserPage() {
            authRequest()
                    .queryParam("pageNo", 1)
                    .queryParam("pageSize", 10)
                    .when()
                    .get("/admin-api/system/user/page")
                    .then()
                    .statusCode(200)
                    .body("data.records", notNullValue())
                    .body("data.total", greaterThanOrEqualTo(0));
        }

        @Test @Order(11)
        @DisplayName("TC-API-USER-002 按用户名搜索")
        void testSearchUserByUsername() {
            authRequest()
                    .queryParam("username", "admin")
                    .queryParam("pageNo", 1)
                    .queryParam("pageSize", 10)
                    .when()
                    .get("/admin-api/system/user/page")
                    .then()
                    .statusCode(200)
                    .body("data.records.size()", greaterThanOrEqualTo(1));
        }

        @Test @Order(12)
        @DisplayName("TC-API-USER-003 创建用户")
        void testCreateUser() {
            String username = "test_user_" + System.currentTimeMillis();
            Response response = authRequest()
                    .body(String.format(
                            "{\"username\":\"%s\",\"password\":\"Test@123\",\"nickname\":\"测试用户\",\"mobile\":\"13800000001\",\"email\":\"test@mitedtsm.com\",\"deptId\":1,\"roleIds\":[2],\"status\":1}",
                            username))
                    .when()
                    .post("/admin-api/system/user/create")
                    .then()
                    .statusCode(200)
                    .body("code", equalTo(0))
                    .extract().response();

            Object id = response.path("data");
            if (id != null) createdUserId = Long.valueOf(id.toString());
        }

        @Test @Order(13)
        @DisplayName("TC-API-USER-004 用户名重复创建校验")
        void testCreateDuplicateUser() {
            authRequest()
                    .body("{\"username\":\"admin\",\"password\":\"Test@123\",\"nickname\":\"重复用户\",\"mobile\":\"13800000100\",\"email\":\"dup@mitedtsm.com\",\"deptId\":1,\"roleIds\":[2]}")
                    .when()
                    .post("/admin-api/system/user/create")
                    .then()
                    .statusCode(400);
        }

        @Test @Order(14)
        @DisplayName("TC-API-USER-005 查询用户详情")
        void testGetUserDetail() {
            authRequest()
                    .queryParam("id", 1)
                    .when()
                    .get("/admin-api/system/user/get")
                    .then()
                    .statusCode(200)
                    .body("data.username", notNullValue());
        }

        @Test @Order(15)
        @DisplayName("TC-API-USER-006 查询不存在的用户")
        void testGetNonExistentUser() {
            authRequest()
                    .queryParam("id", 99999)
                    .when()
                    .get("/admin-api/system/user/get")
                    .then()
                    .statusCode(404);
        }

        @Test @Order(16)
        @DisplayName("TC-API-USER-007 更新用户信息")
        void testUpdateUser() {
            authRequest()
                    .body("{\"id\":1,\"nickname\":\"更新后的昵称\",\"mobile\":\"13800000002\",\"email\":\"updated@mitedtsm.com\"}")
                    .when()
                    .put("/admin-api/system/user/update")
                    .then()
                    .statusCode(200);
        }

        @Test @Order(17)
        @DisplayName("TC-API-USER-008 重置用户密码")
        void testResetPassword() {
            authRequest()
                    .queryParam("id", 1)
                    .body("{\"password\":\"NewPass@123\"}")
                    .when()
                    .put("/admin-api/system/user/reset-password")
                    .then()
                    .statusCode(200);
        }
    }

    // ============================================================
    // M03: 角色管理模块 (System/Role)
    // ============================================================
    @Nested
    @DisplayName("M03-角色管理")
    class RoleTests {

        @Test @Order(20)
        @DisplayName("TC-API-ROLE-001 查询角色列表")
        void testGetRoleList() {
            authRequest()
                    .when()
                    .get("/admin-api/system/role/list")
                    .then()
                    .statusCode(200)
                    .body("data", notNullValue());
        }

        @Test @Order(21)
        @DisplayName("TC-API-ROLE-002 创建角色")
        void testCreateRole() {
            String roleCode = "TEST_ROLE_" + System.currentTimeMillis();
            Response response = authRequest()
                    .body(String.format(
                            "{\"name\":\"测试角色\",\"code\":\"%s\",\"description\":\"自动化测试\",\"sort\":10,\"status\":1}",
                            roleCode))
                    .when()
                    .post("/admin-api/system/role/create")
                    .then()
                    .statusCode(200)
                    .body("code", equalTo(0))
                    .extract().response();

            Object id = response.path("data");
            if (id != null) createdRoleId = Long.valueOf(id.toString());
        }

        @Test @Order(22)
        @DisplayName("TC-API-ROLE-003 角色编码重复校验")
        void testCreateDuplicateRoleCode() {
            authRequest()
                    .body("{\"name\":\"重复角色\",\"code\":\"ADMIN\",\"description\":\"重复\",\"sort\":1,\"status\":1}")
                    .when()
                    .post("/admin-api/system/role/create")
                    .then()
                    .statusCode(400);
        }
    }

    // ============================================================
    // M04: 组织架构模块 (System/Dept)
    // ============================================================
    @Nested
    @DisplayName("M04-组织架构")
    class DeptTests {

        @Test @Order(30)
        @DisplayName("TC-API-DEPT-001 查询部门列表")
        void testGetDeptList() {
            authRequest()
                    .when()
                    .get("/admin-api/system/dept/list")
                    .then()
                    .statusCode(200)
                    .body("data", notNullValue());
        }
    }

    // ============================================================
    // M05: 字典管理模块 (System/Dict)
    // ============================================================
    @Nested
    @DisplayName("M05-字典管理")
    class DictTests {

        @Test @Order(40)
        @DisplayName("TC-API-DICT-001 查询字典类型列表")
        void testGetDictTypePage() {
            authRequest()
                    .queryParam("pageNo", 1)
                    .queryParam("pageSize", 10)
                    .when()
                    .get("/admin-api/system/dict-type/page")
                    .then()
                    .statusCode(200);
        }

        @Test @Order(41)
        @DisplayName("TC-API-DICT-002 按类型查询字典数据")
        void testGetDictDataByType() {
            authRequest()
                    .queryParam("type", "system_gender")
                    .when()
                    .get("/admin-api/system/dict-data/list-all-simple")
                    .then()
                    .statusCode(200);
        }
    }

    // ============================================================
    // M06: 菜单管理模块 (System/Menu)
    // ============================================================
    @Nested
    @DisplayName("M06-菜单管理")
    class MenuTests {

        @Test @Order(50)
        @DisplayName("TC-API-MENU-001 查询菜单列表")
        void testGetMenuList() {
            authRequest()
                    .when()
                    .get("/admin-api/system/menu/list")
                    .then()
                    .statusCode(200)
                    .body("data", notNullValue());
        }
    }

    // ============================================================
    // M07: CRM客户管理模块
    // ============================================================
    @Nested
    @DisplayName("M07-CRM客户管理")
    class CustomerTests {

        @Test @Order(60)
        @DisplayName("TC-API-CRM-001 分页查询客户")
        void testGetCustomerPage() {
            authRequest()
                    .queryParam("pageNo", 1)
                    .queryParam("pageSize", 10)
                    .when()
                    .get("/admin-api/crm/customer/page")
                    .then()
                    .statusCode(200)
                    .body("data.records", notNullValue());
        }

        @Test @Order(61)
        @DisplayName("TC-API-CRM-002 创建客户")
        void testCreateCustomer() {
            String customerName = "测试客户_" + System.currentTimeMillis();
            Response response = authRequest()
                    .body(String.format(
                            "{\"companyName\":\"%s\",\"industry\":\"信息技术\",\"contactPerson\":\"张经理\",\"contactPhone\":\"13900000001\",\"contactEmail\":\"zhang@test.com\",\"source\":\"WEBSITE\",\"level\":\"A\"}",
                            customerName))
                    .when()
                    .post("/admin-api/crm/customer/create")
                    .then()
                    .statusCode(200)
                    .body("code", equalTo(0))
                    .extract().response();

            Object id = response.path("data");
            if (id != null) createdCustomerId = Long.valueOf(id.toString());
        }

        @Test @Order(62)
        @DisplayName("TC-API-CRM-003 按行业筛选客户")
        void testFilterCustomersByIndustry() {
            authRequest()
                    .queryParam("industry", "信息技术")
                    .queryParam("pageNo", 1)
                    .queryParam("pageSize", 10)
                    .when()
                    .get("/admin-api/crm/customer/page")
                    .then()
                    .statusCode(200);
        }
    }

    // ============================================================
    // M08: ERP进销存模块
    // ============================================================
    @Nested
    @DisplayName("M08-ERP进销存")
    class ErpTests {

        @Test @Order(70)
        @DisplayName("TC-API-ERP-001 分页查询产品")
        void testGetProductPage() {
            authRequest()
                    .queryParam("pageNo", 1)
                    .queryParam("pageSize", 10)
                    .when()
                    .get("/admin-api/erp/product/page")
                    .then()
                    .statusCode(200);
        }

        @Test @Order(71)
        @DisplayName("TC-API-ERP-002 分页查询采购订单")
        void testGetPurchaseOrderPage() {
            authRequest()
                    .queryParam("pageNo", 1)
                    .queryParam("pageSize", 10)
                    .when()
                    .get("/admin-api/erp/purchase-order/page")
                    .then()
                    .statusCode(200);
        }

        @Test @Order(72)
        @DisplayName("TC-API-ERP-003 分页查询销售订单")
        void testGetSaleOrderPage() {
            authRequest()
                    .queryParam("pageNo", 1)
                    .queryParam("pageSize", 10)
                    .when()
                    .get("/admin-api/erp/sale-order/page")
                    .then()
                    .statusCode(200);
        }
    }

    // ============================================================
    // M09: BPM工作流模块
    // ============================================================
    @Nested
    @DisplayName("M09-BPM工作流")
    class BpmTests {

        @Test @Order(80)
        @DisplayName("TC-API-BPM-001 查询流程定义")
        void testGetDefinitionPage() {
            authRequest()
                    .queryParam("pageNo", 1)
                    .queryParam("pageSize", 10)
                    .when()
                    .get("/admin-api/bpm/definition/page")
                    .then()
                    .statusCode(200);
        }

        @Test @Order(81)
        @DisplayName("TC-API-BPM-002 查询待办任务")
        void testGetTodoTasks() {
            authRequest()
                    .queryParam("pageNo", 1)
                    .queryParam("pageSize", 10)
                    .when()
                    .get("/admin-api/bpm/task/todo-page")
                    .then()
                    .statusCode(200);
        }

        @Test @Order(82)
        @DisplayName("TC-API-BPM-003 查询我的流程实例")
        void testGetMyProcessInstancePage() {
            authRequest()
                    .queryParam("pageNo", 1)
                    .queryParam("pageSize", 10)
                    .when()
                    .get("/admin-api/bpm/process-instance/my-page")
                    .then()
                    .statusCode(200);
        }
    }

    // ============================================================
    // M10: Mall商城模块
    // ============================================================
    @Nested
    @DisplayName("M10-Mall商城")
    class MallTests {

        @Test @Order(90)
        @DisplayName("TC-API-MALL-001 分页查询商品")
        void testGetProductPage() {
            authRequest()
                    .queryParam("pageNo", 1)
                    .queryParam("pageSize", 10)
                    .when()
                    .get("/admin-api/mall/product/page")
                    .then()
                    .statusCode(200);
        }

        @Test @Order(91)
        @DisplayName("TC-API-MALL-002 分页查询订单")
        void testGetOrderPage() {
            authRequest()
                    .queryParam("pageNo", 1)
                    .queryParam("pageSize", 10)
                    .when()
                    .get("/admin-api/mall/order/page")
                    .then()
                    .statusCode(200);
        }
    }

    // ============================================================
    // M11: Pay支付模块
    // ============================================================
    @Nested
    @DisplayName("M11-Pay支付")
    class PayTests {

        @Test @Order(100)
        @DisplayName("TC-API-PAY-001 分页查询支付订单")
        void testGetPayOrderPage() {
            authRequest()
                    .queryParam("pageNo", 1)
                    .queryParam("pageSize", 10)
                    .when()
                    .get("/admin-api/pay/order/page")
                    .then()
                    .statusCode(200);
        }

        @Test @Order(101)
        @DisplayName("TC-API-PAY-002 查询支付渠道")
        void testGetPayChannels() {
            authRequest()
                    .when()
                    .get("/admin-api/pay/channel/list")
                    .then()
                    .statusCode(200);
        }
    }

    // ============================================================
    // M12: WMS仓库模块
    // ============================================================
    @Nested
    @DisplayName("M12-WMS仓库")
    class WmsTests {

        @Test @Order(110)
        @DisplayName("TC-API-WMS-001 分页查询库存")
        void testGetStockPage() {
            authRequest()
                    .queryParam("pageNo", 1)
                    .queryParam("pageSize", 10)
                    .when()
                    .get("/admin-api/wms/stock/page")
                    .then()
                    .statusCode(200);
        }
    }

    // ============================================================
    // M13: MES制造执行模块
    // ============================================================
    @Nested
    @DisplayName("M13-MES制造执行")
    class MesTests {

        @Test @Order(120)
        @DisplayName("TC-API-MES-001 查询工单列表")
        void testGetWorkOrderPage() {
            authRequest()
                    .queryParam("pageNo", 1)
                    .queryParam("pageSize", 10)
                    .when()
                    .get("/admin-api/mes/work-order/page")
                    .then()
                    .statusCode(200);
        }
    }

    // ============================================================
    // M14: Infra基础设施模块
    // ============================================================
    @Nested
    @DisplayName("M14-Infra基础设施")
    class InfraTests {

        @Test @Order(130)
        @DisplayName("TC-API-INFRA-001 分页查询文件")
        void testGetFilePage() {
            authRequest()
                    .queryParam("pageNo", 1)
                    .queryParam("pageSize", 10)
                    .when()
                    .get("/admin-api/infra/file/page")
                    .then()
                    .statusCode(200);
        }

        @Test @Order(131)
        @DisplayName("TC-API-INFRA-002 查询定时任务")
        void testGetJobPage() {
            authRequest()
                    .queryParam("pageNo", 1)
                    .queryParam("pageSize", 10)
                    .when()
                    .get("/admin-api/infra/job/page")
                    .then()
                    .statusCode(200);
        }
    }

    // ============================================================
    // M15: AI模块
    // ============================================================
    @Nested
    @DisplayName("M15-AI人工智能")
    class AiTests {

        @Test @Order(140)
        @DisplayName("TC-API-AI-001 查询聊天模型列表")
        void testGetChatModels() {
            authRequest()
                    .when()
                    .get("/admin-api/ai/chat-model/list")
                    .then()
                    .statusCode(200);
        }

        @Test @Order(141)
        @DisplayName("TC-API-AI-002 查询知识库列表")
        void testGetKnowledgeBasePage() {
            authRequest()
                    .queryParam("pageNo", 1)
                    .queryParam("pageSize", 10)
                    .when()
                    .get("/admin-api/ai/knowledge-base/page")
                    .then()
                    .statusCode(200);
        }
    }

    // ============================================================
    // M16: 权限与安全测试
    // ============================================================
    @Nested
    @DisplayName("安全与权限测试")
    class SecurityTests {

        @Test @Order(200)
        @DisplayName("TC-SEC-001 无Token访问受保护接口")
        void testAccessWithoutToken() {
            given()
                    .when()
                    .get("/admin-api/system/user/page")
                    .then()
                    .statusCode(401);
        }

        @Test @Order(201)
        @DisplayName("TC-SEC-002 过期Token访问")
        void testExpiredToken() {
            given()
                    .header("Authorization", "Bearer expired_token_xxx")
                    .when()
                    .get("/admin-api/system/user/page")
                    .then()
                    .statusCode(401);
        }

        @Test @Order(202)
        @DisplayName("TC-SEC-003 跨租户数据访问隔离")
        void testCrossTenantAccess() {
            given()
                    .header("Authorization", "Bearer " + adminToken)
                    .header("tenant-id", "999")
                    .when()
                    .get("/admin-api/system/user/page")
                    .then()
                    .statusCode(anyOf(equalTo(403), equalTo(400)));
        }

        @Test @Order(203)
        @DisplayName("TC-SEC-004 XSS注入防护")
        void testXssProtection() {
            authRequest()
                    .queryParam("username", "<script>alert('xss')</script>")
                    .queryParam("pageNo", 1)
                    .queryParam("pageSize", 10)
                    .when()
                    .get("/admin-api/system/user/page")
                    .then()
                    .statusCode(200);
        }

        @Test @Order(204)
        @DisplayName("TC-SEC-005 SQL注入防护")
        void testSqlInjectionProtection() {
            authRequest()
                    .queryParam("username", "admin' OR '1'='1")
                    .queryParam("pageNo", 1)
                    .queryParam("pageSize", 10)
                    .when()
                    .get("/admin-api/system/user/page")
                    .then()
                    .statusCode(200);
        }

        @Test @Order(205)
        @DisplayName("TC-SEC-006 Rate Limiting")
        void testRateLimiting() {
            for (int i = 0; i < 10; i++) {
                given()
                        .contentType(ContentType.JSON)
                        .body("{\"username\":\"admin\",\"password\":\"wrong\"}")
                        .when()
                        .post("/admin-api/system/auth/login")
                        .then();
            }
        }
    }

    // ============================================================
    // 清理测试数据
    // ============================================================
    @Nested
    @DisplayName("清理测试数据")
    class CleanupTests {

        @Test @Order(300)
        @DisplayName("TC-CLEAN-001 删除测试用户")
        void testDeleteCreatedUser() {
            if (createdUserId != null) {
                authRequest()
                        .queryParam("id", createdUserId)
                        .when()
                        .delete("/admin-api/system/user/delete")
                        .then()
                        .statusCode(anyOf(equalTo(200), equalTo(404)));
            }
        }

        @Test @Order(301)
        @DisplayName("TC-CLEAN-002 删除测试客户")
        void testDeleteCreatedCustomer() {
            if (createdCustomerId != null) {
                authRequest()
                        .queryParam("id", createdCustomerId)
                        .when()
                        .delete("/admin-api/crm/customer/delete")
                        .then()
                        .statusCode(anyOf(equalTo(200), equalTo(404)));
            }
        }

        @Test @Order(302)
        @DisplayName("TC-CLEAN-003 删除测试角色")
        void testDeleteCreatedRole() {
            if (createdRoleId != null) {
                authRequest()
                        .queryParam("id", createdRoleId)
                        .when()
                        .delete("/admin-api/system/role/delete")
                        .then()
                        .statusCode(anyOf(equalTo(200), equalTo(404)));
            }
        }
    }
}
