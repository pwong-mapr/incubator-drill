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

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.log4j.Logger;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;

/**
 * This is the test base class that defines test procedures. All test
 * implementations should extend this class and implement their own getCluster()
 * to retrieve cluster information.
 * 
 * @author Zhiyong Liu
 * 
 */
public class DrillTestBase {
  protected static final String TRACK_RESOURCES_HOME_LABEL = "TRACK_RESOURCES_HOME";
  protected static final String START_RESRC_TRACKING_CMD_LABEL = "START_RESRC_TRACKING_CMD";
  protected static final String STOP_RESRC_TRACKING_CMD_LABEL = "STOP_RESRC_TRACKING_CMD";
  protected String trackingSessionUID = null;
  protected static final Logger LOG = Logger.getLogger(Utils
      .getInvokingClassName());
  private InputQueryFileHandler handler = null;
  private DrillQueryDispatcher dispatcher = null;
  private static final String CONGREGATED_FILENAME = "/tmp/congregated.q";
  private DateFormat dateFormat = new SimpleDateFormat(
      "yyyy/MM/dd HH:mm:ss.ssss");
  private static Map<String, String> drillProperties = Utils
      .getDrillTestProperties();
  private static final String SQLLINE_COMMAND = drillProperties
      .get("DRILL_HOME") + "/bin/sqlline";
  private static final String SUBMIT_PLAN_COMMAND = drillProperties
      .get("DRILL_HOME") + "/bin/submit_plan";
  private static final int TIME_OUT_SECONDS = Integer.parseInt(drillProperties
      .get("TIME_OUT_SECONDS"));
  // [Kunal] Adding support for resource tracking
  protected static final String TRACK_RESRC_HOME = drillProperties
      .get(TRACK_RESOURCES_HOME_LABEL);
  protected static final String START_RESRC_TRACKING_CMD = drillProperties
      .get(START_RESRC_TRACKING_CMD_LABEL);
  protected static final String STOP_RESRC_TRACKING_CMD = drillProperties
      .get(STOP_RESRC_TRACKING_CMD_LABEL);
  private Connection connection = null;
  private Statement statement = null;
  private ResultSet resultSet = null;
  private static Map<String, Pair> connectionMap = new HashMap<String, Pair>();

  @BeforeClass
  public void beforeClass() throws SQLException, ClassNotFoundException {
    Class.forName("org.apache.drill.jdbc.Driver");
  }

  @AfterClass
  public void afterClass() throws SQLException {
    if (resultSet != null) {
      resultSet.close();
    }
    for (Map.Entry<String, Pair> entry : connectionMap.entrySet()) {
      Pair pair = entry.getValue();
      if (pair.getStatement() != null) {
        pair.getStatement().close();
      }
      if (pair.getConnection() != null) {
        pair.getConnection().close();
      }
    }
  }

  /**
   * Processes TestCaseModeler and passes information contained therein for test
   * execution.
   * 
   * @param modeler
   *          TestCaseModeler to run a test with.
   * @throws Exception
   */
  protected void runTest(TestCaseModeler modeler) throws Exception {
    List<TestCaseModeler.TestMatrix> matrices = modeler.getMatrices();
    for (TestCaseModeler.TestMatrix matrix : matrices) {
      String schema = matrix.getSchema();
      if (!connectionMap.containsKey(schema)) {
        String url = "jdbc:drill:schema=" + schema + ";zk="
            + Utils.getDrillTestProperties().get("ZOOKEEPERS");
        LOG.info("Connecting to " + url);
        connection = DriverManager.getConnection(url);
        statement = connection.createStatement();
        connectionMap.put(schema, new Pair(connection, statement));
      }
    }
    String[] inputFileNames = new String[matrices.size()];
    String[] schemas = new String[matrices.size()];
    String[] outputFormats = new String[matrices.size()];
    String[] expectedFiles = new String[matrices.size()];
    String[] verificationTypes = new String[matrices.size()];
    int i = 0;
    for (TestCaseModeler.TestMatrix matrix : matrices) {
      inputFileNames[i] = matrix.getInputFile();
      schemas[i] = matrix.getSchema();
      outputFormats[i] = matrix.getOutputFormat();
      expectedFiles[i] = matrix.getExpectedFile();
      if (matrix.getVerificationTypes().size() > 0) {
        verificationTypes[i] = matrix.getVerificationTypes().get(0);
      }
      i++;
    }
    List<TestCaseModeler.DataSource> datasources = modeler.getDatasources();
    prepareData(datasources);
    testProcess(modeler.getTestId(), modeler.getDescription(),
        modeler.getSubmitType(), modeler.getQueryType(), inputFileNames,
        outputFormats, expectedFiles, schemas, verificationTypes);
    if (resultSet != null) {
      resultSet.close();
    }
  }

