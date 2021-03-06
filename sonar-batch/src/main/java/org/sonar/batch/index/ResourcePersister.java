/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.batch.index;

import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringUtils;
import org.sonar.api.database.DatabaseSession;
import org.sonar.api.database.model.ResourceModel;
import org.sonar.api.database.model.Snapshot;
import org.sonar.api.resources.Language;
import org.sonar.api.resources.Library;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.Qualifiers;
import org.sonar.api.resources.Resource;
import org.sonar.api.resources.ResourceUtils;
import org.sonar.api.resources.Scopes;
import org.sonar.api.security.ResourcePermissions;
import org.sonar.api.utils.SonarException;
import org.sonar.api.utils.internal.Uuids;
import org.sonar.batch.ProjectTree;
import org.sonar.core.component.ScanGraph;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import javax.persistence.NonUniqueResultException;
import javax.persistence.Query;

import java.util.Date;
import java.util.List;

public class ResourcePersister implements ScanPersister {

  private static final String RESOURCE_ID = "resourceId";
  private static final String LAST = "last";
  private static final String VERSION = "version";
  private static final String SCOPE = "scope";
  private static final String QUALIFIER = "qualifier";

  private final DatabaseSession session;
  private final ResourcePermissions permissions;
  private final ResourceCache resourceCache;
  private final ScanGraph graph;
  private final ProjectTree projectTree;

  public ResourcePersister(ProjectTree projectTree, DatabaseSession session, ResourcePermissions permissions, ResourceCache resourceCache, ScanGraph graph) {
    this.projectTree = projectTree;
    this.session = session;
    this.permissions = permissions;
    this.resourceCache = resourceCache;
    this.graph = graph;
  }

  @Override
  public void persist() {
    for (BatchResource resource : resourceCache.all()) {
      persist(resource);
    }

    for (BatchResource lib : resourceCache.allLibraries()) {
      if (lib.snapshot() != null) {
        // already persisted
        continue;
      }
      persistLibrary(lib);
    }
  }

  private void persistLibrary(BatchResource lib) {
    Snapshot s = persistLibrary(projectTree.getRootProject().getAnalysisDate(), (Library) lib.resource());
    lib.setSnapshot(s);
  }

  private void persist(BatchResource batchResource) {
    if (batchResource.snapshot() != null) {
      // already persisted
      return;
    }
    BatchResource parentBatchResource = batchResource.parent();
    if (parentBatchResource != null) {
      persist(parentBatchResource);
    }
    Snapshot s = persist(findParentModule(batchResource), batchResource.resource(), parentBatchResource != null ? parentBatchResource.resource() : null);
    batchResource.setSnapshot(s);
    if (ResourceUtils.isPersistable(batchResource.resource())) {
      graph.addComponent(batchResource.resource(), batchResource.snapshotId());
    }
  }

  @CheckForNull
  private Project findParentModule(BatchResource batchResource) {
    if (batchResource != null && batchResource.parent() != null) {
      if (batchResource.parent().resource() instanceof Project) {
        return (Project) batchResource.parent().resource();
      } else {
        return findParentModule(batchResource.parent());
      }
    }
    return null;
  }

  private Snapshot persistProject(Project project, @Nullable Project parent) {
    ResourceModel model = findOrCreateModel(project, parent);
    // Used by ResourceKeyMigration in order to know that a project has already being migrated
    model.setDeprecatedKey(project.getKey());
    // language is null for project since multi-language support
    model.setLanguageKey(null);

    // For views
    if (project instanceof ResourceCopy) {
      model.setCopyResourceId(((ResourceCopy) project).getCopyResourceId());
    }

    Snapshot parentSnapshot = null;
    if (parent != null) {
      // assume that the parent project has already been saved
      parentSnapshot = resourceCache.get(project.getParent().getEffectiveKey()).snapshot();
      model.setRootId((Integer) ObjectUtils.defaultIfNull(parentSnapshot.getRootProjectId(), parentSnapshot.getResourceId()));
    } else {
      model.setRootId(null);
    }
    model = session.save(model);
    project.setId(model.getId());
    project.setUuid(model.getUuid());

    Snapshot snapshot = new Snapshot(model, parentSnapshot);
    snapshot.setVersion(project.getAnalysisVersion());
    snapshot.setCreatedAt(project.getAnalysisDate());
    snapshot.setBuildDate(new Date());
    snapshot = session.save(snapshot);
    session.commit();

    if (!permissions.hasRoles(project)) {
      permissions.grantDefaultRoles(project);
    }

    return snapshot;
  }

