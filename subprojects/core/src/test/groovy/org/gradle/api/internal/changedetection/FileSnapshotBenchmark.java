package org.gradle.api.internal.changedetection;

import groovy.lang.Closure;
import org.gradle.CacheUsage;
import org.gradle.api.UncheckedIOException;
import org.gradle.api.file.FileCollection;
import org.gradle.api.file.FileTree;
import org.gradle.api.file.FileVisitor;
import org.gradle.api.internal.file.DefaultConfigurableFileTree;
import org.gradle.api.internal.file.IdentityFileResolver;
import org.gradle.api.specs.Spec;
import org.gradle.api.tasks.StopExecutionException;
import org.gradle.api.tasks.TaskDependency;
import org.gradle.api.tasks.util.PatternFilterable;
import org.gradle.cache.DefaultCacheFactory;
import org.gradle.cache.DefaultCacheRepository;
import org.gradle.util.Clock;
import org.gradle.util.GUtil;
import org.gradle.util.TemporaryFolder;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import java.io.*;
import java.util.*;

public class FileSnapshotBenchmark {
    @Rule
    public TemporaryFolder tmpDir = new TemporaryFolder();
    @Rule
    public TestName name = new TestName();
    final DefaultCacheRepository cacheRepository = new DefaultCacheRepository(tmpDir.getDir(), CacheUsage.ON, new DefaultCacheFactory());
    final FileSnapshotter snapshotter = new DefaultFileSnapshotter(new CachingHasher(new DefaultHasher(), cacheRepository));

    @Before
    public void setup() {
        System.out.println("----- " + name.getMethodName());
    }

    @Test
    public void warmup() {
        for (int i = 0; i < 4; i++) {
            doSnapshot(getLargeSourceFiles(), snapshotter);
        }
    }

    @Test @Ignore
    public void iterateLargeFileCollection() {
        iterate(getLargeSourceFiles());
    }

    @Test @Ignore
    public void iterateLargeFileCollectionViaCache() {
        iterate(new CachingFileIterable(getLargeSourceFiles()));
    }

    @Test @Ignore
    public void iterateSmallFileCollection() {
        iterate(getSmallSourceFiles());
    }

    @Test @Ignore
    public void iterateSmallFileCollectionViaCache() {
        iterate(new CachingFileIterable(getSmallSourceFiles()));
    }

    @Test
    public void snapshotLargeFileCollection() {
        snapshot(getLargeSourceFiles(), snapshotter);
    }

    @Test
    public void snapshotLargeFileCollectionViaCache() {
        snapshot(new CachingFileIterable(getLargeSourceFiles()), snapshotter);
    }

    @Test
    public void snapshotLargeFileCollectionViaSnapshotCache() {
        snapshot(getLargeSourceFiles(), cachingSnapshotter());
    }

    @Test
    public void snapshotLargeFileCollectionViaPersistentSnapshotCache() {
        snapshot(getLargeSourceFiles(), persistentCachingSnapshotter());
    }

    @Test
    public void snapshotSmallFileCollection() {
        snapshot(getSmallSourceFiles(), snapshotter);
    }

    @Test
    public void snapshotSmallFileCollectionViaCache() {
        snapshot(new CachingFileIterable(getSmallSourceFiles()), snapshotter);
    }

    @Test
    public void snapshotSmallFileCollectionViaSnapshotCache() {
        snapshot(getSmallSourceFiles(), cachingSnapshotter());
    }

    @Test
    public void snapshotSmallFileCollectionViaPersistentSnapshotCache() {
        snapshot(getSmallSourceFiles(), persistentCachingSnapshotter());
    }

    private FileSnapshotter cachingSnapshotter() {
        return new CachingFileSnapshotter();
    }

    private FileSnapshotter persistentCachingSnapshotter() {
        return new PersistentCachingFileSnapshotter(tmpDir.createDir("snapshot-cache"));
    }

    private FileCollection getLargeSourceFiles() {
        File file = new File("performanceTest/build/mixedSize/project1/src/main/java").getAbsoluteFile();
        assert file.isDirectory();
        return new DefaultConfigurableFileTree(file, new IdentityFileResolver(), null);
    }

    private FileCollection getSmallSourceFiles() {
        File file = new File("performanceTest/build/mixedSize/project2/src/main/java").getAbsoluteFile();
        assert file.isDirectory();
        return new DefaultConfigurableFileTree(file, new IdentityFileResolver(), null);
    }

    private void iterate(Iterable<File> files) {
        doIterate(files);
        doIterate(files);

        Clock clock = new Clock();
        doIterate(files);
        doIterate(files);
        System.out.println("TOTAL: " + clock.getTime());
    }

    private void doIterate(Iterable<File> files) {
        Clock clock = new Clock();
        int counter = 0;
        for (File file : files) {
            counter++;
        }
        System.out.println("TIME: " + clock.getTime());
    }

