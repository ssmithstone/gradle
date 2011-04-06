/*
 * Copyright 2010 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.api.internal.changedetection;

import org.gradle.api.file.FileCollection;
import org.gradle.api.file.FileTree;
import org.gradle.api.internal.file.collections.DefaultFileCollectionResolveContext;
import org.gradle.api.internal.file.collections.MinimalFileCollection;
import org.gradle.api.internal.file.collections.SimpleFileCollection;
import org.gradle.util.ChangeListener;
import org.gradle.util.NoOpChangeListener;

import java.io.File;
import java.io.Serializable;
import java.math.BigInteger;
import java.util.*;

public class DefaultFileSnapshotter implements FileSnapshotter {
    private final Hasher hasher;
    private final FileSnapshotCache cache;

    public DefaultFileSnapshotter(Hasher hasher, FileSnapshotCache cache) {
        this.hasher = hasher;
        this.cache = cache;
    }

    public FileCollectionSnapshot emptySnapshot() {
        return new FileCollectionSnapshotImpl(new HashMap<String, Object>());
    }

    public FileCollectionSnapshot snapshot(FileCollection sourceFiles) {
        DefaultFileCollectionResolveContext context = new DefaultFileCollectionResolveContext();
        context.add(sourceFiles.getAsFileTree());
        List<MinimalFileCollection> fileCollections = context.resolveAsMinimalFileCollections();
        Map<String, Object> snapshots = new HashMap<String, Object>();
        for (MinimalFileCollection fileCollection : fileCollections) {
            Map<String, Object> snapshot = cache.get(fileCollection);
            if (snapshot == null) {
                snapshot = snapshot(fileCollection);
                cache.put(fileCollection, snapshot);
            }
            snapshots.putAll(snapshot);
        }
        return new FileCollectionSnapshotImpl(snapshots);
    }

    private Map<String, Object> snapshot(MinimalFileCollection fileCollection) {
        DefaultFileCollectionResolveContext context = new DefaultFileCollectionResolveContext();
        Map<String, Object> snapshots = new HashMap<String, Object>();
        context.add(fileCollection);
        for (FileTree fileTree : context.resolveAsFileTrees()) {
            for (File file : fileTree) {
                if (file.isFile()) {
                    snapshots.put(file.getAbsolutePath(), new FileHashSnapshot(hasher.hash(file)));
                } else if (file.isDirectory()) {
                    snapshots.put(file.getAbsolutePath(), new DirSnapshot());
                } else {
                    snapshots.put(file.getAbsolutePath(), new MissingFileSnapshot());
                }
            }
        }
        return snapshots;
    }

    private static class FileHashSnapshot implements Serializable {
        private final byte[] hash;

        public FileHashSnapshot(byte[] hash) {
            this.hash = hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof FileHashSnapshot)) {
                return false;
            }

            FileHashSnapshot other = (FileHashSnapshot) obj;
            return Arrays.equals(hash, other.hash);
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(hash);
        }

        @Override
        public String toString() {
            return new BigInteger(1, hash).toString(16);
        }
    }

    private static class DirSnapshot implements Serializable {
        @Override
        public boolean equals(Object obj) {
            return obj instanceof DirSnapshot;
        }

        @Override
        public int hashCode() {
            return 1;
        }
    }

    private static class MissingFileSnapshot implements Serializable {
        @Override
        public boolean equals(Object obj) {
            return obj instanceof MissingFileSnapshot;
        }

        @Override
        public int hashCode() {
            return -1;
        }
    }

    private static class FileCollectionSnapshotImpl implements FileCollectionSnapshot {
        private final Map<String, Object> snapshots;

        public FileCollectionSnapshotImpl(Map<String, Object> snapshots) {
            this.snapshots = snapshots;
        }

        public FileCollection getFiles() {
            List<File> files = new ArrayList<File>();
            for (Map.Entry<String, Object> entry : snapshots.entrySet()) {
                if (entry.getValue() instanceof FileHashSnapshot) {
                    files.add(new File(entry.getKey()));
                }
            }
            return new SimpleFileCollection(files);
        }

        public void changesSince(FileCollectionSnapshot oldSnapshot, final ChangeListener<File> listener) {
            FileCollectionSnapshotImpl other = (FileCollectionSnapshotImpl) oldSnapshot;
            diff(snapshots, other.snapshots, new ChangeListener<Map.Entry<String, Object>>() {
                public void added(Map.Entry<String, Object> element) {
                    listener.added(new File(element.getKey()));
                }

                public void removed(Map.Entry<String, Object> element) {
                    listener.removed(new File(element.getKey()));
                }

                public void changed(Map.Entry<String, Object> element) {
                    listener.changed(new File(element.getKey()));
                }
            });
        }

        private void diff(Map<String, Object> snapshots, Map<String, Object> oldSnapshots,
                          ChangeListener<Map.Entry<String, Object>> listener) {
            Map<String, Object> otherSnapshots = new HashMap<String, Object>(oldSnapshots);
            for (Map.Entry<String, Object> entry : snapshots.entrySet()) {
                Object otherFile = otherSnapshots.remove(entry.getKey());
                if (otherFile == null) {
                    listener.added(entry);
                } else if (!entry.getValue().equals(otherFile)) {
                    listener.changed(entry);
                }
            }
            for (Map.Entry<String, Object> entry : otherSnapshots.entrySet()) {
                listener.removed(entry);
            }
        }

        public Diff changesSince(final FileCollectionSnapshot oldSnapshot) {
            final FileCollectionSnapshotImpl other = (FileCollectionSnapshotImpl) oldSnapshot;
            return new Diff() {
                public FileCollectionSnapshot applyTo(FileCollectionSnapshot snapshot) {
                    return applyTo(snapshot, new NoOpChangeListener<Merge>());
                }

                public FileCollectionSnapshot applyTo(FileCollectionSnapshot snapshot, final ChangeListener<Merge> listener) {
                    FileCollectionSnapshotImpl target = (FileCollectionSnapshotImpl) snapshot;
                    final Map<String, Object> newSnapshots = new HashMap<String, Object>(target.snapshots);
                    diff(snapshots, other.snapshots, new MapMergeChangeListener<String, Object>(listener, newSnapshots));
                    return new FileCollectionSnapshotImpl(newSnapshots);
                }
            };
        }
    }
}
