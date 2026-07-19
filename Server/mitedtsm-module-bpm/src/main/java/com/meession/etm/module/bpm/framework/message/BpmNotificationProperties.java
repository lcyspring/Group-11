package com.meession.etm.module.bpm.framework.message;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

/** BPM outbound notification policy. */
@Component
@Data
@Validated
@ConfigurationProperties(prefix = "mitedtsm.bpm.notification")
public class BpmNotificationProperties {

    /** Send the built-in BPM SMS notifications through the System SMS provider. */
    private boolean smsEnabled;

    /** Propagate an outbound notification error to the approval transaction. */
    private boolean failFast;

}
