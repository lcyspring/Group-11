package com.meession.etm.module.pay.controller.admin.notify;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class PayNotifyControllerTest {

    @Test
    void summarizePayloadDoesNotExposeCallbackContent() {
        String signature = "provider-signature-must-not-be-logged";
        String body = "{\"payer\":\"sensitive-user\",\"sign\":\"" + signature + "\"}";

        String summary = PayNotifyController.summarizePayload(
                Map.of("sign", signature, "transaction_id", "secret-transaction"), body);

        assertThat(summary).isEqualTo("paramsCount(2), bodyLength(" + body.length() + ")");
        assertThat(summary).doesNotContain(signature, "sensitive-user", "secret-transaction");
    }

    @Test
    void summarizePayloadHandlesMissingPayload() {
        assertThat(PayNotifyController.summarizePayload(null, null))
                .isEqualTo("paramsCount(0), bodyLength(0)");
    }
}
