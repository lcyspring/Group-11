package com.meession.etm.module.crm.controller.app.marketing;

import com.meession.etm.module.crm.service.marketing.CrmMarketingOutreachService;
import com.meession.etm.framework.tenant.core.aop.TenantIgnore;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

class CrmMarketingTrackingControllerTest {

    @Test
    void endpointExplicitlyBypassesTenantHeaderRequirement() throws NoSuchMethodException {
        var method = AppCrmMarketingTrackingController.class.getMethod("trackOpen", String.class);
        assertTrue(method.isAnnotationPresent(TenantIgnore.class));
        var clickMethod = AppCrmMarketingTrackingController.class.getMethod("trackClick", String.class);
        assertTrue(clickMethod.isAnnotationPresent(TenantIgnore.class));
    }

    @Test
    void endpointAlwaysReturnsTheSameNoStoreGifWithoutBusinessPayload() {
        CrmMarketingOutreachService service = mock(CrmMarketingOutreachService.class);
        AppCrmMarketingTrackingController controller = new AppCrmMarketingTrackingController();
        ReflectionTestUtils.setField(controller, "outreachService", service);

        var first = controller.trackOpen("0123456789abcdef0123456789abcdef");
        var invalid = controller.trackOpen("invalid");

        verify(service).recordMailOpen("0123456789abcdef0123456789abcdef");
        verify(service).recordMailOpen("invalid");
        assertEquals(MediaType.IMAGE_GIF, first.getHeaders().getContentType());
        assertEquals("no-store", first.getHeaders().getCacheControl());
        assertNotNull(first.getBody());
        assertArrayEquals(first.getBody(), invalid.getBody());
    }

    @Test
    void clickRedirectUsesOnlyTheServerResolvedTargetAndInvalidTokenIsGeneric404() {
        CrmMarketingOutreachService service = mock(CrmMarketingOutreachService.class);
        when(service.recordLinkClick("valid-token")).thenReturn(Optional.of("https://example.com/offer"));
        when(service.recordLinkClick("invalid-token")).thenReturn(Optional.empty());
        AppCrmMarketingTrackingController controller = new AppCrmMarketingTrackingController();
        ReflectionTestUtils.setField(controller, "outreachService", service);

        var valid = controller.trackClick("valid-token");
        var invalid = controller.trackClick("invalid-token");

        assertEquals(HttpStatus.FOUND, valid.getStatusCode());
        assertEquals("https://example.com/offer", valid.getHeaders().getLocation().toString());
        assertEquals("no-store", valid.getHeaders().getCacheControl());
        assertEquals(HttpStatus.NOT_FOUND, invalid.getStatusCode());
        assertEquals(null, invalid.getHeaders().getLocation());
    }
}
