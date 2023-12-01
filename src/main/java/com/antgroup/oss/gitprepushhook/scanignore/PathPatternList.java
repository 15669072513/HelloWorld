package com.antgroup.oss.gitprepushhook.scanignore;

import java.util.ArrayList;
import java.util.List;

public class PathPatternList {

    private final List<PathPattern> excludePatterns = new ArrayList<>();
    private final List<PathPattern> includePatterns = new ArrayList<>();

    public PathPatternList() {
    }
    public void add(String patternString) {
        PathPattern pattern = PathPattern.create(patternString);
        if (pattern.isExclude()) {
            excludePatterns.add(pattern);
        } else {
            includePatterns.add(pattern);
        }
    }

    public List<PathPattern> getExcludePatterns() {
        return excludePatterns;
    }

    public List<PathPattern> getIncludePatterns() {
        return includePatterns;
    }
}
