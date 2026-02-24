package com.erp.event;

public record FormChangedEvent(String formType, Long entityId, String action) {
}
