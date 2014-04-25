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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

import org.apache.log4j.Logger;
import org.testng.annotations.Test;

/**
 * Test client for drill performance tests in a MapR cluster.
 * 
 * @author
 * 
 */
public class DrillPerformanceTests extends DrillTestsMapRCluster {
  String startTrackingCmdPrefix;
  String stopTrackingCmdPrefix;

  /**
   * Starts tracking resources with query label
   * 
   * @param label
   * @throws Exception
   */
  private void startResourceTracking(String label) throws Exception {
    if (TRACK_RESRC_HOME != null && TRACK_RESRC_HOME.length() > 0) {
      LOG.info(TRACK_RESOURCES_HOME_LABEL
          + " is set! Starting tracking of resources...");

      // Note: Ensure BOTH are specified. You dont want to start tracking and
      // fail to stop
      if (START_RESRC_TRACKING_CMD == null || STOP_RESRC_TRACKING_CMD == null) {
        LOG.error(START_RESRC_TRACKING_CMD_LABEL + " or "
            + STOP_RESRC_TRACKING_CMD_LABEL + " is not set!");
        stopTrackingCmdPrefix = null;
        return;
      }
      // Read StartTracking & StopTracking Commands from ???
      startTrackingCmdPrefix = START_RESRC_TRACKING_CMD;
      stopTrackingCmdPrefix = STOP_RESRC_TRACKING_CMD;

      // Constructing Command
      // [Kunal] None at the moment

      // Invoke Command in Shell (make this background process/thread?)
      executeShellCommand(startTrackingCmdPrefix + " " + label);

    } else {
      LOG.info(TRACK_RESOURCES_HOME_LABEL
          + " is not set. Will not be tracking resources");
    }

  }

  // Created to provide different Log info
  private static class Shell {
    protected static final Logger LOG = Logger.getLogger(Utils
        .getInvokingClassName());
  }

  /**
   * Allows for execution of shell calls
   * 
   * @param command
   */
  private void executeShellCommand(String command) {
    try {
      LOG.info("[Tracker] Executing: " + command);
      // Running as a Unix Shell command
      Process process = Runtime.getRuntime().exec("/bin/bash " + command);
      int status = process.waitFor();
      if (status > 0) {
        throw new Exception("Process failed to execute!");
      }
      String lineRead = "";
      BufferedReader reader = new BufferedReader(new InputStreamReader(
          process.getInputStream()));
      while ((lineRead = reader.readLine()) != null) {
        lineRead = lineRead.trim();
        // Capturing Uniq ID
        if (lineRead.startsWith("[UNIQ_ID]")) {
          trackingSessionUID = lineRead.substring("[UNIQ_ID]".length()).trim();
        }
        Shell.LOG.info("\t" + lineRead);
      }
      reader.close();
    } catch (IOException e) {
      e.printStackTrace();
    } catch (InterruptedException e) {
      e.printStackTrace();
    } catch (Exception e) {
    }
  }

  @Test(dataProviderClass = JsonTestDataProvider.class, dataProvider = "jsonTestDataProvider")
  public void positiveTests(TestCaseModeler modeler) throws Exception {
    LOG.debug(modeler);
    startResourceTracking(getLabel(modeler));
    runTest(modeler);
    stopResourceTracking();
  }

  /**
   * Returns a query label
   * 
   * @param modeler
   * @return
   */
  private String getLabel(TestCaseModeler modeler) {
    String basicFileName = (new File(modeler.getMatrices().get(0)
        .getInputFile())).getName();
    String queryLabel = basicFileName.subSequence(0,
        basicFileName.lastIndexOf(".q")).toString();
    return queryLabel;
  }

  /**
   * Stops tracking
   * 
   * @throws Exception
   */
  private void stopResourceTracking() throws Exception {
    if (stopTrackingCmdPrefix != null) {
      LOG.info("Stopping Resource tracking....!!");
      // Constructing Command
      // [Kunal] None at the moment

      // Invoke Command in Shell (make this background process/thread?)
      executeShellCommand(stopTrackingCmdPrefix);
    }
  }

}
