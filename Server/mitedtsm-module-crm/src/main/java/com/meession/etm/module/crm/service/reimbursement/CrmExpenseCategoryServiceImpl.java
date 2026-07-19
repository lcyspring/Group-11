package com.meession.etm.module.crm.service.reimbursement;

import cn.hutool.core.util.ObjUtil;
import com.meession.etm.framework.common.util.object.BeanUtils;
import com.meession.etm.module.crm.controller.admin.reimbursement.vo.CrmExpenseCategorySaveReqVO;
import com.meession.etm.module.crm.dal.dataobject.reimbursement.CrmExpenseCategoryDO;
import com.meession.etm.module.crm.dal.mysql.reimbursement.CrmExpenseCategoryMapper;
import com.meession.etm.module.crm.dal.mysql.reimbursement.CrmReimbursementItemMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.List;

import static com.meession.etm.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.meession.etm.module.crm.enums.ErrorCodeConstants.*;

@Service
@Validated
public class CrmExpenseCategoryServiceImpl implements CrmExpenseCategoryService {
    @Resource private CrmExpenseCategoryMapper categoryMapper;
    @Resource private CrmReimbursementItemMapper itemMapper;

    @Override
    public Long createCategory(CrmExpenseCategorySaveReqVO reqVO) {
        validateUnique(null, reqVO.getCode(), reqVO.getName());
        CrmExpenseCategoryDO category = BeanUtils.toBean(reqVO, CrmExpenseCategoryDO.class);
        categoryMapper.insert(category);
        return category.getId();
    }

    @Override
    public void updateCategory(CrmExpenseCategorySaveReqVO reqVO) {
        validateExists(reqVO.getId());
        validateUnique(reqVO.getId(), reqVO.getCode(), reqVO.getName());
        categoryMapper.updateById(BeanUtils.toBean(reqVO, CrmExpenseCategoryDO.class));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteCategory(Long id) {
        validateExists(id);
        if (itemMapper.selectCountByCategoryId(id) > 0) {
            throw exception(REIMBURSEMENT_CATEGORY_USED);
        }
        categoryMapper.deleteById(id);
    }

    @Override
    public CrmExpenseCategoryDO getCategory(Long id) {
        return validateExists(id);
    }

    @Override
    public List<CrmExpenseCategoryDO> getCategoryList(Integer status) {
        return categoryMapper.selectListByStatus(status);
    }

    private CrmExpenseCategoryDO validateExists(Long id) {
        CrmExpenseCategoryDO category = categoryMapper.selectById(id);
        if (category == null) {
            throw exception(REIMBURSEMENT_CATEGORY_NOT_EXISTS);
        }
        return category;
    }

    private void validateUnique(Long id, String code, String name) {
        CrmExpenseCategoryDO byCode = categoryMapper.selectByCode(code);
        if (byCode != null && ObjUtil.notEqual(byCode.getId(), id)) {
            throw exception(REIMBURSEMENT_CATEGORY_CODE_EXISTS);
        }
        CrmExpenseCategoryDO byName = categoryMapper.selectByName(name);
        if (byName != null && ObjUtil.notEqual(byName.getId(), id)) {
            throw exception(REIMBURSEMENT_CATEGORY_NAME_EXISTS);
        }
    }
}
