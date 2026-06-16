package com.erp.formsmanagement.controller.client;

import com.erp.api.clientmanagement.model.ClientInventory;
import com.erp.api.clientmanagement.model.ClientInventoryImportResult;
import com.erp.api.clientmanagement.model.NewClientInventory;
import com.erp.formsmanagement.service.client.ClientInventoryImportService;
import com.erp.formsmanagement.service.client.ClientInventoryService;
import java.util.List;
import java.util.Optional;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class ClientInventoryController {

  private final ClientInventoryService clientInventoryService;
  private final ClientInventoryImportService clientInventoryImportService;

  public ClientInventoryController(
      ClientInventoryService clientInventoryService,
      ClientInventoryImportService clientInventoryImportService) {
    this.clientInventoryService = clientInventoryService;
    this.clientInventoryImportService = clientInventoryImportService;
  }

  @GetMapping("/api/v1/clients/{clientId}/inventory")
  public ResponseEntity<List<ClientInventory>> getInventoryByClient(
      @PathVariable Long clientId,
      @RequestParam(required = false) Long sizeId,
      @RequestParam(required = false) String search) {
    return ResponseEntity.ok(
        clientInventoryService.getAll(clientId, Optional.ofNullable(sizeId), Optional.ofNullable(search)));
  }

  @PostMapping("/api/v1/clients/{clientId}/inventory")
  public ResponseEntity<ClientInventory> createClientInventory(
      @PathVariable Long clientId, @RequestBody NewClientInventory newClientInventory) {
    return ResponseEntity.status(201).body(
        clientInventoryService.create(clientId, newClientInventory));
  }

  @GetMapping("/api/v1/clients/{clientId}/inventory/{inventoryId}")
  public ResponseEntity<ClientInventory> getClientInventoryById(
      @PathVariable Long clientId, @PathVariable Long inventoryId) {
    return ResponseEntity.ok(clientInventoryService.getById(clientId, inventoryId));
  }

  @PutMapping("/api/v1/clients/{clientId}/inventory/{inventoryId}")
  public ResponseEntity<ClientInventory> updateClientInventory(
      @PathVariable Long clientId,
      @PathVariable Long inventoryId,
      @RequestBody NewClientInventory newClientInventory) {
    return ResponseEntity.ok(
        clientInventoryService.update(clientId, inventoryId, newClientInventory));
  }

  @DeleteMapping("/api/v1/clients/{clientId}/inventory/{inventoryId}")
  public ResponseEntity<Void> deleteClientInventory(
      @PathVariable Long clientId, @PathVariable Long inventoryId) {
    clientInventoryService.deleteById(clientId, inventoryId);
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/api/v1/clients/{clientId}/inventory/import")
  public ResponseEntity<ClientInventoryImportResult> importClientInventory(
      @PathVariable Long clientId, @RequestParam("file") MultipartFile file) {
    return ResponseEntity.ok(clientInventoryImportService.importClientInventory(clientId, file));
  }
}
