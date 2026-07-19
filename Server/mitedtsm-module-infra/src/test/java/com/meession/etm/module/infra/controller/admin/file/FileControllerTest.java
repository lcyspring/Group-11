package com.meession.etm.module.infra.controller.admin.file;

import com.meession.etm.module.infra.framework.file.config.FileSecurityProperties;
import com.meession.etm.module.infra.service.file.FileService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class FileControllerTest {

    @Test
    void protectedPathMatchingNormalizesSegmentsWithoutOvermatchingSiblingPrefix() {
        FileSecurityProperties properties = new FileSecurityProperties();
        properties.setProtectedPathPrefixes(List.of("crm-protected/"));

        assertTrue(properties.isProtectedPath("public/../crm-protected/contract/a.pdf"));
        assertTrue(properties.isProtectedPath("/crm-protected\\contract\\a.pdf"));
        assertFalse(properties.isProtectedPath("crm-protected-public/a.pdf"));
    }

    @Test
    void publicRouteReturnsNotFoundBeforeReadingProtectedFile() throws Exception {
        FileController controller = new FileController();
        FileSecurityProperties properties = new FileSecurityProperties();
        properties.setProtectedPathPrefixes(List.of("crm-protected/"));
        ReflectionTestUtils.setField(controller, "fileSecurityProperties", properties);
        FileService fileService = mock(FileService.class);
        ReflectionTestUtils.setField(controller, "fileService", fileService);

        for (String path : List.of("crm-protected/contract/a.pdf", "/crm-protected/contract/a.pdf",
                "public/../crm-protected/contract/a.pdf", "crm-protected\\contract\\a.pdf")) {
            MockHttpServletRequest request = new MockHttpServletRequest();
            request.setRequestURI("/admin-api/infra/file/4/get/" + path);
            MockHttpServletResponse response = new MockHttpServletResponse();

            controller.getFileContent(request, response, 4L);

            assertEquals(404, response.getStatus(), path);
        }
        verifyNoInteractions(fileService);
    }
}
