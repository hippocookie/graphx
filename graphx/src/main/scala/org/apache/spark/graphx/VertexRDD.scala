package org.apache.spark.graphx

import org.apache.spark.rdd.RDD
import org.apache.spark.{Dependency, Partition, SparkContext}

import scala.reflect.ClassTag

abstract class VertexRDD[VD] (sc: SparkContext, deps: Seq[Dependency[_]]) extends RDD[(VertexId, VD)](sc, deps) {

  implicit protected def vdTag: ClassTag[VD]

  private[graphx] def partitionsRDD: RDD[ShippableVertexPartition[VD]]

  override protected def getPartitions: Array[Partition] = partitionsRDD.partitions



}
