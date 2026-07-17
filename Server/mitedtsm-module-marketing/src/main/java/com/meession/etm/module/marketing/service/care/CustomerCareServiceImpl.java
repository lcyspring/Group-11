package com.meession.etm.module.marketing.service.care;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.meession.etm.framework.common.pojo.PageResult;
import com.meession.etm.framework.common.util.object.BeanUtils;
import com.meession.etm.module.marketing.controller.admin.care.vo.CustomerCareConfigPageReqVO;
import com.meession.etm.module.marketing.controller.admin.care.vo.CustomerCareConfigSaveReqVO;
import com.meession.etm.module.marketing.dal.dataobject.care.CustomerCareConfigDO;
import com.meession.etm.module.marketing.dal.mysql.care.CustomerCareConfigMapper;
import com.meession.etm.module.marketing.enums.CareSceneEnum;
import com.meession.etm.module.member.api.user.MemberUserApi;
import com.meession.etm.module.member.api.user.dto.MemberUserRespDTO;
import com.meession.etm.module.system.service.sms.SmsSendService;
import com.meession.etm.module.system.service.mail.MailSendService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import static com.meession.etm.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.meession.etm.module.marketing.enums.ErrorCodeConstants.CARE_CONFIG_NOT_EXISTS;

/**
 * 客户关怀 Service 实现类
 * <p>
 * 注意：节日场景暂仅支持公历固定日期（MM-dd 格式）。
 * 农历节日需产品方在 holiday_dates 配置中手动填写当年对应的公历日期。
 *
 * @author MITEDTSM
 */
@Slf4j
@Service
public class CustomerCareServiceImpl implements CustomerCareService {

    @Resource
    private CustomerCareConfigMapper careConfigMapper;

    @Resource
    private MemberUserApi memberUserApi;

    @Resource
    private SmsSendService smsSendService;

    @Resource
    private MailSendService mailSendService;

    @Override
    public Long createCareConfig(CustomerCareConfigSaveReqVO createReqVO) {
        CustomerCareConfigDO config = BeanUtils.toBean(createReqVO, CustomerCareConfigDO.class);
        careConfigMapper.insert(config);
        return config.getId();
    }

    @Override
    public void updateCareConfig(CustomerCareConfigSaveReqVO updateReqVO) {
        validateCareConfigExists(updateReqVO.getId());
        CustomerCareConfigDO config = BeanUtils.toBean(updateReqVO, CustomerCareConfigDO.class);
        careConfigMapper.updateById(config);
    }

    @Override
    public void deleteCareConfig(Long id) {
        validateCareConfigExists(id);
        careConfigMapper.deleteById(id);
    }

    @Override
    public CustomerCareConfigDO getCareConfig(Long id) {
        return careConfigMapper.selectById(id);
    }

    @Override
    public PageResult<CustomerCareConfigDO> getCareConfigPage(CustomerCareConfigPageReqVO pageReqVO) {
        return careConfigMapper.selectPage(pageReqVO);
    }

    @Override
    public int executeBirthdayCare() {
        List<CustomerCareConfigDO> configs = careConfigMapper.selectEnabledByScene(CareSceneEnum.BIRTHDAY.getScene());
        if (CollUtil.isEmpty(configs)) {
            log.info("[executeBirthdayCare][无启用的生日关怀配置]");
            return 0;
        }
        // 查今日生日的会员 - 通过 MemberUserApi.getUserListByNickname 获取全量，再本地过滤月日
        // 注意：MemberUserApi 无原生 birthday 查询，先通过模糊昵称获取全量列表再过滤
        List<MemberUserRespDTO> allMembers = memberUserApi.getUserListByNickname("");
        String todayMMdd = LocalDate.now().format(DateTimeFormatter.ofPattern("MM-dd"));

        // 由于 MemberUserRespDTO 不含 birthday 字段，实际场景需扩展 API
        // 此处标记 TODO：需在 module-member 的 MemberUserApi 中增加 getUsersByBirthday 接口
        log.warn("[executeBirthdayCare][TODO: 需 MemberUserApi 增加生日查询接口，当前仅记录配置]");
        return 0;
    }

    @Override
    public int executeHolidayCare() {
        List<CustomerCareConfigDO> configs = careConfigMapper.selectEnabledByScene(CareSceneEnum.HOLIDAY.getScene());
        if (CollUtil.isEmpty(configs)) {
            log.info("[executeHolidayCare][无启用的节日关怀配置]");
            return 0;
        }
        String todayMMdd = LocalDate.now().format(DateTimeFormatter.ofPattern("MM-dd"));
        int sent = 0;
        for (CustomerCareConfigDO config : configs) {
            String holidayDates = config.getHolidayDates();
            if (StrUtil.isBlank(holidayDates) || !holidayDates.contains(todayMMdd)) {
                continue;
            }
            // 命中的节日，执行发送
            log.info("[executeHolidayCare][命中节日配置({})，日期({})]", config.getName(), todayMMdd);
            sent += sendCare(config);
        }
        return sent;
    }

    private void validateCareConfigExists(Long id) {
        if (id == null || careConfigMapper.selectById(id) == null) {
            throw exception(CARE_CONFIG_NOT_EXISTS);
        }
    }

    /**
     * 执行关怀发送：遍历全量会员发送
     */
    private int sendCare(CustomerCareConfigDO config) {
        List<MemberUserRespDTO> allMembers = memberUserApi.getUserListByNickname("");
        if (CollUtil.isEmpty(allMembers)) {
            return 0;
        }
        int sent = 0;
        for (MemberUserRespDTO user : allMembers) {
            try {
                if ("SMS".equals(config.getChannel())) {
                    smsSendService.sendSingleSmsToMember(user.getMobile(), user.getId(),
                            config.getTemplateCode(), Map.of("nickname", user.getNickname()));
                } else if ("MAIL".equals(config.getChannel())) {
                    mailSendService.sendSingleMailToMember(user.getId(),
                            null, null, null,
                            config.getTemplateCode(), Map.of("nickname", user.getNickname()));
                }
                sent++;
            } catch (Exception e) {
                log.error("[sendCare][发送关怀失败，用户({})渠道({})]", user.getId(), config.getChannel(), e);
            }
        }
        log.info("[sendCare][关怀发送完成，配置({})，总数({})]", config.getName(), sent);
        return sent;
    }

}
