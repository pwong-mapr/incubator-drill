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

import org.testng.annotations.Test;

/**
 * Test client for all drill tests in a MapR cluster.
 * 
 * @author Zhiyong Liu
 * 
 */
public class DrillTestsMapRCluster extends DrillTestBase {
  @Test(dataProviderClass = JsonTestDataProvider.class, dataProvider = "jsonTestDataProvider")
  public void positiveTests(TestCaseModeler modeler) throws Exception {
    LOG.debug(modeler);
    runTest(modeler);
  }
}