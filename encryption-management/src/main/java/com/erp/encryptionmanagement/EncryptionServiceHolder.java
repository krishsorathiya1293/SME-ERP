package com.erp.encryptionmanagement;

import com.erp.encryptionmanagement.service.EncryptionService;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

/** Bridge for JPA AttributeConverter, which is instantiated by the JPA provider (not Spring). */
@Component
public class EncryptionServiceHolder implements ApplicationContextAware {

  private static ApplicationContext applicationContext;

  @Override
  public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
    EncryptionServiceHolder.applicationContext = applicationContext;
  }

  public static EncryptionService get() {
    if (applicationContext == null) {
      throw new IllegalStateException("Spring ApplicationContext not initialized yet");
    }
    return applicationContext.getBean(EncryptionService.class);
  }
}
