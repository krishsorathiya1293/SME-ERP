package com.erp.formsmanagement.domain.entity.order;

/**
 * Which market rate a job work is priced against.
 *
 * <p>The distinction is where the number lives, not what it means: {@link #FIXED} takes the single
 * house rate held in app settings, so it is the same on every chitthi and changes everywhere at
 * once; {@link #ROJNU} ("daily") is negotiated per chitthi and therefore stored on the row.
 */
public enum BajaarType {
  FIXED,
  ROJNU
}
