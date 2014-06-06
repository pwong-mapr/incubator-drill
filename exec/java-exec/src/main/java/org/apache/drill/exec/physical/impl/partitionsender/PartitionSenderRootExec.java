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
package org.apache.drill.exec.physical.impl.partitionsender;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

import org.apache.drill.common.expression.ErrorCollector;
import org.apache.drill.common.expression.ErrorCollectorImpl;
import org.apache.drill.common.expression.LogicalExpression;
import org.apache.drill.exec.exception.ClassTransformationException;
import org.apache.drill.exec.exception.SchemaChangeException;
import org.apache.drill.exec.expr.ClassGenerator;
import org.apache.drill.exec.expr.CodeGenerator;
import org.apache.drill.exec.expr.ExpressionTreeMaterializer;
import org.apache.drill.exec.memory.OutOfMemoryException;
import org.apache.drill.exec.ops.FragmentContext;
import org.apache.drill.exec.ops.OperatorContext;
import org.apache.drill.exec.ops.OperatorStats;
import org.apache.drill.exec.physical.config.HashPartitionSender;
import org.apache.drill.exec.physical.impl.RootExec;
import org.apache.drill.exec.physical.impl.SendingAccountor;
import org.apache.drill.exec.physical.impl.svremover.RemovingRecordBatch;
import org.apache.drill.exec.proto.CoordinationProtos.DrillbitEndpoint;
import org.apache.drill.exec.proto.ExecProtos.FragmentHandle;
import org.apache.drill.exec.record.*;
import org.apache.drill.exec.record.BatchSchema.SelectionVectorMode;
import org.apache.drill.exec.rpc.data.DataTunnel;

import com.sun.codemodel.JExpr;
import com.sun.codemodel.JExpression;
import com.sun.codemodel.JType;
import org.apache.drill.exec.vector.CopyUtil;


public class PartitionSenderRootExec implements RootExec {

  static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(PartitionSenderRootExec.class);
  private RecordBatch incoming;
  private HashPartitionSender operator;
  private Partitioner partitioner;
  private FragmentContext context;
  private OperatorContext oContext;
  private boolean ok = true;
  private final SendingAccountor sendCount = new SendingAccountor();
  private final OperatorStats stats;
  private final int outGoingBatchCount;
  private final HashPartitionSender popConfig;
  private final StatusHandler statusHandler;


  public PartitionSenderRootExec(FragmentContext context,
                                 RecordBatch incoming,
                                 HashPartitionSender operator) throws OutOfMemoryException {

    this.incoming = incoming;
    this.operator = operator;
    this.context = context;
    this.oContext = new OperatorContext(operator, context);
    this.stats = oContext.getStats();
    this.outGoingBatchCount = operator.getDestinations().size();
    this.popConfig = operator;
    this.statusHandler = new StatusHandler(sendCount, context);
  }

  @Override
  public boolean next() {
    boolean newSchema = false;

    if (!ok) {
      stop();

      return false;
    }

    RecordBatch.IterOutcome out = incoming.next();
    logger.debug("Partitioner.next(): got next record batch with status {}", out);
    switch(out){
      case NONE:
        try {
          // send any pending batches
          if(partitioner != null) {
            partitioner.flushOutgoingBatches(true, false);
          } else {
            sendEmptyBatch();
          }
        } catch (IOException e) {
          incoming.kill();
          logger.error("Error while creating partitioning sender or flushing outgoing batches", e);
          context.fail(e);
        }
        return false;

      case STOP:
        if (partitioner != null) {
          partitioner.clear();
        }
        return false;

      case OK_NEW_SCHEMA:
        newSchema = true;
        try {
          // send all existing batches
          if (partitioner != null) {
            partitioner.flushOutgoingBatches(false, true);
            partitioner.clear();
          }
          // update DeprecatedOutgoingRecordBatch's schema and generate partitioning code
          createPartitioner();
        } catch (IOException e) {
          incoming.kill();
          logger.error("Error while flushing outgoing batches", e);
          context.fail(e);
          return false;
        } catch (SchemaChangeException e) {
          incoming.kill();
          logger.error("Error while setting up partitioner", e);
          context.fail(e);
          return false;
        }
      case OK:
        stats.batchReceived(0, incoming.getRecordCount(), newSchema);
        try {
          partitioner.partitionBatch(incoming);
        } catch (IOException e) {
          incoming.kill();
          context.fail(e);
          return false;
        }
        for (VectorWrapper v : incoming) {
          v.clear();
        }
        return true;
      case NOT_YET:
      default:
        throw new IllegalStateException();
    }
  }

