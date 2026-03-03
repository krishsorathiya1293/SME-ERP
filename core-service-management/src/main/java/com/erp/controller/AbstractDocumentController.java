package com.erp.controller;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

public abstract class AbstractDocumentController {

  protected ResponseEntity<Resource> buildPdfResponse(byte[] pdfBytes, String filename) {
    ByteArrayResource pdf = new ByteArrayResource(pdfBytes);
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
        .contentType(MediaType.APPLICATION_PDF)
        .contentLength(pdf.contentLength())
        .body(pdf);
  }

  protected ResponseEntity<Resource> buildPngResponse(byte[] pngBytes, String filename) {
    ByteArrayResource png = new ByteArrayResource(pngBytes);
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
        .contentType(MediaType.IMAGE_PNG)
        .contentLength(png.contentLength())
        .body(png);
  }
}
