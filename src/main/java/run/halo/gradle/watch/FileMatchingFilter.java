package run.halo.gradle.watch;

import org.apache.commons.lang3.StringUtils;
import org.gradle.api.internal.file.pattern.PatternMatcher;

import java.io.File;
import java.io.FileFilter;

/**
 * @author guqing
 * @since 2.0.0
 */
public class FileMatchingFilter implements FileFilter {
    private final PatternMatcher patternsMatcher;

    public FileMatchingFilter(PatternMatcher patternsMatcher) {
        this.patternsMatcher = patternsMatcher;
    }

    @Override
    public boolean accept(File pathname) {
        String[] segments = StringUtils.split(pathname.getPath(), File.separator);
        boolean isFile = pathname.isFile();
        return patternsMatcher.test(segments, isFile);
    }
}
