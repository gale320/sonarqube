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

package org.sonar.server.db.migrations.v51;

import com.google.common.base.CharMatcher;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import org.sonar.api.utils.System2;
import org.sonar.api.utils.text.CsvWriter;
import org.sonar.core.persistence.Database;
import org.sonar.server.db.migrations.BaseDataChange;
import org.sonar.server.db.migrations.Select;
import org.sonar.server.db.migrations.Upsert;
import org.sonar.server.db.migrations.UpsertImpl;
import org.sonar.server.util.ProgressLogger;

import java.io.StringWriter;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static com.google.common.collect.Lists.newArrayList;

public class CopyScmAccountsFromAuthorsToUsers extends BaseDataChange {

  private final System2 system;
  private final AtomicLong counter = new AtomicLong(0L);

  public CopyScmAccountsFromAuthorsToUsers(Database db, System2 system) {
    super(db);
    this.system = system;
  }

  @Override
  public void execute(final Context context) throws SQLException {
    ProgressLogger progress = ProgressLogger.create(getClass(), counter);
    progress.start();
    final Long now = system.now();

    try {
      final Multimap<Long, String> authorsByPersonId = ArrayListMultimap.create();
      context.prepareSelect("SELECT a.person_id, a.login FROM authors a," +
        "  (SELECT person_id, COUNT(*) AS nb FROM authors GROUP BY person_id HAVING COUNT(*) > 1) group_by_person" +
          "     WHERE a.person_id = group_by_person.person_id "
      ).scroll(new Select.RowHandler() {
        @Override
        public void handle(Select.Row row) throws SQLException {
          authorsByPersonId.put(row.getLong(1), row.getString(2));
        }
      });

      Upsert update = context.prepareUpsert("UPDATE users SET scm_accounts = ?, updated_at = ? WHERE id = ?");
      for (Long personId : authorsByPersonId.keySet()) {
        List<String> authors = newArrayList(authorsByPersonId.get(personId));
        List<User> users = selectUsersFromLoginOrEmail(context, authors);
        if (users.size() == 1) {
          User user = users.get(0);
          if (authors.contains(user.login)) {
            authors.remove(user.login);
          }
          if (authors.contains(user.email)) {
            authors.remove(user.email);
          }
          if (!authors.isEmpty()) {
            update
              .setString(1, convertScmAccountsToCsv(authors))
              .setLong(2, now)
              .setLong(3, user.id)
              .addBatch();
            counter.getAndIncrement();
          }
        }
      }
      if (((UpsertImpl) update).getBatchCount() > 0L) {
        update.execute().commit();
      }
      update.close();

      progress.log();
    } finally {
      progress.stop();
    }
  }

  private List<User> selectUsersFromLoginOrEmail(Context context, Collection<String> authors) throws SQLException {
    final List<User> users = newArrayList();
    StringBuilder sql = new StringBuilder("SELECT u.id, u.login, u.email, u.scm_accounts FROM users u WHERE u.active=? AND (");
    for (int i = 0; i < authors.size(); i++) {
      if (i < authors.size() - 1) {
        sql.append("u.login=? OR u.email=? OR ");
      } else {
        sql.append("u.login=? OR u.email=?)");
      }
    }
    Select select = context.prepareSelect(sql.toString());
    select.setBoolean(1, true);
    int currentIndex = 1;
    for (String author : authors) {
      currentIndex++;
      select.setString(currentIndex, author);
      currentIndex++;
      select.setString(currentIndex, author);
    }

    select.scroll(new Select.RowHandler() {
      @Override
      public void handle(Select.Row row) throws SQLException {
        users.add(new User(row.getLong(1), row.getString(2), row.getString(3), row.getString(4)));
      }
    });
    return users;
  }

  private static String convertScmAccountsToCsv(List<String> scmAccounts) {
    List<String> list = newArrayList(scmAccounts);
    // Add one empty character at the begin and at the end of the list to be able to generate a coma at the begin and at the end of the
    // text
    list.add(0, "");
    list.add("");
    int size = list.size();
    StringWriter writer = new StringWriter(size);
    CsvWriter csv = CsvWriter.of(writer);
    csv.values(list.toArray(new String[size]));
    csv.close();
    // Remove useless line break added by CsvWriter at this end of the text
    return CharMatcher.JAVA_ISO_CONTROL.removeFrom(writer.toString());
  }

  private static class User {
    Long id;
    String login;
    String email;
    String scmAcounts;

    User(Long id, String login, String email, String scmAcounts) {
      this.id = id;
      this.login = login;
      this.email = email;
      this.scmAcounts = scmAcounts;
    }
  }
}
