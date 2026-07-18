package com.meession.etm.module.crm.controller.app.marketing;

import com.meession.etm.module.crm.service.marketing.CrmMarketingOutreachService;
import com.meession.etm.framework.tenant.core.aop.TenantIgnore;
import jakarta.annotation.Resource;
import jakarta.annotation.security.PermitAll;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Base64;
import java.net.URI;

/** 邮件客户端匿名加载的 1×1 像素。无效令牌与有效令牌返回完全相同的响应。 */
@RestController
@RequestMapping("/crm/marketing")
public class AppCrmMarketingTrackingController {

    private static final byte[] TRANSPARENT_GIF = Base64.getDecoder().decode(
            "R0lGODlhAQABAIAAAAAAAP///ywAAAAAAQABAAACAUwAOw==");

    @Resource
    private CrmMarketingOutreachService outreachService;

    @GetMapping(value = "/open/{token}.gif", produces = MediaType.IMAGE_GIF_VALUE)
    @PermitAll
    @TenantIgnore
    public ResponseEntity<byte[]> trackOpen(@PathVariable String token) {
        outreachService.recordMailOpen(token);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .contentType(MediaType.IMAGE_GIF)
                .contentLength(TRANSPARENT_GIF.length)
                .body(TRANSPARENT_GIF);
    }

    @GetMapping("/click/{token}")
    @PermitAll
    @TenantIgnore
    public ResponseEntity<Void> trackClick(@PathVariable String token) {
        return outreachService.recordLinkClick(token)
                .map(target -> ResponseEntity.status(302).location(URI.create(target))
                        .cacheControl(CacheControl.noStore()).<Void>build())
                .orElseGet(() -> ResponseEntity.notFound().cacheControl(CacheControl.noStore()).build());
    }
}
