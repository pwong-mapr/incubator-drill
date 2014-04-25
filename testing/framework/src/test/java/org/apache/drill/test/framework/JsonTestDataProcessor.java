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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

/**
 * Processes the test definition json file.
 * 
 * @author Zhiyong Liu
 * 
 */
public class JsonTestDataProcessor {
  private String jsonFile;
  private JSONObject json;

  /**
   * Constructs the class with the json file to process.
   * 
   * @param jsonFile
   *          json file to process.
   * @throws FileNotFoundException
   * @throws JSONException
   */
  public JsonTestDataProcessor(String jsonFile) throws FileNotFoundException,
      JSONException {
    this.jsonFile = jsonFile;
    json = new JSONObject(new JSONTokener(new FileReader(new File(jsonFile))));
  }

  /**
   * Gets the json file this processor is processing.
   * 
   * @return the json file.
   */
  public String getJsonFile() {
    return jsonFile;
  }

  /**
   * Sets the json file to be processed.
   * 
   * @param jsonFile
   *          json file to be processed.
   */
  public void setJsonFile(String jsonFile) {
    this.jsonFile = jsonFile;
  }

  /**
   * Returns the simple string value of a key.
   * 
   * @param key
   *          for the value to be obtained.
   * 
   * @return simple string value of the key.
   * @throws JSONException
   */
  public String getSimpleTestParameter(String key) throws JSONException {
    return json.getString(key);
  }

  /**
   * Returns the list of values for a key.
   * 
   * @param key
   *          for the values to be obtained.
   * @return list of strings.
   * @throws JSONException
   */
  public List<String> getListTestParameter(String key) throws JSONException {
    JSONArray jsonArray = (JSONArray) json.get(key);
    List<String> list = new ArrayList<String>();
    for (int i = 0; i < jsonArray.length(); i++) {
      list.add(jsonArray.getString(i));
    }
    return list;
  }

  /**
   * Constructs a list of TestCaseModeler.TestMatrix from the json definition
   * file.
   * 
   * @param queryFile
   *          the input query file value to be used. If null, the value is
   *          obtained from the definition file itself (when an individual test
   *          case is being handled). If not null, the file takes on the form of
   *          a regular expression for a class of query files (e.g., .*.q), and
   *          each of the matching query files will be collected for the
   *          data-driven engine.
   * @param expectedFile
   *          similar to queryFile.
   * @return list of TestCaseModeler.TestMatrix objects.
   * @throws JSONException
   */
  public List<TestCaseModeler.TestMatrix> constructTestMatrices(
      String queryFile, String expectedFile) throws JSONException {
    List<TestCaseModeler.TestMatrix> list = new ArrayList<TestCaseModeler.TestMatrix>();
    JSONArray jsonArray = (JSONArray) json.get("matrices");
    for (int i = 0; i < jsonArray.length(); i++) {
      JSONObject item = (JSONObject) jsonArray.get(i);
      JSONArray array = item.getJSONArray("verification-type");
      List<String> verifications = new ArrayList<String>();
      for (int j = 0; j < array.length(); j++) {
        verifications.add((String) array.get(j));
      }
      if (queryFile == null) {
        queryFile = item.getString("query-file");
        if (!queryFile.contains("*")) {
          queryFile = Utils.getDrillTestProperties().get("DRILL_TEST_DATA_DIR")
              + "/" + item.getString("query-file");
        }
      }
      if (expectedFile == null) {
        expectedFile = item.getString("expected-file");
        if (!expectedFile.contains("*")) {
          expectedFile = Utils.getDrillTestProperties().get(
              "DRILL_TEST_DATA_DIR")
              + "/" + item.getString("expected-file");
        }
      }
      list.add(constructTestMatrix(queryFile, item.getString("schema"),
          item.getString("output-format"), expectedFile, verifications));
    }
    return list;
  }

  private TestCaseModeler.TestMatrix constructTestMatrix(String queryFile,
      String schema, String outputFormat, String expectedFile,
      List<String> verifications) throws JSONException {
    return new TestCaseModeler().new TestMatrix(queryFile, schema,
        outputFormat, expectedFile, verifications);
  }

  private List<TestCaseModeler.DataSource> constructTestDataSources()
      throws JSONException {
    List<TestCaseModeler.DataSource> list = new ArrayList<TestCaseModeler.DataSource>();
    JSONArray jsonArray = (JSONArray) json.get("datasources");
    for (int i = 0; i < jsonArray.length(); i++) {
      JSONObject item = (JSONObject) jsonArray.get(i);
      list.add(new TestCaseModeler().new DataSource(item.getString("mode"),
          Utils.toAbsolutePath(item.getString("src"), "DRILL_TEST_DATA_DIR"),
          item.getString("dest") == null ? "" : Utils.toAbsolutePath(
              item.getString("dest"), "DRILL_TESTDATA")));
    }
    return list;
  }

  /**
   * Constructs a TestCaseModeler object with undefined query file.
   * 
   * @return the TestCaseModeler object constructed.
   * @throws JSONException
   */
  public TestCaseModeler constructTestCaseModeler() throws JSONException {
    return constructTestCaseModeler(constructTestMatrices(null, null));
  }

  /**
   * Constructs a TestCaseModeler object.
   * 
   * @param matrices
   *          list of TestCaseModeler.TestMatrix used in the construction.
   * @return a TestCaseModeler object.
   * @throws JSONException
   */
  public TestCaseModeler constructTestCaseModeler(
      List<TestCaseModeler.TestMatrix> matrices) throws JSONException {
    return constructTestCaseModeler(matrices, constructTestDataSources());
  }

  /**
   * Constructs a TestCaseModeler object.
   * 
   * @param matrices
   *          list of TestCaseModeler.TestMatrix used in the construction.
   * @return a TestCaseModeler object.
   * @throws JSONException
   */
  public TestCaseModeler constructTestCaseModeler(
      List<TestCaseModeler.TestMatrix> matrices,
      List<TestCaseModeler.DataSource> datasources) throws JSONException {
    String testId = getSimpleTestParameter("testId");
    String type = getSimpleTestParameter("type");
    String description = getSimpleTestParameter("description");
    List<String> categories = getListTestParameter("categories");
    String submitType = null;
    String queryType = null;
    try {
      submitType = getSimpleTestParameter("submit-type");
    } catch (JSONException e) {
    }
    try {
      queryType = getSimpleTestParameter("query-type");
    } catch (JSONException e) {
    }
    return new TestCaseModeler(testId, type, description, submitType,
        queryType, categories, matrices, datasources);
  }
}
