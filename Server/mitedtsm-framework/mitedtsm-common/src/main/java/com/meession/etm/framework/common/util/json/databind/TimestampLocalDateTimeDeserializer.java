package com.meession.etm.framework.common.util.json.databind;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * 基于时间戳的 LocalDateTime 反序列化器
 *
 * @author 老五
 */
public class TimestampLocalDateTimeDeserializer extends JsonDeserializer<LocalDateTime> {

    public static final TimestampLocalDateTimeDeserializer INSTANCE = new TimestampLocalDateTimeDeserializer();

    @Override
    public LocalDateTime deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        long timestamp;
        if (p.currentToken() == JsonToken.VALUE_NUMBER_INT) {
            timestamp = p.getLongValue();
        } else if (p.currentToken() == JsonToken.VALUE_STRING) {
            String value = p.getText().trim();
            try {
                // Element Plus value-format="x" sends epoch milliseconds as a JSON string.
                // Keep that public contract, but never coerce an arbitrary date string to epoch zero.
                timestamp = Long.parseLong(value);
            } catch (NumberFormatException ex) {
                throw InvalidFormatException.from(p,
                        "expected epoch milliseconds as a number or numeric string", value, LocalDateTime.class);
            }
        } else {
            throw MismatchedInputException.from(p, LocalDateTime.class,
                    "expected epoch milliseconds as a number or numeric string");
        }
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault());
    }

}
