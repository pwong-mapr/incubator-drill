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

import java.io.PrintWriter;
import java.util.Scanner;

/**
 * Additional drill-specific query dispatching methods to the generic
 * dispatcher.
 * 
 * @author Zhiyong Liu
 * 
 */
public class DrillQueryDispatcher extends GenericQueryDispatcher {
  public DrillQueryDispatcher(String schema) {
    super(schema);
  }

  /**
   * Submit query via the submit_plan tool
   * 
   * @param submitPlanCommand
   *          command of submit_plan
   * @param queryFileName
   *          name of file containing input query
   * @param outputFileName
   *          name of file containing query results
   * @param queryType
   *          type of query: sql, logical or physical
   * @throws Exception
   */
  public void dispatchQueriesSubmitPlan(String submitPlanCommand,
      String queryFileName, String outputFileName, String queryType)
      throws Exception {
    String command = submitPlanCommand + " -f " + queryFileName
        + " --format tsv -t " + queryType + " -z "
        + Utils.getDrillTestProperties().get("ZOOKEEPERS");
    LOG.info("Executing " + command + ".");
    Process p = Runtime.getRuntime().exec(command);
    p.waitFor();
    Scanner scanner = new Scanner(p.getInputStream()).useDelimiter("\\A");
    String output = scanner.hasNext() ? scanner.next() : "";
    PrintWriter writer = new PrintWriter(outputFileName);
    writer.write(output);
    writer.close();
  }
}