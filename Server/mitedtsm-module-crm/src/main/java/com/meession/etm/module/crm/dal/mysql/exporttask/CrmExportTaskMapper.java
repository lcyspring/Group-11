package com.meession.etm.module.crm.dal.mysql.exporttask;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.meession.etm.framework.common.pojo.PageResult;
import com.meession.etm.framework.mybatis.core.mapper.BaseMapperX;
import com.meession.etm.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.meession.etm.module.crm.controller.admin.exporttask.vo.CrmExportTaskPageReqVO;
import com.meession.etm.module.crm.dal.dataobject.exporttask.CrmExportTaskDO;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Mapper
public interface CrmExportTaskMapper extends BaseMapperX<CrmExportTaskDO> {

    default PageResult<CrmExportTaskDO> selectPage(CrmExportTaskPageReqVO request, Long userId) {
        return selectPage(request, new LambdaQueryWrapperX<CrmExportTaskDO>()
                .eq(CrmExportTaskDO::getCreatorUserId, userId)
                .eqIfPresent(CrmExportTaskDO::getObjectType, request.getObjectType())
                .eqIfPresent(CrmExportTaskDO::getStatus, request.getStatus())
                .orderByDesc(CrmExportTaskDO::getId));
    }

    default long selectActiveCount(Long userId, Collection<Integer> statuses) {
        return selectCount(new LambdaQueryWrapperX<CrmExportTaskDO>()
                .eq(CrmExportTaskDO::getCreatorUserId, userId)
                .in(CrmExportTaskDO::getStatus, statuses));
    }

    default List<Long> selectQueuedIds(int limit, Integer queuedStatus, LocalDateTime now) {
        return selectList(new LambdaQueryWrapperX<CrmExportTaskDO>()
                .select(CrmExportTaskDO::getId)
                .eq(CrmExportTaskDO::getStatus, queuedStatus)
                .gt(CrmExportTaskDO::getExpiresAt, now)
                .orderByAsc(CrmExportTaskDO::getId)
                .last("LIMIT " + limit)).stream().map(CrmExportTaskDO::getId).toList();
    }

    default List<CrmExportTaskDO> selectExpiredList(LocalDateTime now, Integer expiredStatus, int limit) {
        return selectList(new LambdaQueryWrapperX<CrmExportTaskDO>()
                .ne(CrmExportTaskDO::getStatus, expiredStatus)
                .le(CrmExportTaskDO::getExpiresAt, now)
                .orderByAsc(CrmExportTaskDO::getId)
                .last("LIMIT " + limit));
    }

    default int transition(Long id, Integer expected, Integer target, LocalDateTime startedAt) {
        return update(new LambdaUpdateWrapper<CrmExportTaskDO>()
                .eq(CrmExportTaskDO::getId, id).eq(CrmExportTaskDO::getStatus, expected)
                .set(CrmExportTaskDO::getStatus, target)
                .set(startedAt != null, CrmExportTaskDO::getStartedAt, startedAt));
    }

    default int markSuccess(Long id, Integer running, Integer success, String fileUrl,
                            String fileName, String contentType, LocalDateTime finishedAt) {
        return update(new LambdaUpdateWrapper<CrmExportTaskDO>()
                .eq(CrmExportTaskDO::getId, id).eq(CrmExportTaskDO::getStatus, running)
                .set(CrmExportTaskDO::getStatus, success).set(CrmExportTaskDO::getFileUrl, fileUrl)
                .set(CrmExportTaskDO::getFileName, fileName).set(CrmExportTaskDO::getContentType, contentType)
                .set(CrmExportTaskDO::getFinishedAt, finishedAt).set(CrmExportTaskDO::getFailureReason, null));
    }

    default int markFailure(Long id, Integer running, Integer failed, String reason, LocalDateTime finishedAt) {
        return update(new LambdaUpdateWrapper<CrmExportTaskDO>()
                .eq(CrmExportTaskDO::getId, id).eq(CrmExportTaskDO::getStatus, running)
                .set(CrmExportTaskDO::getStatus, failed).set(CrmExportTaskDO::getFailureReason, reason)
                .set(CrmExportTaskDO::getFinishedAt, finishedAt));
    }

    default int markExpired(Long id, Integer expired) {
        return update(new LambdaUpdateWrapper<CrmExportTaskDO>()
                .eq(CrmExportTaskDO::getId, id).ne(CrmExportTaskDO::getStatus, expired)
                .set(CrmExportTaskDO::getStatus, expired).set(CrmExportTaskDO::getFileUrl, null)
                .set(CrmExportTaskDO::getDownloadTokenHash, null)
                .set(CrmExportTaskDO::getDownloadTokenExpiresAt, null));
    }

    default int issueToken(Long id, Integer success, String hash, LocalDateTime expiresAt) {
        return update(new LambdaUpdateWrapper<CrmExportTaskDO>()
                .eq(CrmExportTaskDO::getId, id).eq(CrmExportTaskDO::getStatus, success)
                .set(CrmExportTaskDO::getDownloadTokenHash, hash)
                .set(CrmExportTaskDO::getDownloadTokenExpiresAt, expiresAt));
    }

    default int consumeToken(Long id, Integer success, String hash, LocalDateTime now) {
        return update(new LambdaUpdateWrapper<CrmExportTaskDO>()
                .eq(CrmExportTaskDO::getId, id).eq(CrmExportTaskDO::getStatus, success)
                .eq(CrmExportTaskDO::getDownloadTokenHash, hash)
                .gt(CrmExportTaskDO::getDownloadTokenExpiresAt, now)
                .set(CrmExportTaskDO::getDownloadTokenHash, null)
                .set(CrmExportTaskDO::getDownloadTokenExpiresAt, null)
                .set(CrmExportTaskDO::getDownloadedAt, now));
    }
}