  Snapshot persist(Project project, Resource resource, @Nullable Resource parent) {
    Snapshot snapshot;
    if (resource instanceof Project) {
      // should not occur, please use the method saveProject()
      snapshot = persistProject((Project) resource, (Project) parent);

    } else if (resource instanceof Library) {
      snapshot = persistLibrary(project.getAnalysisDate(), (Library) resource);

    } else {
      snapshot = persistFileOrDirectory(project, resource, parent);
    }

    return snapshot;
  }

  private Snapshot persistLibrary(Date analysisDate, Library library) {
    ResourceModel model = findOrCreateModel(library, null);
    model = session.save(model);
    // TODO to be removed
    library.setId(model.getId());
    library.setUuid(model.getUuid());
    library.setEffectiveKey(library.getKey());

    Snapshot snapshot = findLibrarySnapshot(model.getId(), library.getVersion());
    if (snapshot == null) {
      snapshot = new Snapshot(model, null);
      snapshot.setCreatedAt(analysisDate);
      snapshot.setBuildDate(new Date());
      snapshot.setVersion(library.getVersion());
      snapshot.setStatus(Snapshot.STATUS_PROCESSED);

      // see http://jira.codehaus.org/browse/SONAR-1850
      // The qualifier must be LIB, even if the resource is TRK, because this snapshot has no measures.
      snapshot.setQualifier(Qualifiers.LIBRARY);
      snapshot = session.save(snapshot);
    }
    session.commit();
    return snapshot;
  }

  private Snapshot findLibrarySnapshot(Integer resourceId, String version) {
    Query query = session.createQuery("from " + Snapshot.class.getSimpleName() +
      " s WHERE s.resourceId=:resourceId AND s.version=:version AND s.scope=:scope AND s.qualifier<>:qualifier AND s.last=:last");
    query.setParameter(RESOURCE_ID, resourceId);
    query.setParameter(VERSION, version);
    query.setParameter(SCOPE, Scopes.PROJECT);
    query.setParameter(QUALIFIER, Qualifiers.LIBRARY);
    query.setParameter(LAST, Boolean.TRUE);
    List<Snapshot> snapshots = query.getResultList();
    if (snapshots.isEmpty()) {
      snapshots = session.getResults(Snapshot.class, RESOURCE_ID, resourceId, VERSION, version, SCOPE, Scopes.PROJECT, QUALIFIER, Qualifiers.LIBRARY);
    }
    return snapshots.isEmpty() ? null : snapshots.get(0);
  }

  /**
   * Everything except project and library
   */
  private Snapshot persistFileOrDirectory(Project project, Resource resource, @Nullable Resource parentReference) {
    BatchResource moduleResource = resourceCache.get(project.getEffectiveKey());
    Integer moduleId = moduleResource.resource().getId();
    ResourceModel model = findOrCreateModel(resource, parentReference != null ? parentReference : project);
    model.setRootId(moduleId);
    model = session.save(model);
    resource.setId(model.getId());
    resource.setUuid(model.getUuid());

    Snapshot parentSnapshot;
    if (parentReference != null) {
      parentSnapshot = resourceCache.get(parentReference.getEffectiveKey()).snapshot();
    } else {
      parentSnapshot = moduleResource.snapshot();
    }

    Snapshot snapshot = new Snapshot(model, parentSnapshot);
    snapshot.setBuildDate(new Date());
    snapshot = session.save(snapshot);
    session.commit();
    return snapshot;
  }

