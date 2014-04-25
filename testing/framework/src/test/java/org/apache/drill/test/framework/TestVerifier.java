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
import java.io.FileReader;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

/**
 * Verification of drill tests by comparing actual query output with expected
 * query result set.
 * 
 * @author Zhiyong Liu
 */
public class TestVerifier {
  private static final int MAX_MISMATCH_SIZE = 10;
  protected static final Logger LOG = Logger.getLogger(Utils
      .getInvokingClassName());

  /**
   * Verifies output of a query. The verification is performed by comparing the
   * contents of the expected and actual query result files.
   * 
   * @param expectedOutput
   *          name of file containing expected output data
   * @param actualOutput
   *          name of file containing actual output data
   * @return true if the outputs are identical
   * @throws IOException
   * @throws InterruptedException
   */
  public static boolean fileComparisonVerify(String expectedOutput,
      String actualOutput) throws IOException,
      InterruptedException {
    Map<String, Integer> expectedMap = getExpectedMap(expectedOutput);
    List<String> unexpectedList = new ArrayList<String>();
    BufferedReader reader = new BufferedReader(new FileReader(new File(
        actualOutput)));
    String line = reader.readLine();
    String record = reader.readLine().trim();
    int unexpectedCount = 0;
    while ((line = reader.readLine()) != null) {
      if (!check(expectedMap, record)) {
        if (unexpectedCount < MAX_MISMATCH_SIZE) {
          unexpectedList.add(record);
          unexpectedCount++;
        }
      }
      record = line.trim();
    }
    reader.close();
    printSummary(unexpectedList, unexpectedCount, expectedMap);
    return expectedMap.isEmpty() && unexpectedList.isEmpty();
  }

  /**
   * Verifies output of a query. The verification is performed using map-reduce.
   * This is used when the query output does not have consistent ordering, and
   * is too large for in-memory verifications.
   * 
   * @param expectedOutput
   *          name of file containing expected output data
   * @param actualOutput
   *          name of file containing actual output data
   * @return true if the outputs are identical
   * @throws IOException
   */
  // TODO need to implement this
  public static boolean mapReduceVerify(String expectedOutput,
      String actualOutput) throws IOException {
    return false;
  }

  public static boolean resultSetVerify(String expectedFile,
      Map<String, Integer> actualMap) throws IOException, SQLException {
    if (actualMap == null) {
      LOG.warn("ResultSet is null from query.");
      return false;
    }
    Map<String, Integer> expectedMap = getExpectedMap(expectedFile);
    Map<String, Integer> unexpectedMap = new HashMap<String, Integer>();
    for (Map.Entry<String, Integer> entry : actualMap.entrySet()) {
      String key = entry.getKey();
      if (expectedMap.containsKey(key)) {
        int diff = expectedMap.get(key) - entry.getValue();
        if (diff == 0) {
          expectedMap.remove(key);
        } else if (diff > 0) {
          expectedMap.put(key, expectedMap.get(key) - 1);
        } else {
          if (!unexpectedMap.containsKey(key)) {
            unexpectedMap.put(key, Math.abs(diff));
          } else {
            unexpectedMap.put(key, unexpectedMap.get(key) + Math.abs(diff));
          }
        }
      } else {
        if (unexpectedMap.containsKey(key)) {
          unexpectedMap.put(key, unexpectedMap.get(key) + entry.getValue());
        } else {
          unexpectedMap.put(key, entry.getValue());
        }
      }
    }
    if (unexpectedMap.isEmpty() && expectedMap.isEmpty()) {
      return true;
    } else {
      printSummary(unexpectedMap, expectedMap);
      return false;
    }
  }

  private static Map<String, Integer> getExpectedMap(String filename)
      throws IOException {
    Map<String, Integer> map = new HashMap<String, Integer>();
    BufferedReader reader = new BufferedReader(new FileReader(filename));
    String line = "";
    while ((line = reader.readLine()) != null) {
      line.trim();
      if (map.containsKey(line)) {
        map.put(line, map.get(line) + 1);
      } else {
        map.put(line, 1);
      }
    }
    reader.close();
    return map;
  }

  private static boolean check(Map<String, Integer> map, String entry) {
    if (map.containsKey(entry)) {
      map.put(entry, map.get(entry) - 1);
      if (map.get(entry) == 0) {
        map.remove(entry);
      }
      return true;
    }
    return false;
  }

  private static void printSummary(List<String> unexpectedList,
      int unexpectedCount, Map<String, Integer> expectedMap) {
    if (!unexpectedList.isEmpty()) {
      LOG.info("These rows are not expected:");
      for (String row : unexpectedList) {
        LOG.info("\t" + row);
      }
      LOG.info("Total number of unexpected rows: " + unexpectedCount);
    }
    unexpectedCount = expectedMap.size();
    if (!expectedMap.isEmpty()) {
      LOG.info("These rows are expected but are not in result set:");
      int count = 0;
      for (Map.Entry<String, Integer> entry : expectedMap.entrySet()) {
        LOG.info("\t" + entry.getKey() + " (" + entry.getValue() + " time(s))");
        count++;
        if (count == MAX_MISMATCH_SIZE) {
          break;
        }
      }
      LOG.info("Total number of expected but missing: " + unexpectedCount);
    }
  }

  private static void printSummary(Map<String, Integer> unexpectedMap,
      Map<String, Integer> expectedMap) {
    int count = 0;
    if (!unexpectedMap.isEmpty()) {
      LOG.info("These rows are not expected:");
      for (Map.Entry<String, Integer> entry : unexpectedMap.entrySet()) {
        if (count < MAX_MISMATCH_SIZE) {
          LOG.info("\t" + entry.getKey() + " : " + entry.getValue()
              + " time(s).");
          count++;
        } else {
          break;
        }
      }
      LOG.info("Total number of unexpected rows: " + unexpectedMap.size());
    }
    count = 0;
    if (!expectedMap.isEmpty()) {
      LOG.info("These rows are expected but are not in result set:");
      for (Map.Entry<String, Integer> entry : expectedMap.entrySet()) {
        if (count < MAX_MISMATCH_SIZE) {
          LOG.info("\t" + entry.getKey() + " : " + entry.getValue()
              + " time(s).");
          count++;
        } else {
          break;
        }
      }
      LOG.info("Total number of expected but missing: " + expectedMap.size());
    }
  }

  public static boolean rowNumberVerify(String expectedOutput,
      String actualOutput, int outputFileHeaderSize) throws IOException,
      InterruptedException {
    Map<String, Integer> expectedMap = getExpectedMap(expectedOutput);
    int expectedNumberOfRows = expectedMap.size();
    BufferedReader reader = new BufferedReader(new FileReader(new File(
        actualOutput)));
    int actualNumberOfRows = 0;
    String line = "";
    while ((line = reader.readLine()) != null && line.trim().length() != 0) {
      if (line.contains("rows selected")) {
        int index = line.indexOf("rows selected");
        line = line.substring(0, index).trim();
        line = line.replaceAll(",", "");
        actualNumberOfRows = Integer.parseInt(line);
        break;
      }
    }
    reader.close();
    LOG.info("Expected number of rows: " + expectedNumberOfRows);
    LOG.info("Actual number of rows: " + actualNumberOfRows);
    return expectedNumberOfRows == actualNumberOfRows;
  }
}
