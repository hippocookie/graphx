package org.apache.spark.graphx

import org.apache.spark.rdd.RDD
import org.apache.spark.storage.StorageLevel

import scala.reflect.ClassTag

abstract class Graph[VD: ClassTag, ED: ClassTag] protected() extends Serializable {

  val vertices: VertexRDD[VD]

  val edges: EdgeRDD[ED]

  val triplets: RDD[EdgeTriplet[VD, ED]]

  def persist(newLevel: StorageLevel = StorageLevel.MEMORY_ONLY): Graph[VD, ED]

  def cache(): Graph[VD, ED]

  def checkpoint(): Unit

  def isCheckpointed: Boolean

  def getCheckpointFiles: Seq[String]

  def unpersist(blocking: Boolean = false): Graph[VD, ED]

  def unpersistVertices(blocking: Boolean = false): Graph[VD, ED]

  def partitionBy(partitionStrategy: PartitionStrategy): Graph[VD, ED]

  def partitionBy(partitionStrategy: PartitionStrategy, numPartitions: Int): Graph[VD, ED]

  def mapVertices[VD2: ClassTag](map: (VertexId, VD) => VD2)
                                (implicit eq: VD =:= VD2 = null): Graph[VD2, ED]

  def mapEdges[ED2: ClassTag](map: Edge[ED] => ED2): Graph[VD, ED2] = {
    mapEdges((pid, iter) => iter.map(map))
  }

  def mapEdges[ED2: ClassTag](map: (PartitionID, Iterator[Edge[ED]]) => Iterator[ED2])
  : Graph[VD, ED2]

  def mapTriplets[ED2: ClassTag](map: EdgeTriplet[VD, ED] => ED2): Graph[VD, ED2] = {
    mapTriplets((pid, iter) => iter.map(map), TripletFields.All)
  }

  def mapTriplets[ED2: ClassTag](map: EdgeTriplet[VD, ED] => ED2, tripletFields: TripletFields): Graph[VD, ED2] = {
    mapTriplets((pid, iter) => iter.map(map), tripletFields)
  }

  def mapTriplets[ED2: ClassTag](map: (PartitionID, Iterator[EdgeTriplet[VD, ED]]) => Iterator[ED2], tripletFields: TripletFields): Graph[VD, ED2]

  def reverse: Graph[VD, ED]

  def subgraph(epred: EdgeTriplet[VD, ED] => Boolean = (x => true), vpred: (VertexId, VD) => Boolean = ((v, d) => true)): Graph[VD, ED]

  def mask[VD2: ClassTag, ED2: ClassTag](other: Graph[VD2, ED2]): Graph[VD, ED]

  def groupEdges(merge: (ED, ED) => ED): Graph[VD, ED]

  def aggregateMessages[A: ClassTag](sendMsg: EdgeContext[VD, ED, A] => Unit, mergeMsg: (A, A) => A, tripletFields: TripletFields = TripletFields.All) : VertexRDD[A] = {
    aggregateMessagesWithActiveSet(sendMsg, mergeMsg, tripletFields, None)
  }

  private[graphx] def aggregateMessagesWithActiveSet[A: ClassTag](sendMsg: EdgeContext[VD, ED, A] => Unit, mergeMsg: (A, A) => A, tripletFields: Any, activeSetOpt: Option[(VertexRDD[_], EdgeDirection)]) = VertexRDD[A]

  def outerJoinVertices[U: ClassTag, VD2: ClassTag](other: RDD[(VertexId, U)]) (mapFunc: (VertexId, VD, Option[U]) => VD2)(implicit eq: VD =:= VD2 = null) : Graph[VD2, ED]

  val ops = new GraphOps(this)

}

object Graph {

  def fromEdgeTuples[VD: ClassTag](rawEdges: RDD[(VertexId, VertexId)], defaultValue: VD, uniqueEdges: Option[PartitionStrategy] = None, edgeStorageLevel: StorageLevel.MEMORY_ONLY, vertexStorageLevel: StorageLevel.MEMORY_ONLY): Graph[VD, Int] = {
    val edges = rawEdges.map(p => Edge(p._1, p._2, 1))
    val graph = GraphImpl(edge, defaultValue, edgeStorageLevel, vertexStorageLevel)
    uniqueEdges match {
      case Some(p) => graph.partitionBy(p).groupEdges((a, b) => a + b)
      case None => graph
    }
  }

  def fromEdges[VD: ClassTag, ED: ClassTag](edges: RDD[Edge[ED]], defaultValue: VD, edgeStorageLevel: StorageLevel.MEMORY_ONLY, vertexStorageLevel: StorageLevel.MEMORY_ONLY): Graph[VD, ED] = {
    GraphImp(edges, defaultValue, edgeStorageLevel, vertexStorageLevel)
  }

  def apply[VD: ClassTag, ED: ClassTag](vertices: RDD[(VertexId, VD)], edges: RDD[Edge[ED]], defaultVertexAttr: VD = null.asInstanceOf[VD], edgeStorageLevel: StorageLevel = StorageLevel.MEMORY_ONLY, vertexStorageLevel: StorageLevel = StorageLevel.MEMORY_ONLY): Graph[VD, ED] = {
    GraphImpl(vertices, edges, defaultVertexAttr, edgeStorageLevel, vertexStorageLevel)
  }

  implicit def graphToGraphOps[VD: ClassTag, ED: ClassTag] (g: Graph[VD, ED]) : GraphOps[VD, ED] = g.ops
}
