package com.meession.etm.module.crm.service.customer;

import cn.hutool.core.util.StrUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.meession.etm.framework.common.util.json.JsonUtils;
import com.meession.etm.framework.common.util.object.BeanUtils;
import com.meession.etm.framework.dict.core.DictFrameworkUtils;
import com.meession.etm.framework.excel.core.util.ExcelUtils;
import com.meession.etm.framework.ip.core.Area;
import com.meession.etm.framework.ip.core.utils.AreaUtils;
import com.meession.etm.module.crm.controller.admin.customer.vo.customer.CrmCustomerImportPreviewReqVO;
import com.meession.etm.module.crm.controller.admin.customer.vo.customer.CrmCustomerImportPreviewRespVO;
import com.meession.etm.module.crm.controller.admin.customer.vo.customer.CrmCustomerImportPreviewRespVO.Field;
import com.meession.etm.module.crm.controller.admin.customer.vo.customer.CrmCustomerImportPreviewRespVO.Row;
import com.meession.etm.module.crm.controller.admin.customer.vo.customer.CrmCustomerImportExcelVO;
import com.meession.etm.module.crm.controller.admin.customer.vo.customer.CrmCustomerImportReqVO;
import com.meession.etm.module.crm.controller.admin.customer.vo.customer.CrmCustomerImportRespVO;
import com.meession.etm.module.crm.controller.admin.customer.vo.customer.CrmCustomerSaveReqVO;
import com.meession.etm.module.crm.dal.dataobject.customer.CrmCustomerDO;
import com.meession.etm.module.crm.dal.dataobject.customer.CrmCustomerImportPreviewDO;
import com.meession.etm.module.crm.dal.mysql.customer.CrmCustomerImportPreviewMapper;
import com.meession.etm.module.crm.dal.mysql.customer.CrmCustomerMapper;
import com.meession.etm.module.crm.framework.customer.CrmCustomerImportProperties;
import com.meession.etm.module.system.api.user.AdminUserApi;
import jakarta.annotation.Resource;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static com.meession.etm.framework.common.exception.util.ServiceExceptionUtil.exception;
import static com.meession.etm.module.crm.enums.DictTypeConstants.CRM_CUSTOMER_INDUSTRY;
import static com.meession.etm.module.crm.enums.DictTypeConstants.CRM_CUSTOMER_LEVEL;
import static com.meession.etm.module.crm.enums.DictTypeConstants.CRM_CUSTOMER_SOURCE;
import static com.meession.etm.module.crm.enums.ErrorCodeConstants.CUSTOMER_IMPORT_MAPPING_INVALID;
import static com.meession.etm.module.crm.enums.ErrorCodeConstants.CUSTOMER_IMPORT_PREVIEW_EXPIRED;
import static com.meession.etm.module.crm.enums.ErrorCodeConstants.CUSTOMER_IMPORT_PREVIEW_NO_VALID_ROWS;
import static com.meession.etm.module.crm.enums.ErrorCodeConstants.CUSTOMER_IMPORT_PREVIEW_NOT_EXISTS;
import static com.meession.etm.module.crm.enums.ErrorCodeConstants.CUSTOMER_IMPORT_PREVIEW_STATUS_INVALID;
import static com.meession.etm.module.crm.enums.ErrorCodeConstants.CUSTOMER_IMPORT_ROW_LIMIT;

@Service
public class CrmCustomerImportPreviewServiceImpl implements CrmCustomerImportPreviewService {

    static final int STATUS_PREVIEWED = 10;
    static final int STATUS_CONFIRMED = 20;
    static final int STATUS_EXPIRED = 30;
    static final String ACTION_CREATE = "CREATE";
    static final String ACTION_UPDATE = "UPDATE";
    static final String ACTION_FAILURE = "FAILURE";

    private static final LinkedHashMap<String, String> FIELD_LABELS = new LinkedHashMap<>();
    private static final Map<String, String> STANDARD_HEADERS = new LinkedHashMap<>();