  private void prepareData(List<TestCaseModeler.DataSource> datasources)
      throws IOException {
    if (datasources == null) {
      return;
    }
    for (TestCaseModeler.DataSource datasource : datasources) {
      String mode = datasource.getMode();
      if (mode.equals("cp")) {
        hdfsCopy(datasource.getSource(), datasource.getDestination(), false);
      } else if (mode.equals("gen")) {
        int exitCode = 0;
        try {
          exitCode = Runtime.getRuntime().exec(datasource.getSource())
              .waitFor();
        } catch (Exception e) {
          LOG.error("Error: Failed to execute the command "
              + datasource.getSource() + ".");
        }
        if (exitCode != 0) {
          throw new RuntimeException("Error executing the command "
              + datasource.getSource() + " has return code " + exitCode);
        }
      }
    }
  }

  private void testProcess(String testId, String testDesc, String submitType,
      String queryType, String[] inputFileNames, String[] outputFormats,
      String[] expectedFiles, String[] schemas, String[] verificationTypes)
      throws Exception {
    boolean verified = true;
    boolean timedOut = false;
    logTestStart(testId, testDesc);
    String[] outputFileNames = generateOutputFileNames(inputFileNames, testId);
    handler = new InputQueryFileHandler(inputFileNames, outputFileNames,
        outputFormats);
    String queries = handler.congregateFiles(CONGREGATED_FILENAME);
    LOG.info("Submitting queries:\n" + queries);
    dispatcher = new DrillQueryDispatcher(schemas[0]);
    Date date1 = new Date();
    List<Map<String, Integer>> resultSets = new ArrayList<Map<String, Integer>>();
    LOG.info("Query dispatch start time: " + dateFormat.format(date1));
    RunThread runThread = new RunThread(submitType, inputFileNames,
        outputFileNames, queryType, resultSets, verificationTypes);
    runThread.start();
    runThread.join(TIME_OUT_SECONDS * 1000);
    if (runThread.isAlive()) {
      LOG.warn("Drill didn't complete query within " + TIME_OUT_SECONDS
          + " seconds.");
      verified = false;
      timedOut = true;
      runThread.interrupt();
      logTestEnd(testId, verified, timedOut);
      return;
    }
    Date date2 = new Date();
    LOG.info("Query dispatch end time: " + dateFormat.format(date2));
    LOG.info("The execution time for the query: "
        + Utils.getDateDiff(date2, date1, "second") + " seconds.");
    if (submitType.equals("sqlline")) {
      verified = true;
    } else if (submitType.equals("submit_plan")) {
      verified = verified
          && verifySubmitPlanSubmitType(expectedFiles, outputFileNames);
    } else if (submitType.equals("jdbc")) {
      if (verificationTypes != null && verificationTypes.length != 0
          && !verificationTypes[0].equalsIgnoreCase("none")) {
        verified = verified && verifyJdbcSubmitType(expectedFiles, resultSets);
      }
    }
    logTestEnd(testId, verified, timedOut);
  }

  private class RunThread extends Thread {
    String submitType;
    String[] inputFileNames;
    String[] outputFileNames;
    String queryType;
    List<Map<String, Integer>> resultSets;
    String[] verificationTypes;

