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

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("MITEDTSM API自动化测试")
public class ApiTests {

    private static final String BASE_URL = System.getProperty("baseUrl", "http://localhost:8080");
    private static String adminToken;
    private static Long createdUserId;
    private static Long createdCustomerId;

    @BeforeAll
    static void setup() {
        RestAssured.baseURI = BASE_URL;
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }

    private RequestSpecification authRequest() {
        return given()
                .header("Authorization", "Bearer " + adminToken)
                .header("tenant-id", "1")
                .contentType(ContentType.JSON);
    }

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
    }

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
    }

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
    }

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
    }

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
    }

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
    }

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
    }

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
    }

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
    }
}