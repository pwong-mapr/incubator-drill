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
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.testng.ITestContext;
import org.testng.annotations.DataProvider;

/**
 * Data provider for all drill tests.
 * 
 * @author Zhiyong Liu
 * 
 */
public class JsonTestDataProvider {
  private static final Logger LOG = Logger.getLogger(Utils
      .getInvokingClassName());

  /**
   * Constructs an iteration of test case definitions from various test data
   * sources, obtained from the mvn command line option. See README.md for more
   * details.
   * 
   * @param context
   *          test context which contains all the information for a given test
   *          run. An instance of this context is passed to the test listeners
   *          so they can query information about their environment.
   * @param method
   *          method that makes a call to this data provider.
   * @return an iteration of object arrays that defines set of tests to be
   *         executed.
   * @throws Exception
   */
  @DataProvider(name = "jsonTestDataProvider")
  public static Iterator<Object[]> getData(ITestContext context, Method method)
      throws Exception {
    String[] testDefSources = null;
    try {
      testDefSources = System.getProperty("test.def.sources").split(",");
    } catch (Exception e) {
      testDefSources = new String[] { "" };
    }
    String[] testGroups = null;
    try {
      testGroups = System.getProperty("test.groups").split(",");
    } catch (Exception e) {
      LOG.info("Test groups not specified.  Will run all collected tests.");
    }
    List<Object[]> data = new ArrayList<Object[]>();
    for (String testDefSource : testDefSources) {
      testDefSource = Utils.toResovedPath(testDefSource, "DRILL_TEST_DATA_DIR");
      File testDefSourceFile = new File(testDefSource);
      JsonTestDataProcessor processor = null;
      List<File> testDefFiles = searchFiles(testDefSourceFile, ".*.json");
      for (File testDefFile : testDefFiles) {
        processor = new JsonTestDataProcessor(testDefFile.getAbsolutePath());
        String type = processor.getSimpleTestParameter("type");
        List<String> categories = processor.getListTestParameter("categories");
        if (type.equals("individual")) {
          if (testGroups == null) {
            data.add(new Object[] { processor.constructTestCaseModeler() });
            continue;
          }
          for (String testGroup : testGroups) {
            if (categories.contains(testGroup)) {
              data.add(new Object[] { processor.constructTestCaseModeler() });
            }
          }
        } else if (type.equals("group")) {
          boolean foundTests = false;
          for (String testGroup : testGroups) {
            if (categories != null && !categories.contains(testGroup)) {
              continue;
            } else {
              foundTests = true;
              break;
            }
          }
          if (!foundTests) {
            continue;
          }
          String queryFileExtension = processor
              .constructTestMatrices(null, null).get(0).getInputFile();
          String expectedFileExtension = processor
              .constructTestMatrices(null, null).get(0).getExpectedFile();
          List<File> testQueryFiles = searchFiles(testDefFile.getParentFile(),
              queryFileExtension);
          boolean checkDatasources = true;
          for (File testQueryFile : testQueryFiles) {
            String expectedFileName = getExpectedFile(
                testQueryFile.getAbsolutePath(), queryFileExtension,
                expectedFileExtension);
            List<TestCaseModeler.TestMatrix> matrices = processor
                .constructTestMatrices(testQueryFile.getAbsolutePath(),
                    expectedFileName);
            if (checkDatasources) {
              data.add(new Object[] { processor
                  .constructTestCaseModeler(matrices) });
              checkDatasources = false;
            } else {
              data.add(new Object[] { processor.constructTestCaseModeler(
                  matrices, null) });
            }
          }
        }
      }
    }
    if (data.size() == 0) {
      LOG.warn("Warning: No test cases have been collected.");
    }
    return data.iterator();
  }

  private static List<File> searchFiles(File root, String regex) {
    List<File> list = new ArrayList<File>();
    Pattern pattern = Pattern.compile(regex + "$");
    Matcher matcher = null;
    if (root.isFile()) {
      matcher = pattern.matcher(root.getName());
      if (matcher.find()) {
        list.add(root);
        return list;
      }
    } else {
      for (File file : root.listFiles()) {
        if (!file.getName().equals("datasources")) {
          list.addAll(searchFiles(file, regex));
        }
      }
    }
    return list;
  }

  private static String getExpectedFile(String queryFile, String queryFileExt,
      String expectedFileExt) {
    int idx = queryFile.indexOf(queryFileExt.substring(2));
    return queryFile.substring(0, idx).concat(expectedFileExt.substring(2));
  }
}