    public RunThread(String submitType, String[] inputFileNames,
        String[] outputFileNames, String queryType,
        List<Map<String, Integer>> resultSets, String[] verificationTypes) {
      this.submitType = submitType;
      this.inputFileNames = inputFileNames;
      this.outputFileNames = outputFileNames;
      this.queryType = queryType;
      this.resultSets = resultSets;
      this.verificationTypes = verificationTypes;
    }

    @Override
    public void run() {
      try {
        if (submitType.equals("sqlline")) {
          dispatcher.dispatchQueriesCLI(SQLLINE_COMMAND, CONGREGATED_FILENAME);
        } else {
          for (int i = 0; i < inputFileNames.length; i++) {
            if (submitType.equals("submit_plan")) {
              dispatcher.dispatchQueriesSubmitPlan(SUBMIT_PLAN_COMMAND,
                  inputFileNames[i], outputFileNames[i], queryType);
            } else if (submitType.equals("jdbc")) {
              if (verificationTypes[i] == null
                  || verificationTypes[i].equalsIgnoreCase("none")) {
                dispatcher.executeQueryJDBC(inputFileNames[i], statement);
              }
              resultSets.add(dispatcher.dispatchQueryJDBC(inputFileNames[i],
                  statement));
            }
          }
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

  private String[] generateOutputFileNames(String[] inputFileNames,
      String testId) {
    String[] outputFileNames = new String[inputFileNames.length];
    for (int i = 0; i < outputFileNames.length; i++) {
      String inputFileName = inputFileNames[i];
      int index = inputFileName.lastIndexOf('/');
      String queryName = inputFileName.substring(index + 1);
      queryName = queryName.split("\\.")[0];
      outputFileNames[i] = "/tmp/" + testId + "_" + queryName + ".output";
    }
    return outputFileNames;
  }

  private boolean verifySubmitPlanSubmitType(String[] expectedOutputs,
      String[] actualOutputs) throws IOException, InterruptedException {
    boolean verified = true;
    for (int i = 0; i < expectedOutputs.length; i++) {
      verified = verified
          && TestVerifier.fileComparisonVerify(expectedOutputs[i],
              actualOutputs[i]);
      if (!verified) {
        return verified;
      }
    }
    return true;
  }

  private boolean verifyJdbcSubmitType(String[] expectedFiles,
      List<Map<String, Integer>> resultSets) throws IOException, SQLException {
    if (resultSets.isEmpty()) {
      return false;
    }
    boolean verified = true;
    for (int i = 0; i < expectedFiles.length; i++) {
      verified = verified
          && TestVerifier.resultSetVerify(expectedFiles[i], resultSets.get(i));
      if (!verified) {
        return verified;
      }
    }
    return true;
  }

  private void logTestStart(String testId, String testDesc) throws IOException {
    LOG.info("+===========================================");
  }

  private void logTestEnd(String testId, boolean verified, boolean timedOut) {
    if (verified) {
      LOG.info("Test " + testId + " was successfully verified.");
    } else {
      if (!timedOut) {
        Assert.fail("Verification of test " + testId + " failed.");
      } else {
        Assert.fail("Test failed through timeout.");
      }
    }
    LOG.info("Test " + testId + " completed.");
    LOG.info("+===========================================");
  }

  private void hdfsCopy(String src, String dest, boolean overWrite)
      throws IOException {
    FileSystem fs = FileSystem.get(new Configuration());
    Path srcPath = new Path(src);
    Path destPath = new Path(dest);
    if (!fs.exists(destPath) || overWrite) {
      fs.copyFromLocalFile(false, overWrite, srcPath, destPath);
    } else {
      LOG.info("The source file " + src
          + " already exists in destination.  Skipping the copy.");
    }
  }

  private class Pair {
    private Connection connection;
    private Statement statement;

    public Pair(Connection connection, Statement statement) {
      this.connection = connection;
      this.statement = statement;
    }

    public Connection getConnection() {
      return connection;
    }

    public Statement getStatement() {
      return statement;
    }
  }
}