    private void snapshot(FileCollection files, FileSnapshotter snapshotter) {
//        doSnapshot(files, snapshotter);
//        doSnapshot(files, snapshotter);

        Clock clock = new Clock();
        doSnapshot(files, snapshotter);
        doSnapshot(files, snapshotter);
        System.out.println("TOTAL: " + clock.getTime());
    }

    private void doSnapshot(FileCollection files, FileSnapshotter snapshotter) {
        Clock clock = new Clock();
        snapshotter.snapshot(files);
        System.out.println("TIME: " + clock.getTime());
    }

    private static class CachingFileIterable implements FileTree {
        private final Iterable<File> files;
        private List<File> cached;

        public CachingFileIterable(Iterable<File> files) {
            this.files = files;
        }

        public Iterator<File> iterator() {
            if (cached == null) {
                cached = GUtil.addLists(files);
            }
            return cached.iterator();
        }

        public FileCollection add(FileCollection collection) throws UnsupportedOperationException {
            throw new UnsupportedOperationException();
        }

        public File getSingleFile() throws IllegalStateException {
            throw new UnsupportedOperationException();
        }

        public Set<File> getFiles() {
            throw new UnsupportedOperationException();
        }

        public boolean contains(File file) {
            throw new UnsupportedOperationException();
        }

        public String getAsPath() {
            throw new UnsupportedOperationException();
        }

        public FileCollection plus(FileCollection collection) {
            throw new UnsupportedOperationException();
        }

        public FileCollection minus(FileCollection collection) {
            throw new UnsupportedOperationException();
        }

        public FileCollection filter(Closure filterClosure) {
            throw new UnsupportedOperationException();
        }

        public FileCollection filter(Spec<? super File> filterSpec) {
            throw new UnsupportedOperationException();
        }

        public Object asType(Class<?> type) throws UnsupportedOperationException {
            throw new UnsupportedOperationException();
        }

        public boolean isEmpty() {
            throw new UnsupportedOperationException();
        }

        public FileCollection stopExecutionIfEmpty() throws StopExecutionException {
            throw new UnsupportedOperationException();
        }

        public FileTree getAsFileTree() {
            return this;
        }

        public void addToAntBuilder(Object builder, String nodeName, AntType type) {
            throw new UnsupportedOperationException();
        }

        public Object addToAntBuilder(Object builder, String nodeName) {
            throw new UnsupportedOperationException();
        }

        public TaskDependency getBuildDependencies() {
            throw new UnsupportedOperationException();
        }

        public FileTree matching(Closure filterConfigClosure) {
            throw new UnsupportedOperationException();
        }

        public FileTree matching(PatternFilterable patterns) {
            throw new UnsupportedOperationException();
        }

        public FileTree visit(FileVisitor visitor) {
            throw new UnsupportedOperationException();
        }

        public FileTree visit(Closure visitor) {
            throw new UnsupportedOperationException();
        }

        public FileTree plus(FileTree fileTree) {
            throw new UnsupportedOperationException();
        }
    }

    private class CachingFileSnapshotter implements FileSnapshotter {
        final Map<FileCollection, FileCollectionSnapshot> cache = new HashMap<FileCollection, FileCollectionSnapshot>();

        public FileCollectionSnapshot emptySnapshot() {
            throw new UnsupportedOperationException();
        }

        public FileCollectionSnapshot snapshot(FileCollection files) {
            FileCollectionSnapshot snapshot = cache.get(files);
            if (snapshot == null) {
                assert cache.size() == 0;
                snapshot = snapshotter.snapshot(files);
                cache.put(files, snapshot);
            }
            return snapshot;
        }
    }

    private class PersistentCachingFileSnapshotter implements FileSnapshotter {
        final Map<FileCollection, File> cache = new HashMap<FileCollection, File>();
        private final File tmpDir;

        private PersistentCachingFileSnapshotter(File tmpDir) {
            this.tmpDir = tmpDir;
        }

        public FileCollectionSnapshot emptySnapshot() {
            throw new UnsupportedOperationException();
        }

        public FileCollectionSnapshot snapshot(FileCollection files) {
            try {
                File snapshotFile = cache.get(files);
                if (snapshotFile == null) {
                    assert cache.size() == 0;
                    FileCollectionSnapshot snapshot = snapshotter.snapshot(files);
                    snapshotFile = new File(tmpDir, String.valueOf(cache.size()));
                    ObjectOutputStream outstr = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(snapshotFile)));
                    try {
                        outstr.writeObject(snapshot);
                    } finally {
                        outstr.close();
                    }
                    cache.put(files, snapshotFile);
                    return snapshot;
                } else {
                    ObjectInputStream instr = new ObjectInputStream(new BufferedInputStream(new FileInputStream(snapshotFile)));
                    try {
                        return (FileCollectionSnapshot) instr.readObject();
                    } finally {
                        instr.close();
                    }
                }
            } catch (Exception e) {
                throw new UncheckedIOException(e);
            }
        }
    }
}
