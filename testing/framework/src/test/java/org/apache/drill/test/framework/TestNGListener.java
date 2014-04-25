/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.drill.test.framework;

import org.apache.log4j.Logger;
import org.testng.ITestContext;
import org.testng.ITestResult;
import org.testng.TestListenerAdapter;

/**
 * An implementation of TestNG listener to monitor drill test status.
 * 
 * @author Zhiyong Liu
 */
public class TestNGListener extends TestListenerAdapter {
  private static final Logger LOG = Logger.getLogger(Utils
      .getInvokingClassName());

  @Override
  public void onStart(ITestContext testContext) {
    LOG.info(testContext.getName() + " Started");
  }

  @Override
  public void onFinish(ITestContext testContext) {
    LOG.info(testContext.getName() + " Finished");
  }

  @Override
  public void onTestStart(ITestResult tr) {
    LOG.info(tr.getName() + " Started");
  }

  @Override
  public void onTestSuccess(ITestResult tr) {
    LOG.info(tr.getName() + " Succeeded");
  }

  @Override
  public void onTestFailure(ITestResult tr) {
    LOG.info(tr.getName() + " Failed");
  }

  @Override
  public void onTestSkipped(ITestResult tr) {
    LOG.info(tr.getName() + " Skipped");
  }
}
