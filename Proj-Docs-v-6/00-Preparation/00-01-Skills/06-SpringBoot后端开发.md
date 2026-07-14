# Spring Boot 后端开发

## 学习目标

- 掌握密讯ETM的后端项目结构
- 学会编写完整 CRUD 接口（Controller → Service → DAL）
- 理解 MyBatis-Plus + MapStruct 的使用方式
- 掌握多租户数据隔离的实现原理

## 前置知识与课时

- **前置知识**：Java 基础、Spring Boot 基础、SQL

---

## 核心内容

### 6.1 项目结构

```
mitedtsm-module-system/
├── mitedtsm-module-system-api/         # API接口 + DTO定义
└── mitedtsm-module-system-biz/         # 业务实现
    ├── controller/
    │   ├── admin/                      # 管理端控制器 (/admin-api)
    │   │   └── user/
    │   │       └── UserController.java
    │   └── app/                        # 用户端控制器 (/app-api)
    │       └── user/
    │           └── AppUserController.java
    ├── service/
    │   ├── UserService.java            # 接口
    │   └── impl/
    │       └── UserServiceImpl.java    # 实现
    ├── convert/
    │   └── UserConvert.java            # MapStruct DO↔VO转换
    ├── dal/
    │   ├── dataobject/
    │   │   └── UserDO.java             # 继承 TenantBaseDO
    │   └── mapper/
    │       └── UserMapper.java         # MyBatis-Plus Mapper
    ├── enums/                          # 枚举常量
    ├── framework/                      # 模块级工具
    └── util/                           # 工具类
```

### 6.2 核心代码示例

#### DataObject（DO类）

```java
package com.meession.etm.module.system.dal.dataobject.user;

import com.baomidou.mybatisplus.annotation.*;
import com.meession.etm.framework.tenant.core.db.TenantBaseDO;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("system_user")
public class AdminUserDO extends TenantBaseDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String username;
    private String password;
    private String nickname;
    private String email;
    private String mobile;
    private Integer sex;
    private Integer status;
    private String avatar;

    // create_time, update_time, deleted, tenant_id 继承自 TenantBaseDO
}
```

#### Mapper 接口

```java
package com.meession.etm.module.system.dal.mapper.user;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.meession.etm.framework.mybatis.core.mapper.BaseMapperX;
import com.meession.etm.module.system.dal.dataobject.user.AdminUserDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AdminUserMapper extends BaseMapperX<AdminUserDO> {

    default AdminUserDO selectByUsername(String username) {
        return selectOne(new LambdaQueryWrapper<AdminUserDO>()
                .eq(AdminUserDO::getUsername, username));
    }
}
```

> 注意：`BaseMapperX` 是项目自定义的增强 Mapper，扩展了 `BaseMapper` 的分页查询能力。租户过滤由 `TenantBaseDO` + MyBatis 拦截器自动完成。

#### Service 层

```java
package com.meession.etm.module.system.service.user;

import com.meession.etm.module.system.controller.admin.user.vo.UserPageReqVO;
import com.meession.etm.module.system.controller.admin.user.vo.UserCreateReqVO;
import com.meession.etm.module.system.controller.admin.user.vo.UserUpdateReqVO;
import com.meession.etm.framework.common.pojo.PageResult;

public interface AdminUserService {

    PageResult<AdminUserDO> getUserPage(UserPageReqVO reqVO);

    AdminUserDO getUser(Long id);

    Long createUser(UserCreateReqVO reqVO);

    void updateUser(UserUpdateReqVO reqVO);

    void deleteUser(Long id);

    void resetPassword(Long id, String password);
}
```

