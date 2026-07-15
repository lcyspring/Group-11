package com.meession.etm.module.crm.dal.mysql.activity;

import com.meession.etm.framework.common.pojo.PageResult;
import com.meession.etm.framework.mybatis.core.mapper.BaseMapperX;
import com.meession.etm.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.meession.etm.module.crm.controller.admin.activity.vo.CrmActivityPageReqVO;
import com.meession.etm.module.crm.dal.dataobject.activity.CrmSmsRecordDO;
import com.meession.etm.module.crm.enums.common.CrmBizTypeEnum;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface CrmSmsRecordMapper extends BaseMapperX<CrmSmsRecordDO> {
    default PageResult<CrmSmsRecordDO> selectPage(CrmActivityPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<CrmSmsRecordDO>()
                .eq(CrmSmsRecordDO::getBizType, reqVO.getBizType())
                .eq(CrmSmsRecordDO::getBizId, reqVO.getBizId())
                .orderByDesc(CrmSmsRecordDO::getOccurredTime).orderByDesc(CrmSmsRecordDO::getId));
    }

    @Update("""
            UPDATE crm_sms_record SET biz_type=#{customerType}, biz_id=#{customerId},
              source_clue_id=#{clueId}, updater=#{userId}, update_time=NOW()
            WHERE deleted=b'0' AND biz_type=#{clueType} AND biz_id=#{clueId}
              AND source_clue_id IS NULL
            """)
    int migrateFromClue(@Param("clueType") Integer clueType, @Param("customerType") Integer customerType,
                        @Param("clueId") Long clueId, @Param("customerId") Long customerId,
                        @Param("userId") Long userId);

    default int migrateFromClue(Long clueId, Long customerId, Long userId) {
        return migrateFromClue(CrmBizTypeEnum.CRM_CLUE.getType(), CrmBizTypeEnum.CRM_CUSTOMER.getType(),
                clueId, customerId, userId);
    }
}
