/*
 * Copyright 2011 the original author or authors.
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
import org.gradle.api.internal.file.collections.DefaultFileCollectionResolveContext;
import org.gradle.api.internal.file.collections.DirectoryFileTree;
import org.gradle.api.internal.file.collections.MinimalFileCollection;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;

import java.util.*;

public class DefaultFileCacheListener implements FileCacheListener, FileSnapshotCache {
    private static final Logger LOGGER = Logging.getLogger(DefaultFileCacheListener.class);
    private final Set<DirectoryFileTree> cachable = new HashSet<DirectoryFileTree>();
    private final Map<DirectoryFileTree, Map<String, Object>> snapshots = new HashMap<DirectoryFileTree, Map<String, Object>>();

    public void cacheable(FileCollection files) {
        List<MinimalFileCollection> collections = new DefaultFileCollectionResolveContext().add(files.getAsFileTree()).resolveAsMinimalFileCollections();
        for (MinimalFileCollection collection : collections) {
            if (collection instanceof DirectoryFileTree) {
                DirectoryFileTree fileTree = (DirectoryFileTree) collection;
//                LOGGER.lifecycle("-> Can cache snapshot for {}", fileTree.getDisplayName());
                cachable.add(fileTree);
            }
        }
    }

    public void invalidate(FileCollection files) {
        List<MinimalFileCollection> collections = new DefaultFileCollectionResolveContext().add(files.getAsFileTree()).resolveAsMinimalFileCollections();
        for (MinimalFileCollection collection : collections) {
            if (collection instanceof DirectoryFileTree) {
//                LOGGER.lifecycle("-> Invalidate cached snapshot for {}", collection.getDisplayName());
                cachable.remove(collection);
                snapshots.remove(collection);
            }
        }
    }

    public void invalidateAll() {
//        LOGGER.lifecycle("-> Invalidate all cached snapshots");
        cachable.clear();
        snapshots.clear();
    }

    public Map<String, Object> get(MinimalFileCollection fileCollection) {
        Map<String, Object> snapshot = snapshots.get(fileCollection);
        if (fileCollection instanceof DirectoryFileTree) {
            if (snapshot != null) {
                LOGGER.lifecycle("-> USING CACHED SNAPSHOT FOR {}", fileCollection.getDisplayName());
            } else {
//                LOGGER.lifecycle("-> no cached snapshot for {}", fileCollection.getDisplayName());
            }
        }
        return snapshot;
    }

    public void put(MinimalFileCollection fileCollection, Map<String, Object> snapshot) {
        if (cachable.contains(fileCollection)) {
//            LOGGER.lifecycle("-> caching snapshot for {}", fileCollection.getDisplayName());
            snapshots.put((DirectoryFileTree) fileCollection, snapshot);
        } else {
            if (fileCollection instanceof DirectoryFileTree) {
//                LOGGER.lifecycle("-> ignoring snapshot for {}", fileCollection.getDisplayName());
            }
        }
    }
}
