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

package org.sonar.server.computation;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.DisableOnDebug;
import org.junit.rules.TestRule;
import org.junit.rules.Timeout;
import org.sonar.api.platform.Server;

import java.util.concurrent.TimeUnit;

import static org.mockito.Mockito.*;

public class ComputationWorkerLauncherTest {

  @Rule
  public TestRule timeout = new DisableOnDebug(Timeout.seconds(5));

  private ComputationWorkerLauncher sut;
  private ComputationService service;
  private AnalysisReportQueue queue;

  @Before
  public void before() {
    this.service = mock(ComputationService.class);
    this.queue = mock(AnalysisReportQueue.class);
  }

  @After
  public void after() {
    sut.stop();
  }

  @Test
  public void call_findAndBook_when_launching_a_recurrent_task() throws Exception {
    sut = new ComputationWorkerLauncher(service, queue, 0, 1, TimeUnit.MILLISECONDS);

    sut.onServerStart(mock(Server.class));

    sleep();

    verify(queue, atLeastOnce()).pop();
  }

  @Test
  public void call_findAndBook_when_executing_task_immediately() throws Exception {
    sut = new ComputationWorkerLauncher(service, queue, 1, 1, TimeUnit.HOURS);
    sut.start();

    sut.startAnalysisTaskNow();

    sleep();

    verify(queue, atLeastOnce()).pop();
  }

  private void sleep() throws InterruptedException {
    TimeUnit.MILLISECONDS.sleep(500L);
  }
}
