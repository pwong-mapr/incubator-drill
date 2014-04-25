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
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Handler that processes an array of input files and gathers queries into a
 * congregated input file. It also adds necessary sqlline commands to record
 * query output in desired format.
 * 
 * @author Zhiyong Liu
 */
public class InputQueryFileHandler {
  private String[] inputFileNames = null;
  private String[] outputFileNames = null;
  private String[] outputFormats = null;
  private int[] headSizes = null;

  /**
   * Default constructor.
   */
  public InputQueryFileHandler() {
  }

  /**
   * Constructs the handler with provided input and output files and output
   * format information.
   * 
   * @param inputFileNames
   *          array of input file names
   * @param outputFileNames
   *          array of output file names
   * @param outputFormats
   *          array of output file formats
   */
  public InputQueryFileHandler(String[] inputFileNames,
      String[] outputFileNames, String[] outputFormats) {
    this.setInputFileNames(inputFileNames);
    this.setOutputFileNames(outputFileNames);
    this.setOutputFormats(outputFormats);
  }

  /**
   * Sets input file names
   * 
   * @param inputFileNames
   *          names of input files to set
   */
  public void setInputFileNames(String[] inputFileNames) {
    this.inputFileNames = inputFileNames;
  }

  /**
   * Gets input file names
   * 
   * @return array of input file names
   */
  public String[] getInputFileNames() {
    return inputFileNames;
  }

  /**
   * Gets output file names
   * 
   * @return array of output file names
   */
  public String[] getOutputFileNames() {
    return outputFileNames;
  }

  /**
   * Sets output file names
   * 
   * @param outputFileNames
   *          names of output files to set
   */
  public void setOutputFileNames(String[] outputFileNames) {
    this.outputFileNames = outputFileNames;
  }

  /**
   * Gets output formats
   * 
   * @return array of output formats
   */
  public String[] getOutputFormats() {
    return outputFormats;
  }

  /**
   * Sets output formats
   * 
   * @param outputFormats
   *          names of output formats to set
   */
  public void setOutputFormats(String[] outputFormats) {
    this.outputFormats = outputFormats;
  }

  /**
   * Creates a file whose content is the concatenation of a number of query
   * files. It also adds flags to capture sqlline command line output and any
   * other sqlline commands needed.
   * 
   * @param congregatedFileName
   *          name of the resulting file
   * @param outputFileName
   *          name of output file to which command line output is written
   * @param outputFormat
   *          format used in presenting query output
   * @return
   * @throws IOException
   */
  public String congregateFiles(String congregatedFileName) throws IOException {
    if (inputFileNames.length != outputFileNames.length
        || (outputFormats != null && inputFileNames.length != outputFormats.length)) {
      throw new IllegalArgumentException(
          "Numbers of input files, output files and formats should be equal.");
    }
    File file = new File(congregatedFileName);
    StringBuilder builder = new StringBuilder();
    BufferedWriter writer = new BufferedWriter(new FileWriter(file));
    headSizes = new int[inputFileNames.length];
    for (int i = 0; i < inputFileNames.length; i++) {
      writer.write("!record " + outputFileNames[i] + "\n");
      headSizes[i]++;
      if (outputFormats != null && outputFormats[i] != null) {
        writer.write("!outputformat " + outputFormats[i] + "\n");
        headSizes[i]++;
      }
      BufferedReader reader = new BufferedReader(new FileReader(
          inputFileNames[i]));
      String line = "";
      while ((line = reader.readLine()) != null && line.length() > 0) {
        writer.write(line);
        writer.newLine();
        builder.append(line + "\n");
        headSizes[i]++;
      }
      reader.close();
      writer.write("!record\n");
    }
    writer.close();
    return builder.toString();
  }

  public int[] getHeadSizes() {
    return headSizes;
  }
}
