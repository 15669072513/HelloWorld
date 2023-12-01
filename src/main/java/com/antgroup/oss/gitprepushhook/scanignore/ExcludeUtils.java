package com.antgroup.oss.gitprepushhook.scanignore;

import com.antgroup.oss.gitprepushhook.PrePushHook;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public class ExcludeUtils {

    private ExcludeUtils() {
    }

    static PathPatternList readExcludeFile(File file) {
        PrePushHook.logDebug("读取.scanignore: "+ file.getAbsolutePath());
        if (!file.exists() || !file.canRead()) {
            PrePushHook.logDebug(".scanignore 文件不存在");
            return null;
        }
        BufferedReader reader = null;
        PathPatternList list = null;
        try {
            reader = new BufferedReader(new FileReader(file));
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.length() > 0 && line.charAt(0) != '#') {
                    if (list == null)
                        list = new PathPatternList();
                    list.add(line);
                }
            }
        } catch (Throwable error) {
            return null;
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException ignored) {
                }
            }
        }
        return list;
    }

    static String readScanignoreFile(File file) {
        PrePushHook.logDebug("读取.scanignore: "+ file.getAbsolutePath());
        if (!file.exists() || !file.canRead()) {
            PrePushHook.logDebug(".scanignore 文件不存在");
            return null;
        }
        BufferedReader reader = null;
        StringBuilder content = new StringBuilder();
        try {
            reader = new BufferedReader(new FileReader(file));
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.length() > 0 && line.charAt(0) != '#') {
                    content.append(line+'\n');
                }
            }
        } catch (Throwable error) {
            return null;
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException ignored) {
                }
            }
        }
        return content.toString();
    }

}
