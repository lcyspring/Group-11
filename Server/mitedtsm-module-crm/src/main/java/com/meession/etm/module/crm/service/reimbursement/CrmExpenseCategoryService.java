package com.meession.etm.module.crm.service.reimbursement;

import com.meession.etm.module.crm.controller.admin.reimbursement.vo.CrmExpenseCategorySaveReqVO;
import com.meession.etm.module.crm.dal.dataobject.reimbursement.CrmExpenseCategoryDO;

import java.util.List;

public interface CrmExpenseCategoryService {
    Long createCategory(CrmExpenseCategorySaveReqVO reqVO);
    void updateCategory(CrmExpenseCategorySaveReqVO reqVO);
    void deleteCategory(Long id);
    CrmExpenseCategoryDO getCategory(Long id);
    List<CrmExpenseCategoryDO> getCategoryList(Integer status);
}
