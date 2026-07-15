package com.meession.etm.framework.common.util.json.databind;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TimestampLocalDateTimeDeserializerTest {

    private static final long TIMESTAMP = 1_784_092_800_000L;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        SimpleModule module = new SimpleModule();
        module.addDeserializer(LocalDateTime.class, TimestampLocalDateTimeDeserializer.INSTANCE);
        objectMapper = new ObjectMapper().registerModule(module);
    }

    @Test
    void shouldDeserializeNumericTimestamp() throws Exception {
        assertThat(objectMapper.readValue(String.valueOf(TIMESTAMP), LocalDateTime.class))
                .isEqualTo(expectedDateTime());
    }

    @Test
    void shouldDeserializeNumericStringUsedByWebDatePicker() throws Exception {
        assertThat(objectMapper.readValue('"' + String.valueOf(TIMESTAMP) + '"', LocalDateTime.class))
                .isEqualTo(expectedDateTime());
    }

    @Test
    void shouldRejectFormattedDateInsteadOfSilentlyUsingEpochZero() {
        assertThatThrownBy(() -> objectMapper.readValue("\"2026-07-15 14:00:00\"", LocalDateTime.class))
                .hasMessageContaining("expected epoch milliseconds");
    }

    @Test
    void shouldRejectUnsupportedJsonToken() {
        assertThatThrownBy(() -> objectMapper.readValue("true", LocalDateTime.class))
                .hasMessageContaining("expected epoch milliseconds");
    }

    private static LocalDateTime expectedDateTime() {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(TIMESTAMP), ZoneId.systemDefault());
    }
}