```java
package com.meession.etm.module.system.service.user.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.meession.etm.framework.common.pojo.PageResult;
import com.meession.etm.module.system.dal.dataobject.user.AdminUserDO;
import com.meession.etm.module.system.dal.mapper.user.AdminUserMapper;
import com.meession.etm.module.system.service.user.AdminUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminUserServiceImpl implements AdminUserService {

    private final AdminUserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public PageResult<AdminUserDO> getUserPage(UserPageReqVO reqVO) {
        Page<AdminUserDO> page = userMapper.selectPage(
                new Page<>(reqVO.getPageNo(), reqVO.getPageSize()),
                new LambdaQueryWrapper<AdminUserDO>()
                        .likeIfPresent(AdminUserDO::getUsername, reqVO.getUsername())
                        .likeIfPresent(AdminUserDO::getMobile, reqVO.getMobile())
                        .eqIfPresent(AdminUserDO::getStatus, reqVO.getStatus())
                        .orderByDesc(AdminUserDO::getId)
        );
        return new PageResult<>(page.getRecords(), page.getTotal());
    }

    @Override
    public AdminUserDO getUser(Long id) {
        return userMapper.selectById(id);
    }

    @Override
    @Transactional
    public Long createUser(UserCreateReqVO reqVO) {
        // 校验用户名唯一性
        AdminUserDO existUser = userMapper.selectByUsername(reqVO.getUsername());
        if (existUser != null) {
            throw new RuntimeException("用户名已存在");
        }
        // 组装DO
        AdminUserDO user = new AdminUserDO();
        user.setUsername(reqVO.getUsername());
        user.setPassword(passwordEncoder.encode(reqVO.getPassword()));
        user.setNickname(reqVO.getNickname());
        user.setEmail(reqVO.getEmail());
        user.setMobile(reqVO.getMobile());
        user.setStatus(reqVO.getStatus());
        user.setDeptId(reqVO.getDeptId());
        // 插入数据库
        userMapper.insert(user);
        log.info("创建用户成功: id={}, username={}", user.getId(), user.getUsername());
        return user.getId();
    }

    @Override
    @Transactional
    public void updateUser(UserUpdateReqVO reqVO) {
        AdminUserDO user = userMapper.selectById(reqVO.getId());
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        user.setNickname(reqVO.getNickname());
        user.setEmail(reqVO.getEmail());
        user.setMobile(reqVO.getMobile());
        user.setStatus(reqVO.getStatus());
        userMapper.updateById(user);
    }

    @Override
    @Transactional
    public void deleteUser(Long id) {
        userMapper.deleteById(id);  // 逻辑删除，MyBatis-Plus自动设置deleted=1
    }

    @Override
    @Transactional
    public void resetPassword(Long id, String password) {
        AdminUserDO user = userMapper.selectById(id);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        user.setPassword(passwordEncoder.encode(password));
        userMapper.updateById(user);
    }
}
```

#### Controller 层

```java
package com.meession.etm.module.system.controller.admin.user;

import com.meession.etm.framework.common.pojo.CommonResult;
import com.meession.etm.framework.common.pojo.PageResult;
import com.meession.etm.module.system.controller.admin.user.vo.*;
import com.meession.etm.module.system.service.user.AdminUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Tag(name = "管理后台 - 用户管理")
@RestController
@RequestMapping("/system/user")
@RequiredArgsConstructor
public class UserController {

    private final AdminUserService userService;

    @GetMapping("/page")
    @Operation(summary = "用户分页查询")
    @PreAuthorize("@ss.hasPermission('system:user:query')")
    public CommonResult<PageResult<UserRespVO>> getUserPage(@Valid UserPageReqVO reqVO) {
        PageResult<AdminUserDO> pageResult = userService.getUserPage(reqVO);
        return CommonResult.success(UserConvert.INSTANCE.convertPage(pageResult));
    }

    @GetMapping("/get")
    @Operation(summary = "用户详情查询")
    @PreAuthorize("@ss.hasPermission('system:user:query')")
    public CommonResult<UserRespVO> getUser(@RequestParam("id") Long id) {
        AdminUserDO user = userService.getUser(id);
        return CommonResult.success(UserConvert.INSTANCE.convert(user));
    }

    @PostMapping("/create")
    @Operation(summary = "新增用户")
    @PreAuthorize("@ss.hasPermission('system:user:create')")
    public CommonResult<Long> createUser(@Valid @RequestBody UserCreateReqVO reqVO) {
        Long id = userService.createUser(reqVO);
        return CommonResult.success(id);
    }

    @PutMapping("/update")
    @Operation(summary = "更新用户")
    @PreAuthorize("@ss.hasPermission('system:user:update')")
    public CommonResult<Boolean> updateUser(@Valid @RequestBody UserUpdateReqVO reqVO) {
        userService.updateUser(reqVO);
        return CommonResult.success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除用户")
    @PreAuthorize("@ss.hasPermission('system:user:delete')")
    public CommonResult<Boolean> deleteUser(@RequestParam("id") Long id) {
        userService.deleteUser(id);
        return CommonResult.success(true);
    }
}
```

