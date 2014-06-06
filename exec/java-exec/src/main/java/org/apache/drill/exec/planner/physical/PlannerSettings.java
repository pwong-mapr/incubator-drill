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
package org.apache.drill.exec.planner.physical;

import net.hydromatic.optiq.tools.FrameworkContext;

import org.apache.drill.exec.server.options.OptionManager;
import org.apache.drill.exec.server.options.OptionValidator;
import org.apache.drill.exec.server.options.TypeValidators.BooleanValidator;
import org.apache.drill.exec.server.options.TypeValidators.PositiveLongValidator;

public class PlannerSettings implements FrameworkContext{
  static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(PlannerSettings.class);

  private int numEndPoints = 0;
  private boolean useDefaultCosting = false; // True: use default Optiq costing, False: use Drill costing

  public static final int MAX_BROADCAST_THRESHOLD = Integer.MAX_VALUE;

  public static final OptionValidator EXCHANGE = new BooleanValidator("planner.disable_exchanges", false);
  public static final OptionValidator HASHAGG = new BooleanValidator("planner.enable_hashagg", true);
  public static final OptionValidator STREAMAGG = new BooleanValidator("planner.enable_streamagg", true);
  public static final OptionValidator HASHJOIN = new BooleanValidator("planner.enable_hashjoin", true);
  public static final OptionValidator MERGEJOIN = new BooleanValidator("planner.enable_mergejoin", true);
  public static final OptionValidator MULTIPHASE = new BooleanValidator("planner.enable_multiphase_agg", false);
  public static final OptionValidator BROADCAST = new BooleanValidator("planner.enable_broadcast_join", true);
  public static final OptionValidator BROADCAST_THRESHOLD = new PositiveLongValidator("planner.broadcast_threshold", MAX_BROADCAST_THRESHOLD, 10000);

  public OptionManager options = null;

  public PlannerSettings(OptionManager options){
    this.options = options;
  }

  public boolean isSingleMode() {
    return options.getOption(EXCHANGE.getOptionName()).bool_val;
  }

  public int numEndPoints() {
    return numEndPoints;
  }

  public boolean useDefaultCosting() {
    return useDefaultCosting;
  }

  public void setNumEndPoints(int numEndPoints) {
    this.numEndPoints = numEndPoints;
  }

  public void setUseDefaultCosting(boolean defcost) {
    this.useDefaultCosting = defcost;
  }

  public boolean isHashAggEnabled() {
    return options.getOption(HASHAGG.getOptionName()).bool_val;
  }

  public boolean isStreamAggEnabled() {
    return options.getOption(STREAMAGG.getOptionName()).bool_val;
  }

  public boolean isHashJoinEnabled() {
    return options.getOption(HASHJOIN.getOptionName()).bool_val;
  }

  public boolean isMergeJoinEnabled() {
    return options.getOption(MERGEJOIN.getOptionName()).bool_val;
  }

  public boolean isMultiPhaseAggEnabled() {
    return options.getOption(MULTIPHASE.getOptionName()).bool_val;
  }

  public boolean isBroadcastJoinEnabled() {
    return options.getOption(BROADCAST.getOptionName()).bool_val;
  }

  public long getBroadcastThreshold() {
    return options.getOption(BROADCAST_THRESHOLD.getOptionName()).num_val;
  }

  @Override
  public <T> T unwrap(Class<T> clazz) {
    if(clazz == PlannerSettings.class){
      return (T) this;
    }else{
      return null;
    }
  }


}
