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

import java.util.List;

/**
 * Modeler for a drill test case from test definition json file.
 * 
 * @author Zhiyong Liu
 * 
 */
public class TestCaseModeler {
  private String testId;
  private String type;
  private String description;
  private String submitType = "jdbc";
  private String queryType = "sql";
  private List<String> categories;
  private List<TestMatrix> matrices;
  private List<DataSource> datasources;

  /**
   * Default constructor.
   */
  public TestCaseModeler() {
  }

  /**
   * Constructor with various fields.
   * 
   * @param testId
   * @param type
   * @param description
   * @param categories
   * @param matrices
   * @param datasources
   */
  public TestCaseModeler(String testId, String type, String description,
      List<String> categories, List<TestMatrix> matrices,
      List<DataSource> datasources) {
    this.testId = testId;
    this.type = type;
    this.description = description;
    this.categories = categories;
    this.matrices = matrices;
    this.datasources = datasources;
  }

  /**
   * Constructor with various fields.
   * 
   * @param testId
   * @param type
   * @param description
   * @param submitType
   * @param queryType
   * @param categories
   * @param matrices
   * @param datasources
   */
  public TestCaseModeler(String testId, String type, String description,
      String submitType, String queryType, List<String> categories,
      List<TestMatrix> matrices, List<DataSource> datasources) {
    this(testId, type, description, categories, matrices, datasources);
    if (submitType != null) {
      this.submitType = submitType;
    }
    if (queryType != null) {
      this.queryType = queryType;
    }
  }

  public String getTestId() {
    return testId;
  }

  public String getType() {
    return type;
  }

  public String getDescription() {
    return description;
  }

  public String getSubmitType() {
    return submitType;
  }

  public String getQueryType() {
    return queryType;
  }

  public List<String> getCategories() {
    return categories;
  }

  public List<TestMatrix> getMatrices() {
    return matrices;
  }

  public List<DataSource> getDatasources() {
    return datasources;
  }

  public String toString() {
    String result = "Test Id: " + testId + "\nTest Type: " + type
        + "\nTest Description: " + description;
    if (submitType != null) {
      result += "\nSubmit Type: " + submitType;
    }
    if (queryType != null) {
      result += "\nQuery Type: " + queryType;
    }
    result += "\nTest Categories: " + categories;
    for (TestMatrix matrix : matrices) {
      result += "\n" + matrix.toString();
    }
    if (datasources != null) {
      for (DataSource datasource : datasources) {
        result += "\n" + datasource.toString();
      }
    }
    return result;
  }

  /**
   * Modeler for the TestMatrix section of test definition.
   * 
   * @author Zhiyong Liu
   * 
   */
  public class TestMatrix {
    private String inputFile;
    private String schema;
    private String outputFormat;
    private String expectedFile;
    private List<String> verificationTypes;

    /**
     * Constructor with various fields.
     * 
     * @param inputFile
     * @param schema
     * @param outputFormat
     * @param expectedFile
     * @param verificationTypes
     */
    public TestMatrix(String inputFile, String schema, String outputFormat,
        String expectedFile, List<String> verificationTypes) {
      this.inputFile = inputFile;
      this.schema = schema;
      this.outputFormat = outputFormat;
      this.expectedFile = expectedFile;
      this.verificationTypes = verificationTypes;
    }

    public String getInputFile() {
      return inputFile;
    }

    public String getSchema() {
      return schema;
    }

    public String getOutputFormat() {
      return outputFormat;
    }

    public String getExpectedFile() {
      return expectedFile;
    }

    public List<String> getVerificationTypes() {
      return verificationTypes;
    }

    public String toString() {
      return "Test Matrices:" + "\n\tinput-file: " + inputFile + "\n\tschema: "
          + schema + "\n\toutput-format: " + outputFormat
          + "\n\texpected-file: " + expectedFile + "\n\tverification-type: "
          + verificationTypes.toString();
    }
  }

  /**
   * Modeler for the DataSource section of test definition.
   * 
   * @author Zhiyong Liu
   * 
   */
  public class DataSource {
    private String mode;
    private String source;
    private String destination;

    /**
     * Constructor with various fields.
     * 
     * @param mode
     * @param source
     * @param destination
     */
    public DataSource(String mode, String source, String destination) {
      this.mode = mode;
      this.source = source;
      this.destination = destination;
    }

    public String getMode() {
      return mode;
    }

    public String getSource() {
      return source;
    }

    public String getDestination() {
      return destination;
    }

    public String toString() {
      String result = "Data Sources: " + "\n\tMode: " + mode + "\n\tSource: "
          + source;
      if (destination != null && destination.length() > 0) {
        result += "\n\tDestination: " + destination;
      }
      return result;
    }
  }
}
