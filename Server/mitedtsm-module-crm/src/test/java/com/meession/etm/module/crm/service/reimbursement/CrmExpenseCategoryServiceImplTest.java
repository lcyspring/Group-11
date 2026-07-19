package com.meession.etm.module.crm.service.reimbursement;

import com.meession.etm.module.crm.controller.admin.reimbursement.vo.CrmExpenseCategorySaveReqVO;
import com.meession.etm.module.crm.dal.dataobject.reimbursement.CrmExpenseCategoryDO;
import com.meession.etm.module.crm.dal.mysql.reimbursement.CrmExpenseCategoryMapper;
import com.meession.etm.module.crm.dal.mysql.reimbursement.CrmReimbursementItemMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static com.meession.etm.framework.test.core.util.AssertUtils.assertServiceException;
import static com.meession.etm.module.crm.enums.ErrorCodeConstants.REIMBURSEMENT_CATEGORY_CODE_EXISTS;
import static com.meession.etm.module.crm.enums.ErrorCodeConstants.REIMBURSEMENT_CATEGORY_USED;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CrmExpenseCategoryServiceImplTest {
    @Mock private CrmExpenseCategoryMapper categoryMapper;
    @Mock private CrmReimbursementItemMapper itemMapper;
    @InjectMocks private CrmExpenseCategoryServiceImpl service;

    @Test
    void createRejectsDuplicateCode() {
        when(categoryMapper.selectByCode("TRAVEL")).thenReturn(new CrmExpenseCategoryDO().setId(1L));

        assertServiceException(() -> service.createCategory(request()), REIMBURSEMENT_CATEGORY_CODE_EXISTS);
        verify(categoryMapper, never()).insert(any(CrmExpenseCategoryDO.class));
    }

    @Test
    void deleteRejectsUsedCategory() {
        when(categoryMapper.selectById(1L)).thenReturn(new CrmExpenseCategoryDO().setId(1L));
        when(itemMapper.selectCountByCategoryId(1L)).thenReturn(2L);

        assertServiceException(() -> service.deleteCategory(1L), REIMBURSEMENT_CATEGORY_USED);
        verify(categoryMapper, never()).deleteById(1L);
    }

    private static CrmExpenseCategorySaveReqVO request() {
        return new CrmExpenseCategorySaveReqVO().setCode("TRAVEL").setName("差旅费")
                .setStatus(0).setSort(10);
    }
}
