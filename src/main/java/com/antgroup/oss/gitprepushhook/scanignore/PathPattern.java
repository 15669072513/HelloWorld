package com.antgroup.oss.gitprepushhook.scanignore;

public abstract class PathPattern {

    protected final String pattern;
    private final boolean exclude;
    protected final boolean matchFileName;
    private final boolean matchDir;

    public static PathPattern create(String pattern) {
        if (hasNoWildcards(pattern, 0, pattern.length())) {
            return new NoWildcardPathPattern(pattern);
        } else {
            return new WildcardPathPattern(pattern);
        }
    }

    private static boolean isWildcard(char c) {
        return c == '*' || c == '[' || c == '?' || c == '\\';
    }

    private static boolean hasNoWildcards(String pattern, int from, int to) {
        for (int i = from; i < to; i++) {
            if (isWildcard(pattern.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    private PathPattern(String pattern) {
        StringBuilder builder = new StringBuilder(pattern);
        this.exclude = pattern.charAt(0) != '!';
        if (!exclude) {
            builder.deleteCharAt(0);
        }
        this.matchDir = pattern.endsWith("/");
        if (matchDir) {
            builder.deleteCharAt(builder.length() - 1);
        }

        if (builder.charAt(0) == '/') {
            builder.deleteCharAt(0);
            this.matchFileName = false;
        } else {
            this.matchFileName = builder.indexOf("/") == -1;
        }
        this.pattern = builder.toString();
    }

    public boolean isExclude() {
        return exclude;
    }

    public boolean matches(String path, boolean isDirectory, String basePath) {
        if (matchDir && !isDirectory) {
            return false;
        }
        if (matchFileName && matchesFileName(path)) {
            return true;
        }
        if (basePath.length() > 0 && !path.startsWith(basePath)) {
            return false;
        }

        return matchesPathName(path, basePath);
    }

    protected abstract boolean matchesFileName(String path);

    protected abstract boolean matchesPathName(String path, String basePath);

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(exclude ? "EX" : "IN").append("CLUDE(");
        builder.append(pattern);
        if (!matchFileName) {
            builder.append(", path");
        }
        if (matchDir) {
            builder.append(", dirs");
        }
        return builder.append(")").toString();
    }

    private static class NoWildcardPathPattern extends PathPattern {

        private NoWildcardPathPattern(String pattern) {
            super(pattern);
        }

        @Override
        protected boolean matchesFileName(String path) {
            if (path.length() > pattern.length() &&
                    path.charAt(path.length() - pattern.length() - 1) != '/') {
                return false;
            }
            return path.endsWith(pattern);
        }

        @Override
        protected boolean matchesPathName(String path, String basePath) {
            int baseLength = basePath.length() > 0 ? basePath.length() + 1 : 0;
            return path.length() - baseLength == pattern.length() &&
                    path.startsWith(pattern, baseLength);
        }
    }

    private static class WildcardPathPattern extends PathPattern {

        private final int regionFrom, regionLength;

        private WildcardPathPattern(String patternString) {
            super(patternString);
            int from = 0, to = 0;
            if (matchFileName) {
                if (pattern.startsWith("*")) {
                    from = 1;
                    to = pattern.length();
                } else if (pattern.endsWith("*")) {
                    from = 0;
                    to = pattern.length() - 1;
                }
                if (hasNoWildcards(pattern, from, to)) {
                    this.regionFrom = from;
                    this.regionLength = to - from;
                    return;
                }
            }
            this.regionFrom = 0;
            this.regionLength = 0;
        }

        @Override
        protected boolean matchesFileName(String path) {
            int from = path.lastIndexOf('/') + 1;
            if (from >= path.length()) {
                return false;
            }
            if (regionLength > 0) {
                int offset = regionFrom > 0
                        ? path.length() - regionLength : from;
                return offset >= from &&
                        path.regionMatches(offset, pattern, regionFrom, regionLength);
            }
            return FnMatch.fnmatch(pattern, path, from);
        }

        @Override
        protected boolean matchesPathName(String path, String basePath) {
            int baseLength = basePath.length() > 0 ? basePath.length() + 1 : 0;
            return FnMatch.fnmatch(pattern, path, baseLength, FnMatch.Flag.PATHNAME);
        }
    }
}
