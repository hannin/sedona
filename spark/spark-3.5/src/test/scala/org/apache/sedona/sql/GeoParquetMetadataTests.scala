/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.sedona.sql

import org.apache.spark.sql.Row
import org.scalatest.BeforeAndAfterAll

import scala.collection.JavaConverters._

class GeoParquetMetadataTests extends TestBaseScala with BeforeAndAfterAll {
  val geoparquetdatalocation: String = resourceFolder + "geoparquet/"

  describe("GeoParquet Metadata tests") {
    it("Reading GeoParquet Metadata") {
      val df = sparkSession.read.format("geoparquet.metadata").load(geoparquetdatalocation)
      val metadataArray = df.collect()
      assert(metadataArray.length > 1)
      assert(metadataArray.exists(_.getAs[String]("path").endsWith(".parquet")))
      assert(metadataArray.exists(_.getAs[String]("version") == "1.0.0-dev"))
      assert(metadataArray.exists(_.getAs[String]("primary_column") == "geometry"))
      assert(metadataArray.exists { row =>
        val columnsMap = row.getJavaMap(row.fieldIndex("columns"))
        columnsMap != null && columnsMap.containsKey("geometry") && columnsMap.get("geometry").isInstanceOf[Row]
      })
      assert(metadataArray.forall { row =>
        val columnsMap = row.getJavaMap(row.fieldIndex("columns"))
        if (columnsMap == null || !columnsMap.containsKey("geometry")) true else {
          val columnMetadata = columnsMap.get("geometry").asInstanceOf[Row]
          columnMetadata.getAs[String]("encoding") == "WKB" &&
            columnMetadata.getList[Any](columnMetadata.fieldIndex("bbox")).asScala.forall(_.isInstanceOf[Double]) &&
            columnMetadata.getList[Any](columnMetadata.fieldIndex("geometry_types")).asScala.forall(_.isInstanceOf[String]) &&
            columnMetadata.getAs[String]("crs").nonEmpty
        }
      })
    }

    it("Reading GeoParquet Metadata with column pruning") {
      val df = sparkSession.read.format("geoparquet.metadata").load(geoparquetdatalocation)
      val metadataArray = df.selectExpr("path", "substring(primary_column, 1, 2) AS partial_primary_column").collect()
      assert(metadataArray.length > 1)
      assert(metadataArray.forall(_.length == 2))
      assert(metadataArray.exists(_.getAs[String]("path").endsWith(".parquet")))
      assert(metadataArray.exists(_.getAs[String]("partial_primary_column") == "ge"))
    }

    it("Reading GeoParquet Metadata of plain parquet files") {
      val df = sparkSession.read.format("geoparquet.metadata").load(geoparquetdatalocation)
      val metadataArray = df.where("path LIKE '%plain.parquet'").collect()
      assert(metadataArray.nonEmpty)
      assert(metadataArray.forall(_.getAs[String]("path").endsWith("plain.parquet")))
      assert(metadataArray.forall(_.getAs[String]("version") == null))
      assert(metadataArray.forall(_.getAs[String]("primary_column") == null))
      assert(metadataArray.forall(_.getAs[String]("columns") == null))
    }
  }
}
