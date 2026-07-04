package com.erp.formsmanagement.clientportal.service;

import com.erp.exception.EntityNotFoundException;
import com.erp.formsmanagement.domain.entity.master.PartyEntity;
import com.erp.formsmanagement.domain.repository.master.PartyRepository;
import com.erp.usermanagement.model.entity.UserEntity;
import com.erp.usermanagement.repository.UserRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Resolves the {@link UserEntity} for the currently authenticated client user and the party they
 * are currently acting as. A standalone client is always their single party; a group login can act
 * as any of the group's member companies, chosen per-request via the {@code X-Party-Id} header.
 */
@Component
@RequiredArgsConstructor
public class CurrentClientProvider {

  /** Header carrying the party id the client is currently acting as (group logins). */
  public static final String PARTY_HEADER = "X-Party-Id";

  private final UserRepository userRepository;
  private final PartyRepository partyRepository;

  public UserEntity getCurrentUser() {
    String username = SecurityContextHolder.getContext().getAuthentication().getName();
    return userRepository
        .findByUsername(username)
        .orElseThrow(() -> new EntityNotFoundException("User not found: " + username));
  }

  /** The companies/parties the current user may act as (group members, or a single party). */
  public List<PartyEntity> accessibleParties() {
    UserEntity user = getCurrentUser();
    if (user.getGroupId() != null) {
      return partyRepository.findByGroupId(user.getGroupId());
    }
    if (user.getPartyId() != null) {
      return partyRepository.findById(user.getPartyId()).map(List::of).orElseGet(List::of);
    }
    return List.of();
  }

  /**
   * The id of the party the current request is acting on. Uses the {@code X-Party-Id} header when
   * present, after verifying it is one of the user's accessible parties; otherwise defaults to the
   * user's single/first accessible party.
   */
  public Long getActivePartyId() {
    List<Long> accessible = accessibleParties().stream().map(PartyEntity::getId).toList();
    if (accessible.isEmpty()) {
      throw new EntityNotFoundException("No company is linked to this account");
    }

    Long requested = readRequestedPartyId();
    if (requested != null) {
      if (!accessible.contains(requested)) {
        throw new AccessDeniedException(
            "You do not have access to company with id: " + requested);
      }
      return requested;
    }
    return accessible.get(0);
  }

  private Long readRequestedPartyId() {
    ServletRequestAttributes attrs =
        (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
    if (attrs == null) {
      return null;
    }
    String header = attrs.getRequest().getHeader(PARTY_HEADER);
    if (header == null || header.isBlank()) {
      return null;
    }
    try {
      return Long.parseLong(header.trim());
    } catch (NumberFormatException e) {
      return null;
    }
  }
}