    static {
        FIELD_LABELS.put("name", "客户名称");
        FIELD_LABELS.put("mobile", "手机");
        FIELD_LABELS.put("telephone", "电话");
        FIELD_LABELS.put("qq", "QQ");
        FIELD_LABELS.put("wechat", "微信");
        FIELD_LABELS.put("email", "邮箱");
        FIELD_LABELS.put("areaId", "地区");
        FIELD_LABELS.put("detailAddress", "详细地址");
        FIELD_LABELS.put("industryId", "所属行业");
        FIELD_LABELS.put("level", "客户等级");
        FIELD_LABELS.put("source", "客户来源");
        FIELD_LABELS.put("remark", "备注");
        FIELD_LABELS.forEach((key, label) -> STANDARD_HEADERS.put(label, key));
    }

    @Resource
    private CrmCustomerImportPreviewMapper previewMapper;
    @Resource
    private CrmCustomerMapper customerMapper;
    @Resource
    private CrmCustomerService customerService;
    @Resource
    private CrmCustomerImportProperties properties;
    @Resource
    private AdminUserApi adminUserApi;
    @Resource
    private Validator validator;

    @Override
    public CrmCustomerImportPreviewRespVO createPreview(CrmCustomerImportPreviewReqVO request, Long userId)
            throws IOException {
        if (request.getOwnerUserId() != null) {
            adminUserApi.validateUser(request.getOwnerUserId());
        }
        List<Map<Integer, String>> rawRows = ExcelUtils.readRows(request.getFile());
        if (rawRows.isEmpty()) {
            throw exception(CUSTOMER_IMPORT_MAPPING_INVALID, "Excel 不包含表头");
        }
        Map<Integer, String> headerRow = rawRows.get(0);
        LinkedHashMap<Integer, String> headersByIndex = normalizeHeaders(headerRow);
        Map<String, String> mapping = resolveMapping(headersByIndex.values(), request.getFieldMapping());
        List<Map<Integer, String>> dataRows = rawRows.subList(1, rawRows.size()).stream()
                .filter(this::hasValue).toList();
        if (dataRows.size() > properties.getMaxRows()) {
            throw exception(CUSTOMER_IMPORT_ROW_LIMIT, properties.getMaxRows());
        }

        Set<String> candidateNames = new HashSet<>();
        Set<String> candidateMobiles = new HashSet<>();
        dataRows.forEach(rawRow -> {
            addIfNotBlank(candidateNames, value(rawRow, headersByIndex, mapping, "name"));
            addIfNotBlank(candidateMobiles, value(rawRow, headersByIndex, mapping, "mobile"));
        });
        Map<String, CrmCustomerDO> existingByName = new LinkedHashMap<>();
        Map<String, CrmCustomerDO> existingByMobile = new LinkedHashMap<>();
        customerMapper.selectListByNamesOrMobiles(candidateNames, candidateMobiles).forEach(customer -> {
            existingByName.putIfAbsent(customer.getName(), customer);
            if (StrUtil.isNotBlank(customer.getMobile())) {
                existingByMobile.putIfAbsent(customer.getMobile(), customer);
            }
        });

        List<Row> previewRows = new ArrayList<>();
        Set<String> namesInFile = new HashSet<>();
        Set<String> mobilesInFile = new HashSet<>();
        int rowNumber = 2;
        for (Map<Integer, String> rawRow : dataRows) {
            previewRows.add(buildRow(rowNumber++, rawRow, headersByIndex, mapping,
                    Boolean.TRUE.equals(request.getUpdateSupport()), namesInFile, mobilesInFile,
                    existingByName, existingByMobile, request.getOwnerUserId(), userId));
        }

        int createCount = countAction(previewRows, ACTION_CREATE);
        int updateCount = countAction(previewRows, ACTION_UPDATE);
        int failureCount = countAction(previewRows, ACTION_FAILURE);
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(properties.getPreviewTtlMinutes());
        CrmCustomerImportPreviewDO preview = new CrmCustomerImportPreviewDO()
                .setFileName(request.getFile().getOriginalFilename())
                .setFieldMapping(JsonUtils.toJsonString(mapping))
                .setUpdateSupport(request.getUpdateSupport())
                .setOwnerUserId(request.getOwnerUserId())
                .setStatus(STATUS_PREVIEWED)
                .setTotalCount(previewRows.size())
                .setCreateCount(createCount)
                .setUpdateCount(updateCount)
                .setFailureCount(failureCount)
                .setRowsSnapshot(JsonUtils.toJsonString(previewRows))
                .setExpiresAt(expiresAt);
        previewMapper.insert(preview);
        return toResponse(preview, headersByIndex.values().stream().toList(), mapping, previewRows);
    }

