package com.meession.etm.module.crm.controller.admin.marketing.vo;

import lombok.Data;

import java.util.List;

@Data
public class CrmMarketingTargetOptionsRespVO {
    private List<Customer> customers;
    private List<Contact> contacts;

    @Data
    public static class Customer {
        private Long id;
        private String name;
        private String mobile;
        private String email;
    }

    @Data
    public static class Contact {
        private Long id;
        private Long customerId;
        private String name;
        private String mobile;
        private String email;
    }
}