  private void createPartitioner() throws SchemaChangeException {

    // set up partitioning function
    final LogicalExpression expr = operator.getExpr();
    final ErrorCollector collector = new ErrorCollectorImpl();
    final ClassGenerator<Partitioner> cg ;

    boolean hyper = false;

    cg = CodeGenerator.getRoot(Partitioner.TEMPLATE_DEFINITION, context.getFunctionRegistry());
    ClassGenerator<Partitioner> cgInner = cg.getInnerGenerator("OutgoingRecordBatch");

    final LogicalExpression materializedExpr = ExpressionTreeMaterializer.materialize(expr, incoming, collector, context.getFunctionRegistry());
    if (collector.hasErrors()) {
      throw new SchemaChangeException(String.format(
          "Failure while trying to materialize incoming schema.  Errors:\n %s.",
          collector.toErrorString()));
    }

    // generate code to copy from an incoming value vector to the destination partition's outgoing value vector
    JExpression bucket = JExpr.direct("bucket");

    // generate evaluate expression to determine the hash
    ClassGenerator.HoldingContainer exprHolder = cg.addExpr(materializedExpr);
    cg.getEvalBlock().decl(JType.parse(cg.getModel(), "int"), "bucket", exprHolder.getValue().mod(JExpr.lit(outGoingBatchCount)));
    cg.getEvalBlock()._return(cg.getModel().ref(Math.class).staticInvoke("abs").arg(bucket));

    CopyUtil.generateCopies(cgInner, incoming, incoming.getSchema().getSelectionVectorMode() == SelectionVectorMode.FOUR_BYTE);

    try {
      // compile and setup generated code
//      partitioner = context.getImplementationClassMultipleOutput(cg);
      partitioner = context.getImplementationClass(cg);
      partitioner.setup(context, incoming, popConfig, stats, sendCount, oContext, statusHandler);

    } catch (ClassTransformationException | IOException e) {
      throw new SchemaChangeException("Failure while attempting to load generated class", e);
    }
  }

  public void stop() {
    logger.debug("Partition sender stopping.");
    ok = false;
    if (partitioner != null) {
      partitioner.clear();
    }
    sendCount.waitForSendComplete();

    if (!statusHandler.isOk()) {
      context.fail(statusHandler.getException());
    }

    oContext.close();
    incoming.cleanup();
  }

  public void sendEmptyBatch() {
    FragmentHandle handle = context.getHandle();
    int fieldId = 0;
    VectorContainer container = new VectorContainer();
    StatusHandler statusHandler = new StatusHandler(sendCount, context);
    for (DrillbitEndpoint endpoint : popConfig.getDestinations()) {
      FragmentHandle opposite = context.getHandle().toBuilder().setMajorFragmentId(popConfig.getOppositeMajorFragmentId()).setMinorFragmentId(fieldId).build();
      DataTunnel tunnel = context.getDataTunnel(endpoint, opposite);
      FragmentWritableBatch writableBatch = new FragmentWritableBatch(true,
              handle.getQueryId(),
              handle.getMajorFragmentId(),
              handle.getMinorFragmentId(),
              operator.getOppositeMajorFragmentId(),
              fieldId,
              WritableBatch.getBatchNoHVWrap(0, container, false));
      tunnel.sendRecordBatch(statusHandler, writableBatch);
      this.sendCount.increment();
      fieldId++;
    }
  }

}