#### MapStruct 转换器

```java
package com.meession.etm.module.system.convert.user;

import com.meession.etm.module.system.dal.dataobject.user.AdminUserDO;
import com.meession.etm.module.system.controller.admin.user.vo.UserRespVO;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface UserConvert {

    UserConvert INSTANCE = Mappers.getMapper(UserConvert.class);

    UserRespVO convert(AdminUserDO bean);

    // PageResult转换由框架层统一处理
}
```

### 6.3 多租户实现原理

```
┌─────────────────────────────────────────────────┐
│              HTTP Request                        │
│  Header: tenant-id = 1                          │
├─────────────────────────────────────────────────┤
│              TenantContextHolder                 │
│  ThreadLocal<Long> tenantId = 1                 │
├─────────────────────────────────────────────────┤
│         MyBatis TenantLineInnerInterceptor       │
│  自动在SQL WHERE中添加 tenant_id = 1             │
│                                                  │
│  SELECT * FROM system_user                      │
│  → SELECT * FROM system_user WHERE tenant_id=1  │
├─────────────────────────────────────────────────┤
│  @TenantIgnore                                  │
│  跳过租户过滤 (用于共享表/平台级查询)              │
└─────────────────────────────────────────────────┘
```

---

## 练习

### 练习 1：完成角色管理 CRUD

参照用户管理，实现角色管理的完整 CRUD 接口（从 DO → Mapper → Service → Controller → Convert）。

### 练习 2：实现部门管理

实现部门管理的 CRUD 接口，包含树形结构查询功能。

### 练习 3：编写 VO 类

为 CRM 客户管理模块编写完整的 VO 类体系（CreateReqVO / UpdateReqVO / PageReqVO / RespVO）。

---

## 常见问题

**Q：DO类应该继承 BaseDO 还是 TenantBaseDO？**

需要按租户隔离的业务表继承 `TenantBaseDO`（如用户、客户、订单）；共享表继承 `BaseDO`（如系统配置、字典类型）。如果需要跳过租户过滤，在方法上加 `@TenantIgnore` 注解。

**Q：为什么使用 MapStruct 而不是 BeanUtils？**

MapStruct 在编译期生成转换代码，性能优于反射-based 的 BeanUtils，且类型安全，编译期即可发现字段不匹配问题。项目中 `lombok-mapstruct-binding` 确保 Lombok 与 MapStruct 协作正常。

**Q：密码编码使用什么算法？**

使用 Spring Security 的 `PasswordEncoder`（默认 BCryptPasswordEncoder）。密讯ETM项目不直接使用 BCrypt API，而是通过 Spring Security 的代理进行编码，方便后续切换算法。

---

## 小结

- 项目结构：controller → service → convert → dal（mapper + dataobject）
- DO 类继承 TenantBaseDO 实现多租户隔离
- MyBatis-Plus 拦截器自动添加租户过滤
- MapStruct 用于 DO ↔ VO 高效转换
- @PreAuthorize 注解实现细粒度权限控制

---

## 参考资料

- Spring Boot 3.5.x 文档：https://spring.io/projects/spring-boot
- MyBatis-Plus 文档：https://baomidou.com/
- MapStruct 文档：https://mapstruct.org/
- Code/Server/EDIT_GUIDE_BY_3031.md — 后端开发指南
