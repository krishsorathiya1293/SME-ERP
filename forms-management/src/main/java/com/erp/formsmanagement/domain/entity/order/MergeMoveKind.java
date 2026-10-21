package com.erp.formsmanagement.domain.entity.order;

/** Which kind of record an {@link OrderItemMergeMoveEntity} moved onto a merged line. */
public enum MergeMoveKind {
  /** An {@code order_dispatch} row. */
  DISPATCH,
  /** A {@code job_works} row whose primary order line moved. */
  JOB_WORK,
  /** A {@code job_work_order_items} row: one line's share of a chitthi covering several. */
  ALLOCATION
}
