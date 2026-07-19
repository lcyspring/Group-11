package com.meession.etm.module.infra.api.file;

import com.meession.etm.framework.common.util.object.BeanUtils;
import com.meession.etm.module.infra.api.file.dto.FileRespDTO;
import com.meession.etm.module.infra.service.file.FileService;
import jakarta.annotation.Resource;
import lombok.SneakyThrows;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

/**
 * 文件 API 实现类
 *
 * @author 密讯
 */
@Service
@Validated
public class FileApiImpl implements FileApi {

    @Resource
    private FileService fileService;

    @Override
    public String createFile(byte[] content, String name, String directory, String type) {
        return fileService.createFile(content, name, directory, type);
    }

    @Override
    public String presignGetUrl(String url, Integer expirationSeconds) {
        return fileService.presignGetUrl(url, expirationSeconds);
    }

    @Override
    public FileRespDTO getFileByUrl(String url) {
        return BeanUtils.toBean(fileService.getFileByUrl(url), FileRespDTO.class);
    }

    @Override
    @SneakyThrows
    public byte[] getFileContent(Long configId, String path) {
        return fileService.getFileContent(configId, path);
    }

    @Override
    @SneakyThrows
    public void deleteFileByUrl(String url) {
        fileService.deleteFile(fileService.getFileByUrl(url).getId());
    }

}
