package com.meession.etm.framework.security.config;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SecurityPropertiesTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void shouldUseProductionSafeBcryptDefault() {
        SecurityProperties properties = new SecurityProperties();

        assertEquals(10, properties.getPasswordEncoderLength());
        assertTrue(validator.validate(properties).isEmpty());
    }

    @Test
    void shouldRejectWeakBcryptStrength() {
        SecurityProperties properties = new SecurityProperties();
        properties.setPasswordEncoderLength(4);

        assertTrue(validator.validate(properties).stream()
                .anyMatch(violation -> "passwordEncoderLength".equals(violation.getPropertyPath().toString())));
    }

    @Test
    void shouldRequireExplicitSecretWhenMockLoginIsEnabled() {
        SecurityProperties properties = new SecurityProperties();
        properties.setMockEnable(true);

        assertTrue(validator.validate(properties).stream()
                .anyMatch(violation -> "mockSecretValid".equals(violation.getPropertyPath().toString())));
    }
}
