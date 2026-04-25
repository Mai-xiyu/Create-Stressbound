package org.xiyu.create_stressbound.content.link;

public enum ReceiverStatus {
    IDLE("create_stressbound.receiver_status.idle"),
    ACTIVE("create_stressbound.receiver_status.active"),
    OVERLOADED("create_stressbound.receiver_status.overloaded"),
    TRANSMITTER_UNLOADED("create_stressbound.receiver_status.transmitter_unloaded"),
    RECEIVER_UNLOADED("create_stressbound.receiver_status.receiver_unloaded"),
    RECEIVER_DISABLED("create_stressbound.receiver_status.receiver_disabled"),
    TRANSMITTER_DISABLED("create_stressbound.receiver_status.transmitter_disabled"),
    INVALID_TRANSMITTER("create_stressbound.receiver_status.invalid_transmitter"),
    INVALID_RECEIVER("create_stressbound.receiver_status.invalid_receiver"),
    REMOTE_LOOP("create_stressbound.receiver_status.remote_loop"),
    UNSUPPORTED_ANCHOR("create_stressbound.receiver_status.unsupported_anchor");

    private final String translationKey;

    ReceiverStatus(String translationKey) {
        this.translationKey = translationKey;
    }

    public String translationKey() {
        return translationKey;
    }
}
