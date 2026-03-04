package com.erp.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Configuration
public class OpenApiConfig {

  @RestController
  @RequestMapping("/doc/v3/user-api")
  public class UserAuthAPIDocumentationController {
    @GetMapping
    public String getOpenApiYaml() throws IOException {
      String data = "";
      ClassPathResource cpr = new ClassPathResource("user-management.yaml");
      byte[] bdata = FileCopyUtils.copyToByteArray(cpr.getInputStream());
      data = new String(bdata, StandardCharsets.UTF_8);
      return data;
    }
  }

  @RestController
  @RequestMapping("/doc/v3/master")
  public class MasterDocumentationController {
    @GetMapping
    public String getOpenApiYaml() throws IOException {
      String data = "";
      ClassPathResource cpr = new ClassPathResource("master.yaml");
      byte[] bdata = FileCopyUtils.copyToByteArray(cpr.getInputStream());
      data = new String(bdata, StandardCharsets.UTF_8);
      return data;
    }
  }

  @RestController
  @RequestMapping("/doc/v3/invoice")
  public class InvoiceDocumentationController {
    @GetMapping
    public String getOpenApiYaml() throws IOException {
      String data = "";
      ClassPathResource cpr = new ClassPathResource("invoice-management.yaml");
      byte[] bdata = FileCopyUtils.copyToByteArray(cpr.getInputStream());
      data = new String(bdata, StandardCharsets.UTF_8);
      return data;
    }
  }

  @RestController
  @RequestMapping("/doc/v3/export")
  public class ExportDocumentationController {
    @GetMapping
    public String getOpenApiYaml() throws IOException {
      String data = "";
      ClassPathResource cpr = new ClassPathResource("export-management.yaml");
      byte[] bdata = FileCopyUtils.copyToByteArray(cpr.getInputStream());
      data = new String(bdata, StandardCharsets.UTF_8);
      return data;
    }
  }

  @RestController
  @RequestMapping("/doc/v3/item")
  public class ItemDocumentationController {
    @GetMapping
    public String getOpenApiYaml() throws IOException {
      String data = "";
      ClassPathResource cpr = new ClassPathResource("item-management.yaml");
      byte[] bdata = FileCopyUtils.copyToByteArray(cpr.getInputStream());
      data = new String(bdata, StandardCharsets.UTF_8);
      return data;
    }
  }

  @RestController
  @RequestMapping("/doc/v3/client")
  public class ClientDocumentationController {
    @GetMapping
    public String getOpenApiYaml() throws IOException {
      String data = "";
      ClassPathResource cpr = new ClassPathResource("client-management.yaml");
      byte[] bdata = FileCopyUtils.copyToByteArray(cpr.getInputStream());
      data = new String(bdata, StandardCharsets.UTF_8);
      return data;
    }
  }

  @RestController
  @RequestMapping("/doc/v3/orders")
  public class OrderDocumentationController {
    @GetMapping
    public String getOpenApiYaml() throws IOException {
      String data = "";
      ClassPathResource cpr = new ClassPathResource("order-management.yaml");
      byte[] bdata = FileCopyUtils.copyToByteArray(cpr.getInputStream());
      data = new String(bdata, StandardCharsets.UTF_8);
      return data;
    }
  }

  @RestController
  @RequestMapping("/doc/v3/packing-invoice")
  public class PackingInvoiceDocumentationController {
    @GetMapping
    public String getOpenApiYaml() throws IOException {
      String data = "";
      ClassPathResource cpr = new ClassPathResource("packing-invoice-management.yaml");
      byte[] bdata = FileCopyUtils.copyToByteArray(cpr.getInputStream());
      data = new String(bdata, StandardCharsets.UTF_8);
      return data;
    }
  }
}
