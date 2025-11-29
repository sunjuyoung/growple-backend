package com.grow.payment.domain;

import com.grow.payment.domain.enums.WalletEventMessageType;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class WalletEventMessage {
    private WalletEventMessageType type;
    private Map<String, Object> payload;
    private Map<String, Object> metadata;

    public WalletEventMessage(WalletEventMessageType type, Map<String, Object> payload, Map<String, Object> metadata) {
        this.type = type;
        this.payload = payload != null ? new HashMap<>(payload) : new HashMap<>();
        this.metadata = metadata != null ? new HashMap<>(metadata) : new HashMap<>();
    }

    public WalletEventMessage(WalletEventMessageType type) {
        this(type, new HashMap<>(), new HashMap<>());
    }

    public WalletEventMessageType getType() {
        return type;
    }

    public Map<String, Object> getPayload() {
        return Collections.unmodifiableMap(payload);
    }

    public Map<String, Object> getMetadata() {
        return Collections.unmodifiableMap(metadata);
    }

    public String orderId() {
        return (String) payload.get("orderId");
    }
}
