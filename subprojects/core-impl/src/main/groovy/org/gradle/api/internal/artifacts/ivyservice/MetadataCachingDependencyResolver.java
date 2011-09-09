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
package org.gradle.api.internal.artifacts.ivyservice;

import org.apache.ivy.core.cache.ArtifactOrigin;
import org.apache.ivy.core.cache.RepositoryCacheManager;
import org.apache.ivy.core.module.descriptor.Artifact;
import org.apache.ivy.core.module.descriptor.DependencyDescriptor;
import org.apache.ivy.core.module.id.ModuleRevisionId;
import org.apache.ivy.core.report.ArtifactDownloadReport;
import org.apache.ivy.core.report.DownloadReport;
import org.apache.ivy.core.resolve.DownloadOptions;
import org.apache.ivy.core.resolve.ResolveData;
import org.apache.ivy.core.resolve.ResolvedModuleRevision;
import org.apache.ivy.core.search.ModuleEntry;
import org.apache.ivy.core.search.OrganisationEntry;
import org.apache.ivy.core.search.RevisionEntry;
import org.apache.ivy.plugins.namespace.Namespace;
import org.apache.ivy.plugins.resolver.DependencyResolver;
import org.apache.ivy.plugins.resolver.ResolverSettings;
import org.apache.ivy.plugins.resolver.util.ResolvedResource;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.*;

public class MetadataCachingDependencyResolver implements DependencyResolver {
    private final DependencyResolver resolver;
    private final Map<ModuleRevisionId, ResolvedModuleRevision> dependencies = new HashMap<ModuleRevisionId, ResolvedModuleRevision>();
    private final Map<Artifact, ArtifactDownloadReport> artifacts = new HashMap<Artifact, ArtifactDownloadReport>();

    public MetadataCachingDependencyResolver(DependencyResolver resolver) {
        this.resolver = resolver;
    }

    public DependencyResolver getResolver() {
        return resolver;
    }

    public String getName() {
        return resolver.getName();
    }

    public void setName(String name) {
        resolver.setName(name);
    }

    public ResolvedModuleRevision getDependency(DependencyDescriptor dd, ResolveData data) throws ParseException {
        // Ignore this resolver if the dependency has already been resolved
        if (data.getCurrentResolvedModuleRevision() != null) {
            return data.getCurrentResolvedModuleRevision();
        }

        System.out.format("-> %s resolve %s%n", resolver.getName(), dd);
        System.out.format("   dep %s%n", dd.getDependencyRevisionId());
        ResolvedModuleRevision dependency;
        // Cached dep may be null
        if (dependencies.containsKey(dd.getDependencyRevisionId())) {
            dependency = dependencies.get(dd.getDependencyRevisionId());
            System.out.format("   reusing %s%n", dependency);
        } else {
            dependency = resolver.getDependency(dd, data);
            dependencies.put(dd.getDependencyRevisionId(), dependency);
            System.out.format("   got %s%n", dependency);
        }
        return dependency;
    }

    public ResolvedResource findIvyFileRef(DependencyDescriptor dd, ResolveData data) {
        throw new UnsupportedOperationException("This should not be called.");
    }

    public DownloadReport download(Artifact[] artifacts, DownloadOptions options) {
        DownloadReport report = new DownloadReport();
        List<Artifact> missing = new ArrayList<Artifact>();
        for (Artifact artifact : artifacts) {
            ArtifactDownloadReport artfactReport = this.artifacts.get(artifact);
            if (artfactReport != null) {
                System.out.format("   reusing report for %s%n", artifact);
                report.addArtifactReport(artfactReport);
            } else {
                missing.add(artifact);
            }
        }

        if (!missing.isEmpty()) {
            DownloadReport actualReport = resolver.download(missing.toArray(new Artifact[missing.size()]), options);
            for (ArtifactDownloadReport artifactReport : actualReport.getArtifactsReports()) {
                System.out.format("    got report for %s%n", artifactReport.getArtifact());
                this.artifacts.put(artifactReport.getArtifact(), artifactReport);
                report.addArtifactReport(artifactReport);
            }
        }

        return report;
    }

    public ArtifactDownloadReport download(ArtifactOrigin artifact, DownloadOptions options) {
        ArtifactDownloadReport downloadReport = artifacts.get(artifact.getArtifact());
        if (downloadReport == null) {
            downloadReport = resolver.download(artifact, options);
            artifacts.put(artifact.getArtifact(), downloadReport);
            System.out.format("    got report for %s%n", artifact.getArtifact());
        } else {
            System.out.format("   reusing report for %s%n", artifact.getArtifact());
        }
        return downloadReport;
    }

    public boolean exists(Artifact artifact) {
        throw new UnsupportedOperationException("This should not be called.");
    }

    public ArtifactOrigin locate(Artifact artifact) {
        throw new UnsupportedOperationException("This should not be called.");
    }

    public void reportFailure() {
        resolver.reportFailure();
    }

    public void reportFailure(Artifact art) {
        resolver.reportFailure(art);
    }

    public String[] listTokenValues(String token, Map otherTokenValues) {
        return resolver.listTokenValues(token, otherTokenValues);
    }

    public Map[] listTokenValues(String[] tokens, Map criteria) {
        return resolver.listTokenValues(tokens, criteria);
    }

    public OrganisationEntry[] listOrganisations() {
        return resolver.listOrganisations();
    }

    public ModuleEntry[] listModules(OrganisationEntry org) {
        return resolver.listModules(org);
    }

    public RevisionEntry[] listRevisions(ModuleEntry module) {
        return resolver.listRevisions(module);
    }

    public void abortPublishTransaction() throws IOException {
        throw new UnsupportedOperationException("Should not be publishing using this resolver");
    }

    public void publish(Artifact artifact, File src, boolean overwrite) throws IOException {
        throw new UnsupportedOperationException("Should not be publishing using this resolver");
    }

    public void beginPublishTransaction(ModuleRevisionId module, boolean overwrite) throws IOException {
        throw new UnsupportedOperationException("Should not be publishing using this resolver");
    }

    public void commitPublishTransaction() throws IOException {
        throw new UnsupportedOperationException("Should not be publishing using this resolver");
    }

    public Namespace getNamespace() {
        return resolver.getNamespace();
    }

    public void dumpSettings() {
        resolver.dumpSettings();
    }

    public void setSettings(ResolverSettings settings) {
        resolver.setSettings(settings);
    }

    public RepositoryCacheManager getRepositoryCacheManager() {
        return resolver.getRepositoryCacheManager();
    }
}
