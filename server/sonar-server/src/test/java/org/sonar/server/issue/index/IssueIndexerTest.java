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
package org.sonar.server.issue.index;

import com.google.common.collect.Iterators;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.sonar.api.config.Settings;
import org.sonar.api.rule.RuleKey;
import org.sonar.core.persistence.DbTester;
import org.sonar.server.db.DbClient;
import org.sonar.server.es.EsTester;
import org.sonar.test.DbTests;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Category(DbTests.class)
public class IssueIndexerTest {

  @Rule
  public DbTester dbTester = new DbTester();

  @Rule
  public EsTester esTester = new EsTester().addDefinitions(new IssueIndexDefinition(new Settings()));

  @Test
  public void index_nothing() throws Exception {
    IssueIndexer indexer = createIndexer();
    indexer.index(Iterators.<IssueDoc>emptyIterator());
    assertThat(esTester.countDocuments(IssueIndexDefinition.INDEX, IssueIndexDefinition.TYPE_ISSUE)).isEqualTo(0L);
  }

  @Test
  public void index() throws Exception {
    dbTester.prepareDbUnit(getClass(), "index.xml");

    IssueIndexer indexer = createIndexer();
    indexer.index();

    List<IssueDoc> docs = esTester.getDocuments("issues", "issue", IssueDoc.class);
    assertThat(docs).hasSize(1);
    IssueDoc doc = docs.get(0);
    assertThat(doc.projectUuid()).isEqualTo("THE_PROJECT");
    assertThat(doc.componentUuid()).isEqualTo("THE_FILE");
    assertThat(doc.severity()).isEqualTo("BLOCKER");
    assertThat(doc.ruleKey()).isEqualTo(RuleKey.of("squid", "AvoidCycles"));

    // delete project
    indexer.deleteProject("THE_PROJECT", true);

    assertThat(esTester.countDocuments("issues", "issue")).isZero();
  }

  private IssueIndexer createIndexer() {
    return new IssueIndexer(new DbClient(dbTester.database(), dbTester.myBatis()), esTester.client());
  }
}
