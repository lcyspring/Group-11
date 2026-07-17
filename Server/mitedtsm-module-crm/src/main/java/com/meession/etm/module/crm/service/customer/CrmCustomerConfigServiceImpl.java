package com.meession.etm.module.crm.service.customer;

import cn.hutool.core.collection.CollUtil;
import com.meession.etm.framework.common.pojo.PageResult;
import com.meession.etm.framework.common.util.object.BeanUtils;
import com.meession.etm.module.crm.controller.admin.customer.vo.config.CrmCustomerConfigSaveReqVO;
import com.meession.etm.module.crm.controller.admin.customer.vo.config.CrmCustomerConfigRespVO;
import com.meession.etm.module.crm.dal.dataobject.customer.CrmCustomerConfigDO;
import com.meession.etm.module.crm.dal.mysql.customer.CrmCustomerConfigMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.util.Collections;
import java.util.List;

@Service
@Validated
public class CrmCustomerConfigServiceImpl implements CrmCustomerConfigService {

    @Resource
    private CrmCustomerConfigMapper crmCustomerConfigMapper;

    @Override
    public Long createConfig(CrmCustomerConfigSaveReqVO reqVO) {
        CrmCustomerConfigDO config = BeanUtils.toBean(reqVO, CrmCustomerConfigDO.class);
        if (config.getStatus() == null) {
            config.setStatus(true);
        }
        if (config.getSort() == null) {
            config.setSort(0);
        }
        crmCustomerConfigMapper.insert(config);
        return config.getId();
    }

    @Override
    public void updateConfig(CrmCustomerConfigSaveReqVO reqVO) {
        CrmCustomerConfigDO config = BeanUtils.toBean(reqVO, CrmCustomerConfigDO.class);
        crmCustomerConfigMapper.updateById(config);
    }

    @Override
    public void deleteConfig(Long id) {
        crmCustomerConfigMapper.deleteById(id);
    }

    @Override
    public CrmCustomerConfigDO getConfig(Long id) {
        return crmCustomerConfigMapper.selectById(id);
    }

    @Override
    public List<CrmCustomerConfigDO> getConfigListByType(String configType) {
        return crmCustomerConfigMapper.selectListByConfigType(configType);
    }

    @Override
    public List<CrmCustomerConfigRespVO> getConfigListByTypeVo(String configType) {
        List<CrmCustomerConfigDO> list = getConfigListByType(configType);
        if (CollUtil.isEmpty(list)) {
            return Collections.emptyList();
        }
        return BeanUtils.toBean(list, CrmCustomerConfigRespVO.class);
    }

    @Override
    public PageResult<CrmCustomerConfigRespVO> getConfigPage(String configType) {
        List<CrmCustomerConfigDO> list = crmCustomerConfigMapper.selectListByConfigTypeIncludeDisabled(configType);
        List<CrmCustomerConfigRespVO> voList = BeanUtils.toBean(list, CrmCustomerConfigRespVO.class);
        return new PageResult<>(voList, (long) voList.size());
    }

    @Override
    public void validateConfig(Long id) {
        CrmCustomerConfigDO config = getConfig(id);
        if (config == null) {
            throw new IllegalArgumentException("配置不存在");
        }
    }

}