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
package org.sonar.batch.protocol.input.issues;

import javax.annotation.Nullable;

public class PreviousIssue {

  private String key;
  private String componentPath;
  private String ruleKey;
  private String ruleRepo;
  private Integer line;
  private String message;
  private String severity;
  private String resolution;
  private String status;
  private String checksum;
  private String assigneeLogin;
  private String assigneeFullname;

  public PreviousIssue setKey(String key) {
    this.key = key;
    return this;
  }

  public String key() {
    return key;
  }

  public PreviousIssue setComponentPath(@Nullable String path) {
    this.componentPath = path;
    return this;
  }

  public String componentPath() {
    return componentPath;
  }

  public PreviousIssue setLine(Integer line) {
    this.line = line;
    return this;
  }

  public Integer line() {
    return line;
  }

  public PreviousIssue setMessage(String message) {
    this.message = message;
    return this;
  }

  public String message() {
    return message;
  }

  public PreviousIssue setResolution(String resolution) {
    this.resolution = resolution;
    return this;
  }

  public String resolution() {
    return resolution;
  }

  public PreviousIssue setStatus(String status) {
    this.status = status;
    return this;
  }

  public String status() {
    return status;
  }

  public PreviousIssue setSeverity(String severity) {
    this.severity = severity;
    return this;
  }

  public String severity() {
    return severity;
  }

  public PreviousIssue setChecksum(String checksum) {
    this.checksum = checksum;
    return this;
  }

  public String checksum() {
    return checksum;
  }

  public PreviousIssue setAssigneeLogin(String assigneeLogin) {
    this.assigneeLogin = assigneeLogin;
    return this;
  }

  public String assigneeLogin() {
    return assigneeLogin;
  }

  public PreviousIssue setAssigneeFullname(String assigneeFullname) {
    this.assigneeFullname = assigneeFullname;
    return this;
  }

  public String assigneeFullname() {
    return assigneeFullname;
  }

  public PreviousIssue setRuleKey(String ruleRepo, String ruleKey) {
    this.ruleRepo = ruleRepo;
    this.ruleKey = ruleKey;
    return this;
  }

  public String ruleRepo() {
    return ruleRepo;
  }

  public String ruleKey() {
    return ruleKey;
  }

}