  private ResourceModel findOrCreateModel(Resource resource, @Nullable Resource parentResource) {
    ResourceModel model;
    try {
      model = session.getSingleResult(ResourceModel.class, "key", resource.getEffectiveKey());
      if (model == null) {
        if (StringUtils.isBlank(resource.getEffectiveKey())) {
          throw new SonarException("Unable to persist resource " + resource.toString() + ". Resource effective key is blank. This may be caused by an outdated plugin.");
        }
        model = createModel(resource, parentResource);

      } else {
        mergeModel(model, resource);
      }
      updateUuids(resource, parentResource, model);
      return model;

    } catch (NonUniqueResultException e) {
      throw new SonarException("The resource '" + resource.getEffectiveKey() + "' is duplicated in database.", e);
    }
  }

  ResourceModel createModel(Resource resource, @Nullable Resource parentResource) {
    ResourceModel model = new ResourceModel();
    model.setEnabled(Boolean.TRUE);
    model.setDescription(resource.getDescription());
    model.setKey(resource.getEffectiveKey());
    model.setUuid(Uuids.create());
    model.setPath(resource.getPath());
    Language language = resource.getLanguage();
    if (language != null) {
      model.setLanguageKey(language.getKey());
    }
    if (StringUtils.isNotBlank(resource.getName())) {
      model.setName(resource.getName());
    } else {
      model.setName(resource.getKey());
    }
    model.setLongName(resource.getLongName());
    model.setScope(resource.getScope());
    model.setQualifier(resource.getQualifier());
    return model;
  }

  private void updateUuids(Resource resource, Resource parentResource, ResourceModel model) {
    // Don't override uuids when persisting a library and a project already exists
    if (ResourceUtils.isLibrary(resource) && !Qualifiers.LIBRARY.equals(model.getQualifier())) {
      return;
    }
    if (parentResource != null) {
      ResourceModel parentModel = session.getSingleResult(ResourceModel.class, "id", parentResource.getId());
      if (parentModel.getProjectUuid() != null) {
        model.setProjectUuid(parentModel.getProjectUuid());
      } else {
        model.setProjectUuid(parentModel.getUuid());
      }
      if (Scopes.isProject(parentResource)) {
        model.setModuleUuid(parentResource.getUuid());
        String parentModuleUuidPath = parentModel.getModuleUuidPath();
        model.setModuleUuidPath(StringUtils.isNotBlank(parentModuleUuidPath) ? parentModuleUuidPath + "." + parentModel.getUuid() : parentModel.getUuid());
      } else {
        model.setModuleUuid(parentModel.getModuleUuid());
        String parentModuleUuidPath = parentModel.getModuleUuidPath();
        model.setModuleUuidPath(StringUtils.isNotBlank(parentModuleUuidPath) ? parentModuleUuidPath : parentModel.getUuid());
      }
    } else {
      // Root module && libraries
      model.setProjectUuid(model.getUuid());
      model.setModuleUuidPath("");
    }
  }

  static void mergeModel(ResourceModel model, Resource resource) {
    model.setEnabled(true);
    model.setKey(resource.getEffectiveKey());
    if (StringUtils.isNotBlank(resource.getName())) {
      model.setName(resource.getName());
    }
    if (StringUtils.isNotBlank(resource.getLongName())) {
      model.setLongName(resource.getLongName());
    }
    if (StringUtils.isNotBlank(resource.getDescription())) {
      model.setDescription(resource.getDescription());
    }
    if (StringUtils.isNotBlank(resource.getPath())) {
      model.setPath(resource.getPath());
    }
    if (!ResourceUtils.isLibrary(resource)) {
      model.setScope(resource.getScope());
      model.setQualifier(resource.getQualifier());
    }
    Language language = resource.getLanguage();
    if (language != null) {
      model.setLanguageKey(language.getKey());
    }
  }
}
