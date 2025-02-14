/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.sedona.sql.UDF

import org.apache.spark.sql.catalyst.FunctionIdentifier
import org.apache.spark.sql.{SQLContext, SparkSession, functions}

object UdfRegistrator {

  def registerAll(sqlContext: SQLContext): Unit = {
    registerAll(sqlContext.sparkSession)
  }

  def registerAll(sparkSession: SparkSession): Unit = {
    Catalog.expressions.foreach { case (functionIdentifier, expressionInfo, functionBuilder) =>
      sparkSession.sessionState.functionRegistry.registerFunction(
        functionIdentifier,
        expressionInfo,
        functionBuilder
      )
    }
Catalog.aggregateExpressions.foreach(f => sparkSession.udf.register(f.getClass.getSimpleName, functions.udaf(f))) // SPARK3 anchor
//Catalog.aggregateExpressions_UDAF.foreach(f => sparkSession.udf.register(f.getClass.getSimpleName, f)) // SPARK2 anchor
    sparkSession.udf.register(Catalog.rasterAggregateExpression.getClass.getSimpleName, functions.udaf(Catalog.rasterAggregateExpression))
  }

  def dropAll(sparkSession: SparkSession): Unit = {
    Catalog.expressions.foreach { case (functionIdentifier, _, _) =>
      sparkSession.sessionState.functionRegistry.dropFunction(functionIdentifier)
    }
Catalog.aggregateExpressions.foreach(f => sparkSession.sessionState.functionRegistry.dropFunction(FunctionIdentifier(f.getClass.getSimpleName))) // SPARK3 anchor
//Catalog.aggregateExpressions_UDAF.foreach(f => sparkSession.sessionState.functionRegistry.dropFunction(FunctionIdentifier(f.getClass.getSimpleName))) // SPARK2 anchor
    sparkSession.sessionState.functionRegistry.dropFunction(FunctionIdentifier(Catalog.rasterAggregateExpression.getClass.getSimpleName))
  }
}
