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

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

public class FileData {

  private final String hash;
  private final boolean needBlame;
  private final String scmLastCommitDatetimesByLine;
  private final String scmRevisionsByLine;
  private final String scmAuthorsByLine;

  public FileData(@Nullable String hash, boolean needBlame, @Nullable String scmLastCommitDatetimesByLine, @Nullable String scmRevisionsByLine, @Nullable String scmAuthorsByLine) {
    this.hash = hash;
    this.needBlame = needBlame;
    this.scmLastCommitDatetimesByLine = scmLastCommitDatetimesByLine;
    this.scmRevisionsByLine = scmRevisionsByLine;
    this.scmAuthorsByLine = scmAuthorsByLine;
  }

  @CheckForNull
  public String hash() {
    return hash;
  }

  public boolean needBlame() {
    return needBlame;
  }

  @CheckForNull
  public String scmLastCommitDatetimesByLine() {
    return scmLastCommitDatetimesByLine;
  }

  @CheckForNull
  public String scmRevisionsByLine() {
    return scmRevisionsByLine;
  }

  @CheckForNull
  public String scmAuthorsByLine() {
    return scmAuthorsByLine;
  }

}
