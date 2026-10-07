package com.kob.botruningsystem.core;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.FileObject;
import javax.tools.ForwardingJavaFileManager;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 运行时内存编译器, 替代 joor。
 * joor-java-8 0.9.14 在 JDK 16+ 的强封装下深反射被拒, compile 静默失败(type()==null),
 * 导致任何用户 Bot 都无法编译。这里直接使用 javax.tools.JavaCompiler:
 *  - 支持嵌套类(用户 Bot 代码的内部类字节码全部注册进 ClassLoader)
 *  - 编译错误会带诊断信息抛出, 便于在服务日志中排查用户代码语法问题
 */
public class BotCompiler {

    public static SupplierHolder compile(String fullClassName, String source) {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            throw new IllegalStateException("no system java compiler available");
        }
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        MemClassManager fileManager = new MemClassManager(
                compiler.getStandardFileManager(diagnostics, null, null));
        JavaFileObject sourceFile = new StringSource(fullClassName, source);

        Boolean ok = compiler.getTask(null, fileManager, diagnostics, null, null,
                List.of(sourceFile)).call();
        if (!Boolean.TRUE.equals(ok)) {
            StringBuilder sb = new StringBuilder("bot code compile failed:");
            for (Diagnostic<? extends JavaFileObject> d : diagnostics.getDiagnostics()) {
                sb.append("\n").append(d);
            }
            throw new IllegalStateException(sb.toString());
        }
        return new SupplierHolder(fileManager, fullClassName);
    }

    /** 持有编译产物, 惰性加载主类并实例化 */
    public static class SupplierHolder {
        private final MemClassManager fileManager;
        private final String fullClassName;
        private volatile java.util.function.Supplier<Integer> instance;

        SupplierHolder(MemClassManager fileManager, String fullClassName) {
            this.fileManager = fileManager;
            this.fullClassName = fullClassName;
        }

        public java.util.function.Supplier<Integer> get() {
            if (instance == null) {
                synchronized (this) {
                    if (instance == null) {
                        try {
                            // 每次编译产物用独立的 ClassLoader, 支持同名类反复编译(不同 uid)
                            MemClassLoader loader = new MemClassLoader(
                                    fileManager.classes, Thread.currentThread().getContextClassLoader());
                            Class<?> clazz = loader.loadClass(fullClassName);
                            instance = (java.util.function.Supplier<Integer>) clazz
                                    .getDeclaredConstructor().newInstance();
                        } catch (Exception e) {
                            throw new IllegalStateException("bot class load failed: " + fullClassName, e);
                        }
                    }
                }
            }
            return instance;
        }
    }

    /** 从内存字节码加载主类与其嵌套类 */
    static class MemClassLoader extends ClassLoader {
        private final Map<String, byte[]> classes;

        MemClassLoader(Map<String, byte[]> classes, ClassLoader parent) {
            super(parent);
            this.classes = classes;
        }

        @Override
        protected Class<?> findClass(String name) throws ClassNotFoundException {
            byte[] bytes = classes.get(name);
            if (bytes == null) throw new ClassNotFoundException(name);
            return defineClass(name, bytes, 0, bytes.length);
        }
    }

    /** 收集编译输出的字节码(主类与嵌套类) */
    static class MemClassManager extends ForwardingJavaFileManager<JavaFileManager> {
        private final Map<String, byte[]> classes = new HashMap<>();

        MemClassManager(JavaFileManager delegate) {
            super(delegate);
        }

        @Override
        public JavaFileObject getJavaFileForOutput(Location location, String className,
                                                   JavaFileObject.Kind kind, FileObject sibling) {
            return new SimpleJavaFileObject(
                    URI.create("mem:///" + className.replace('.', '/') + kind.extension), kind) {
                @Override
                public java.io.OutputStream openOutputStream() {
                    return new ByteArrayOutputStream() {
                        @Override
                        public void close() {
                            classes.put(className, toByteArray());
                        }
                    };
                }
            };
        }
    }

    static class StringSource extends SimpleJavaFileObject {
        private final String source;

        StringSource(String className, String source) {
            super(URI.create("string:///" + className.replace('.', '/') + Kind.SOURCE.extension), Kind.SOURCE);
            this.source = source;
        }

        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) {
            return source;
        }
    }
}
