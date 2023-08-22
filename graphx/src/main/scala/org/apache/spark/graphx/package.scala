package org.apache.spark

import org.apache.spark.util.collection.OpenHashSet

package object graphx {
  type VertexId = Long

  type PartitionID = Int

  private[graphx] type VertexSet = OpenHashSet[VertexId]
}