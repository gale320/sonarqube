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
package org.sonar.batch.protocol.input;

import org.sonar.batch.protocol.GsonHelper;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import java.util.*;

/**
 * Container for all project data going from server to batch.
 * This is not an API since server and batch always share the same version.
 */
public class ProjectReferentials {

  private long timestamp;
  private Map<String, QProfile> qprofilesByLanguage = new HashMap<String, QProfile>();
  private Collection<ActiveRule> activeRules = new ArrayList<ActiveRule>();
  private Map<String, Map<String, String>> settingsByModule = new HashMap<String, Map<String, String>>();
  private Map<String, Map<String, FileData>> fileDataByModuleAndPath = new HashMap<String, Map<String, FileData>>();
  private Date lastAnalysisDate;

  public Map<String, String> settings(String moduleKey) {
    return settingsByModule.containsKey(moduleKey) ? settingsByModule.get(moduleKey) : Collections.<String, String>emptyMap();
  }

  public ProjectReferentials addSettings(String moduleKey, Map<String, String> settings) {
    Map<String, String> existingSettings = settingsByModule.get(moduleKey);
    if (existingSettings == null) {
      existingSettings = new HashMap<>();
      settingsByModule.put(moduleKey, existingSettings);
    }
    existingSettings.putAll(settings);
    return this;
  }

  public Collection<QProfile> qProfiles() {
    return qprofilesByLanguage.values();
  }

  public ProjectReferentials addQProfile(QProfile qProfile) {
    qprofilesByLanguage.put(qProfile.language(), qProfile);
    return this;
  }

  public Collection<ActiveRule> activeRules() {
    return activeRules;
  }

  public ProjectReferentials addActiveRule(ActiveRule activeRule) {
    activeRules.add(activeRule);
    return this;
  }

  public Map<String, FileData> fileDataByPath(String moduleKey) {
    return fileDataByModuleAndPath.containsKey(moduleKey) ? fileDataByModuleAndPath.get(moduleKey) : Collections.<String, FileData>emptyMap();
  }

  public ProjectReferentials addFileData(String moduleKey, String path, FileData fileData) {
    Map<String, FileData> existingFileDataByPath = fileDataByModuleAndPath.get(moduleKey);
    if (existingFileDataByPath == null) {
      existingFileDataByPath = new HashMap<>();
      fileDataByModuleAndPath.put(moduleKey, existingFileDataByPath);
    }
    existingFileDataByPath.put(path, fileData);
    return this;
  }

  @CheckForNull
  public FileData fileData(String projectKey, String path) {
    return fileDataByPath(projectKey).get(path);
  }

  public long timestamp() {
    return timestamp;
  }

  public void setTimestamp(long timestamp) {
    this.timestamp = timestamp;
  }

  @CheckForNull
  public Date lastAnalysisDate() {
    return lastAnalysisDate;
  }

  public void setLastAnalysisDate(@Nullable Date lastAnalysisDate) {
    this.lastAnalysisDate = lastAnalysisDate;
  }

  public String toJson() {
    return GsonHelper.create().toJson(this);
  }

  public static ProjectReferentials fromJson(String json) {
    return GsonHelper.create().fromJson(json, ProjectReferentials.class);
  }

}
