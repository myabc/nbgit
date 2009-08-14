package org.nbgit.util.exclude;

import java.util.Vector;

public class PathPatternList {

    private final Vector<PathPattern> patterns = new Vector<PathPattern>();
    private final String basePath;

    public PathPatternList(String basePath) {
        this.basePath = basePath;
    }

    public void add(String patternString) {
        PathPattern pattern = PathPattern.create(patternString);
        if (pattern.isExclude()) {
            patterns.add(pattern);
        } else {
            patterns.add(0, pattern);
        }
    }

    public PathPattern findPattern(String path, boolean isDirectory) {
        for (PathPattern pattern : patterns) {
            if (pattern.matches(path, isDirectory, basePath)) {
                return pattern;
            }
        }
        return null;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder(getClass().getSimpleName());
        builder.append("[");
        builder.append(basePath);
        builder.append("; ");
        builder.append(patterns.size());
        builder.append("]");
        return builder.toString();
    }
}
