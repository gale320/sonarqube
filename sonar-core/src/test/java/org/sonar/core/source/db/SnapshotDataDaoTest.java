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

package org.sonar.core.source.db;

import com.google.common.collect.Lists;
import org.junit.Before;
import org.junit.Test;
import org.sonar.core.persistence.AbstractDaoTestCase;

import java.util.Collection;

import static org.assertj.core.api.Assertions.assertThat;

public class SnapshotDataDaoTest extends AbstractDaoTestCase {

  private SnapshotDataDao dao;

  @Before
  public void createDao() {
    dao = new SnapshotDataDao(getMyBatis());
    setupData("shared");
  }

  @Test
  public void select_snapshot_data_by_snapshot_id() throws Exception {
    Collection<SnapshotDataDto> data = dao.selectSnapshotData(10L, Lists.newArrayList("highlight_syntax", "symbol"));

    assertThat(data).extracting("snapshotId").containsOnly(10L, 10L);
    assertThat(data).extracting("dataType").containsOnly("highlight_syntax", "symbol");
    assertThat(data).extracting("data").containsOnly("0,10,k;", "20,25,20,35,45;");
  }

  @Test
  public void serialize_snapshot_data() throws Exception {
    String data = "0,10,k;";
    String dataType = "highlight_syntax";

    SnapshotDataDto dto = new SnapshotDataDto();
    dto.setResourceId(1L);
    dto.setSnapshotId(11L);
    dto.setData(data);
    dto.setDataType(dataType);

    dao.insert(dto);

    Collection<SnapshotDataDto> serializedData = dao.selectSnapshotData(11L, Lists.newArrayList("highlight_syntax"));

    assertThat(serializedData).extracting("snapshotId").containsOnly(11L);
    assertThat(serializedData).extracting("dataType").containsOnly(dataType);
    assertThat(serializedData).extracting("data").containsOnly(data);
  }

  @Test
  public void select_snapshot_data_by_project_id() throws Exception {
    Collection<SnapshotDataDto> data = dao.selectSnapshotDataByComponentKey("org.apache.struts:struts:Dispatcher", Lists.newArrayList("highlight_syntax", "symbol"));

    assertThat(data).isNotEmpty();
    assertThat(data).extracting("snapshotId").containsOnly(10L, 10L);
    assertThat(data).extracting("dataType").containsOnly("highlight_syntax", "symbol");
    assertThat(data).extracting("data").containsOnly("0,10,k;", "20,25,20,35,45;");
  }
}
