package com.erp.formsmanagement.service.attendance;

import java.time.Duration;
import java.time.Instant;
import org.springframework.stereotype.Component;

/**
 * When the factory PC last called a sync endpoint. HRMS checks in every 30 seconds even when
 * nobody punched, so this tells "PC is on and connected" apart from "no new punches overnight".
 * In memory only: after a server restart it's unknown until the next check-in.
 */
@Component
public class AttendanceAgentHeartbeat {

  private volatile Instant lastSeen;

  public void touch() {
    lastSeen = Instant.now();
  }

  /** Seconds since the last check-in, or null if none since the server started. */
  public Long secondsAgo() {
    Instant seen = lastSeen;
    return seen == null ? null : Duration.between(seen, Instant.now()).getSeconds();
  }
}
