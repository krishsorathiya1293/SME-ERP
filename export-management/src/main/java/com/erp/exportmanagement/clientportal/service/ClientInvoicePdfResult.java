package com.erp.exportmanagement.clientportal.service;

/** A generated invoice PDF for a client, along with the filename to expose it under. */
public record ClientInvoicePdfResult(byte[] bytes, String filename) {}
