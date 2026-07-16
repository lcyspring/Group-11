package com.meession.etm.module.crm.controller.admin.workorder.vo;

import lombok.Data;

import java.util.List;

@Data
public class CrmWorkOrderDispatchContextRespVO {
    private boolean enabled;
    private boolean autoAssignOnCreate;
    private String fallbackMode;
    private Integer maxCcUsers;
    private boolean manualAssignmentAllowed;
    private List<CrmWorkOrderGroupRespVO> groups;
    private List<User> candidates;

    @Data
    public static class User {
        private Long id;
        private String nickname;
        private Long deptId;
        private String source;
        private Integer openCount;
    }
}
