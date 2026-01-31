package com.erp.usermanagement.repository;

import com.erp.repository.CoreRepository;
import com.erp.usermanagement.model.entity.UserEntity;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

/** Repository interface for UserEntity database operations. */
@Repository
public interface UserRepository extends CoreRepository<UserEntity, Long> {

  /**
   * Find user by email address.
   *
   * @param userEmail The email to search for
   * @return Optional containing user if found
   */
  Optional<UserEntity> findByUserEmail(String userEmail);

  /**
   * Check if user exists with given email.
   *
   * @param userEmail The email to check
   * @return true if user exists, false otherwise
   */
  boolean existsByUserEmail(String userEmail);

  /**
   * Find users by email containing search string (case-insensitive) with pagination.
   *
   * @param email The email search string
   * @param pageable Pagination information
   * @return Page of users matching the search criteria
   */
  Page<UserEntity> findByUserEmailContainingIgnoreCase(String email, Pageable pageable);
}
