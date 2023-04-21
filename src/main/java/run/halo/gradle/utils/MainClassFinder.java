package run.halo.gradle.utils;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;
import run.halo.gradle.Assert;

public class MainClassFinder {
    private static final String DOT_CLASS = ".class";

    private static final FileFilter CLASS_FILE_FILTER = MainClassFinder::isClassFile;

    private static final FileFilter PACKAGE_DIRECTORY_FILTER = MainClassFinder::isPackageDirectory;

    private static boolean isClassFile(File file) {
        return file.isFile() && file.getName().endsWith(DOT_CLASS);
    }

    private static boolean isPackageDirectory(File file) {
        return file.isDirectory() && !file.getName().startsWith(".");
    }

    /**
     * Find a single main class from the given {@code rootDirectory}. A main class
     * extends the given {@code superClassName} will be returned.
     *
     * @param rootDirectory the root directory to search
     * @param superClassName the subclass extends the super class is present main-class
     * class
     * @return the main class or {@code null}
     * @throws IOException if the directory cannot be read
     */
    public static String findSingleMainClass(File rootDirectory, String superClassName)
        throws IOException {
        return MainClassFinder.doWithMainClasses(rootDirectory, superClassName);
    }

    /**
     * Perform the given callback operation on all main classes from the given root
     * directory.
     *
     * @param rootDirectory the root directory
     * @param superClassName the super class name to search for main class
     * @return the first callback result or {@code null}
     * @throws IOException in case of I/O errors
     */
    static String doWithMainClasses(File rootDirectory, String superClassName)
        throws IOException {
        if (!rootDirectory.exists()) {
            return null; // nothing to do
        }
        if (!rootDirectory.isDirectory()) {
            throw new IllegalArgumentException("Invalid root directory '" + rootDirectory + "'");
        }
        String prefix = rootDirectory.getAbsolutePath() + "/";
        Deque<File> stack = new ArrayDeque<>();
        stack.push(rootDirectory);
        while (!stack.isEmpty()) {
            File file = stack.pop();
            if (file.isFile()) {
                try (InputStream inputStream = new FileInputStream(file)) {
                    ClassReader classReader = new ClassReader(inputStream);
                    ClassDescriptor classDescriptor = new ClassDescriptor(superClassName);
                    classReader.accept(classDescriptor, ClassReader.SKIP_CODE);
                    if (StringUtils.isNotBlank(classDescriptor.getMainClassName())) {
                        return convertToClassName(file.getAbsolutePath(), prefix);
                    }
                }
            }
            if (file.isDirectory()) {
                pushAllSorted(stack, file.listFiles(PACKAGE_DIRECTORY_FILTER));
                pushAllSorted(stack, file.listFiles(CLASS_FILE_FILTER));
            }
        }
        return null;
    }

    private static void pushAllSorted(Deque<File> stack, File[] files) {
        Arrays.sort(files, Comparator.comparing(File::getName));
        for (File file : files) {
            stack.push(file);
        }
    }

    private static String convertToClassName(String name, String prefix) {
        name = name.replace('/', '.');
        name = name.replace('\\', '.');
        name = name.substring(0, name.length() - DOT_CLASS.length());
        if (prefix != null) {
            name = name.substring(prefix.length());
        }
        return name;
    }

    private static class ClassDescriptor extends ClassVisitor {

        private final Set<String> mainClassNames = new HashSet<>();

        private final String superClassPath;

        ClassDescriptor(String superClassName) {
            super(AsmConst.ASM_VERSION);
            Assert.notNull(superClassName, "Super class name must not be null");
            Assert.state(!superClassName.startsWith(".") && !superClassName.endsWith(DOT_CLASS),
                () -> "Super class name must not start with '.'");
            this.superClassPath = superClassName.replace('.', '/');
        }

        public void visit(int version, int access, String name, String signature, String superName,
            String[] interfaces) {
            if (superName.equals(superClassPath)) {
                boolean isPublic = (access & Opcodes.ACC_PUBLIC) != 0;
                boolean isAbstract = (access & Opcodes.ACC_ABSTRACT) != 0;
                if (isPublic && !isAbstract) {
                    this.mainClassNames.add(name);
                }
            }
        }

        public String getMainClassName() {
            if (mainClassNames.size() > 1) {
                throw new IllegalStateException(
                    "Unable to find a single main class from the following candidates "
                        + mainClassNames);
            }
            return (mainClassNames.isEmpty() ? null
                : mainClassNames.iterator().next());
        }
    }
}
