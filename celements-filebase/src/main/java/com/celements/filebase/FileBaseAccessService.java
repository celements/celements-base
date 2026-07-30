package com.celements.filebase;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.xwiki.configuration.ConfigurationSource;

import com.celements.filebase.exceptions.NoValidFileBaseImplFound;
import com.google.common.base.Strings;
import com.xpn.xwiki.web.Utils;

@Component
public class FileBaseAccessService implements IFileBaseAccessRole {

  private static final Logger LOGGER = LoggerFactory.getLogger(FileBaseAccessService.class);

  private final ConfigurationSource configuration;

  public FileBaseAccessService(ConfigurationSource configuration) {
    this.configuration = configuration;
  }

  @Override
  public IFileBaseServiceRole getInstance() throws NoValidFileBaseImplFound {
    String fileBaseImplKey = getFileBaseImplKey();
    if (!Strings.isNullOrEmpty(fileBaseImplKey) && !"default".equals(fileBaseImplKey)) {
      IFileBaseServiceRole theFileBaseImpl = Utils.getComponent(IFileBaseServiceRole.class,
          fileBaseImplKey);
      if (theFileBaseImpl != null) {
        return theFileBaseImpl;
      }
    }
    LOGGER.error("Failed to get valid FileBase implementation instance.");
    throw new NoValidFileBaseImplFound(fileBaseImplKey);
  }

  private String getFileBaseImplKey() {
    return configuration.getProperty(FILEBASE_SERVICE_IMPL_CFG,
        SingleDocFileBaseService.FILEBASE_SINGLE_DOC);
  }

}
