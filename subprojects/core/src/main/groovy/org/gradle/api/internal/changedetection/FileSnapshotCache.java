package org.gradle.api.internal.changedetection;

import org.gradle.api.internal.file.collections.MinimalFileCollection;

import java.util.Map;

public interface FileSnapshotCache {
    Map<String, Object> get(MinimalFileCollection fileCollection);

    void put(MinimalFileCollection fileCollection, Map<String, Object> snapshot);
}