    @Override
    public CrmCustomerImportPreviewRespVO getPreview(Long id, Long userId) {
        CrmCustomerImportPreviewDO preview = validateOwner(previewMapper.selectById(id), userId);
        expireIfNecessary(preview);
        return toResponse(preview, List.copyOf(parseMapping(preview.getFieldMapping()).keySet()),
                parseMapping(preview.getFieldMapping()), parseRows(preview.getRowsSnapshot()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CrmCustomerImportRespVO confirmPreview(Long id, Long userId) {
        CrmCustomerImportPreviewDO preview = validateOwner(previewMapper.selectByIdForUpdate(id), userId);
        if (Objects.equals(preview.getStatus(), STATUS_CONFIRMED)) {
            return JsonUtils.parseObject(preview.getResultSnapshot(), CrmCustomerImportRespVO.class);
        }
        if (!Objects.equals(preview.getStatus(), STATUS_PREVIEWED)) {
            throw exception(CUSTOMER_IMPORT_PREVIEW_STATUS_INVALID);
        }
        if (preview.getExpiresAt().isBefore(LocalDateTime.now())) {
            previewMapper.updateById(preview.setStatus(STATUS_EXPIRED));
            throw exception(CUSTOMER_IMPORT_PREVIEW_EXPIRED);
        }
        List<Row> rows = parseRows(preview.getRowsSnapshot());
        List<CrmCustomerImportExcelVO> customers = rows.stream()
                .filter(row -> ACTION_CREATE.equals(row.getAction()) || ACTION_UPDATE.equals(row.getAction()))
                .map(Row::getCustomer).toList();
        if (customers.isEmpty()) {
            throw exception(CUSTOMER_IMPORT_PREVIEW_NO_VALID_ROWS);
        }
        CrmCustomerImportReqVO importRequest = CrmCustomerImportReqVO.builder()
                .updateSupport(preview.getUpdateSupport()).ownerUserId(preview.getOwnerUserId()).build();
        CrmCustomerImportRespVO result = customerService.importCustomerList(customers, importRequest);
        previewMapper.updateById(preview.setStatus(STATUS_CONFIRMED)
                .setConfirmedAt(LocalDateTime.now()).setResultSnapshot(JsonUtils.toJsonString(result)));
        return result;
    }

    private Row buildRow(int rowNumber, Map<Integer, String> rawRow, Map<Integer, String> headersByIndex,
                         Map<String, String> mapping, boolean updateSupport, Set<String> namesInFile,
                         Set<String> mobilesInFile, Map<String, CrmCustomerDO> existingByName,
                         Map<String, CrmCustomerDO> existingByMobile, Long ownerUserId, Long currentUserId) {
        List<String> errors = new ArrayList<>();
        CrmCustomerImportExcelVO customer = new CrmCustomerImportExcelVO();
        customer.setName(value(rawRow, headersByIndex, mapping, "name"));
        customer.setMobile(value(rawRow, headersByIndex, mapping, "mobile"));
        customer.setTelephone(value(rawRow, headersByIndex, mapping, "telephone"));
        customer.setQq(value(rawRow, headersByIndex, mapping, "qq"));
        customer.setWechat(value(rawRow, headersByIndex, mapping, "wechat"));
        customer.setEmail(value(rawRow, headersByIndex, mapping, "email"));
        customer.setDetailAddress(value(rawRow, headersByIndex, mapping, "detailAddress"));
        customer.setRemark(value(rawRow, headersByIndex, mapping, "remark"));
        customer.setIndustryId(parseDict(value(rawRow, headersByIndex, mapping, "industryId"),
                CRM_CUSTOMER_INDUSTRY, "所属行业", errors));
        customer.setLevel(parseDict(value(rawRow, headersByIndex, mapping, "level"),
                CRM_CUSTOMER_LEVEL, "客户等级", errors));
        customer.setSource(parseDict(value(rawRow, headersByIndex, mapping, "source"),
                CRM_CUSTOMER_SOURCE, "客户来源", errors));
        customer.setAreaId(parseArea(value(rawRow, headersByIndex, mapping, "areaId"), errors));

        CrmCustomerSaveReqVO validation = BeanUtils.toBean(customer, CrmCustomerSaveReqVO.class);
        validation.setOwnerUserId(ownerUserId == null ? currentUserId : ownerUserId);
        validator.validate(validation).stream().map(ConstraintViolation::getMessage).forEach(errors::add);
        if (StrUtil.isNotBlank(customer.getName()) && !namesInFile.add(customer.getName())) {
            errors.add("文件内客户名称重复");
        }
        if (StrUtil.isNotBlank(customer.getMobile()) && !mobilesInFile.add(customer.getMobile())) {
            errors.add("文件内客户手机号重复");
        }
        CrmCustomerDO existing = existingByName.get(customer.getName());
        CrmCustomerDO mobileOwner = existingByMobile.get(customer.getMobile());
        if (mobileOwner != null && (existing == null || !Objects.equals(existing.getId(), mobileOwner.getId()))) {
            errors.add("手机号已被其他客户使用");
        }
        String action = errors.isEmpty() ? existing == null ? ACTION_CREATE
                : updateSupport ? ACTION_UPDATE : ACTION_FAILURE : ACTION_FAILURE;
        if (existing != null && !updateSupport && errors.isEmpty()) {
            errors.add("客户名称已存在且未允许更新");
        }
        return Row.builder().rowNumber(rowNumber).action(action)
                .existingCustomerId(existing == null ? null : existing.getId())
                .errors(errors).customer(customer).build();
    }

    private LinkedHashMap<Integer, String> normalizeHeaders(Map<Integer, String> raw) {
        LinkedHashMap<Integer, String> result = new LinkedHashMap<>();
        Set<String> unique = new HashSet<>();
        raw.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry -> {
            String header = clean(entry.getValue());
            if (header == null) {
                return;
            }
            if (!unique.add(header)) {
                throw exception(CUSTOMER_IMPORT_MAPPING_INVALID, "表头重复：" + header);
            }
            result.put(entry.getKey(), header);
        });
        if (result.isEmpty()) {
            throw exception(CUSTOMER_IMPORT_MAPPING_INVALID, "Excel 表头为空");
        }
        return result;
    }

    private Map<String, String> resolveMapping(Iterable<String> headers, String requestedJson) {
        LinkedHashMap<String, String> result = new LinkedHashMap<>();
        Set<String> actualHeaders = new HashSet<>();
        headers.forEach(header -> {
            actualHeaders.add(header);
            result.put(header, STANDARD_HEADERS.getOrDefault(header, ""));
        });
        if (StrUtil.isNotBlank(requestedJson)) {
            Map<String, String> requested;
            try {
                requested = JsonUtils.parseObject(requestedJson, new TypeReference<LinkedHashMap<String, String>>() { });
            } catch (RuntimeException ex) {
                throw exception(CUSTOMER_IMPORT_MAPPING_INVALID, "映射不是合法 JSON");
            }
            requested.forEach((header, field) -> {
                if (!actualHeaders.contains(header)) {
                    throw exception(CUSTOMER_IMPORT_MAPPING_INVALID, "映射表头不存在：" + header);
                }
                if (StrUtil.isBlank(field)) {
                    result.put(header, "");
                } else if (!FIELD_LABELS.containsKey(field)) {
                    throw exception(CUSTOMER_IMPORT_MAPPING_INVALID, "未知系统字段：" + field);
                } else {
                    result.put(header, field);
                }
            });
        }
        Set<String> targets = new HashSet<>();
        for (String target : result.values()) {
            if (StrUtil.isNotBlank(target) && !targets.add(target)) {
                throw exception(CUSTOMER_IMPORT_MAPPING_INVALID, "系统字段不能重复映射：" + FIELD_LABELS.get(target));
            }
        }
        return result;
    }

    private String value(Map<Integer, String> row, Map<Integer, String> headersByIndex,
                         Map<String, String> mapping, String field) {
        for (Map.Entry<Integer, String> header : headersByIndex.entrySet()) {
            if (Objects.equals(mapping.get(header.getValue()), field)) {
                return clean(row.get(header.getKey()));
            }
        }
        return null;
    }

    private Integer parseDict(String raw, String type, String label, List<String> errors) {
        if (raw == null) {
            return null;
        }
        String value = raw.matches("-?\\d+") ? raw : DictFrameworkUtils.parseDictDataValue(type, raw);
        if (value == null || DictFrameworkUtils.parseDictDataLabel(type, value) == null) {
            errors.add(label + "无效：" + raw);
            return null;
        }
        try {
            return Integer.valueOf(value);
        } catch (NumberFormatException ex) {
            errors.add(label + "无效：" + raw);
            return null;
        }
    }

    private Integer parseArea(String raw, List<String> errors) {
        if (raw == null) {
            return null;
        }
        Area area;
        try {
            area = raw.matches("\\d+") ? AreaUtils.getArea(Integer.valueOf(raw)) : AreaUtils.parseArea(raw);
        } catch (RuntimeException ex) {
            area = null;
        }
        if (area == null) {
            errors.add("地区无效：" + raw);
            return null;
        }
        return area.getId();
    }

    private CrmCustomerImportPreviewDO validateOwner(CrmCustomerImportPreviewDO preview, Long userId) {
        if (preview == null || !Objects.equals(preview.getCreator(), String.valueOf(userId))) {
            throw exception(CUSTOMER_IMPORT_PREVIEW_NOT_EXISTS);
        }
        return preview;
    }

    private void expireIfNecessary(CrmCustomerImportPreviewDO preview) {
        if (Objects.equals(preview.getStatus(), STATUS_PREVIEWED)
                && preview.getExpiresAt().isBefore(LocalDateTime.now())) {
            previewMapper.updateById(preview.setStatus(STATUS_EXPIRED));
        }
    }

    private CrmCustomerImportPreviewRespVO toResponse(CrmCustomerImportPreviewDO preview, List<String> headers,
                                                       Map<String, String> mapping, List<Row> rows) {
        List<Field> fields = FIELD_LABELS.entrySet().stream().map(entry -> Field.builder()
                .key(entry.getKey()).label(entry.getValue()).required("name".equals(entry.getKey())).build()).toList();
        return CrmCustomerImportPreviewRespVO.builder().id(preview.getId()).fileName(preview.getFileName())
                .status(preview.getStatus()).expiresAt(preview.getExpiresAt()).totalCount(preview.getTotalCount())
                .createCount(preview.getCreateCount()).updateCount(preview.getUpdateCount())
                .failureCount(preview.getFailureCount()).fieldMapping(mapping).headers(headers)
                .fields(fields).rows(rows).build();
    }

    private Map<String, String> parseMapping(String json) {
        return JsonUtils.parseObject(json, new TypeReference<LinkedHashMap<String, String>>() { });
    }

    private List<Row> parseRows(String json) {
        return JsonUtils.parseObject(json, new TypeReference<List<Row>>() { });
    }

    private int countAction(List<Row> rows, String action) {
        return (int) rows.stream().filter(row -> action.equals(row.getAction())).count();
    }

    private boolean hasValue(Map<Integer, String> row) {
        return row != null && row.values().stream().anyMatch(StrUtil::isNotBlank);
    }

    private static void addIfNotBlank(Set<String> values, String value) {
        if (StrUtil.isNotBlank(value)) {
            values.add(value);
        }
    }

    private static String clean(String value) {
        String trimmed = StrUtil.trim(value);
        return StrUtil.isBlank(trimmed) ? null : trimmed;
    }
}
