package com.antgroup.oss.gitprepushhook.scanignore;

import org.springframework.util.CollectionUtils;

import java.io.File;
import java.util.List;

public class GitIgnore {

  public static final String FILE_NAME = ".scanignore";
  public static final String BASE_PATH = System.getProperty("user.dir");

  private PathPatternList pathPatternList;

  public GitIgnore() {
    File rootDir = new File(BASE_PATH);
    this.init(rootDir);
  }

  public void init(File dir) {
    pathPatternList = ExcludeUtils.readExcludeFile(new File(dir, FILE_NAME));
  }

  public String readScanignoreFile(){
    File rootDir = new File(BASE_PATH);
    String content = ExcludeUtils.readScanignoreFile(new File(rootDir, FILE_NAME));
    return content;
  }

  public boolean isExcluded(String filePath) {
    return isExcluded(new File(filePath));
  }
  public boolean isExcluded(File file) {
    if (pathPatternList == null) {
      return false;
    }
    PathPattern includePattern = match(file, pathPatternList.getIncludePatterns());
    if (includePattern != null) {
      return false;
    }
    PathPattern excludePattern = match(file, pathPatternList.getExcludePatterns());
    return excludePattern != null;
  }
  public PathPattern match(File file, List<PathPattern> patterns) {
    if (CollectionUtils.isEmpty(patterns)) {
      return null;
    }
    String filePath = file.getPath();
    StringBuilder pathBuilder = new StringBuilder(filePath.length());

    while (true) {
      int offset = filePath.indexOf('/', pathBuilder.length() + 1);
      boolean isDirectory = true;

      if (offset == -1) {
        offset = filePath.length();
        isDirectory = file.isDirectory();
      }

      pathBuilder.insert(pathBuilder.length(), filePath, pathBuilder.length(), offset);
      String currentPath = pathBuilder.toString();

      for (PathPattern pattern : patterns) {
        if (pattern.matches(currentPath, isDirectory, "")) {
          return pattern;
        }
      }
      if (!isDirectory || pathBuilder.length() >= filePath.length()) {
        return null;
      }
    }
  }

}
