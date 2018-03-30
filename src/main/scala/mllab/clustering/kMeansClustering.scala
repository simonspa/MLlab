package clustering

import scala.collection.mutable.ListBuffer

import datastructures._
import utils._


/** k-Means clustering
 * @param k Number of clusters to search for
 * @todo improve centroid initialization
 */
class kMeansClustering(k: Int = 3) extends Clustering {

  val name: String = "kMeansClustering"

  var lossEvolution = new ListBuffer[(Double, Double)]()
  var centroidEvolution = new ListBuffer[List[List[Double]]]()

  def clusterMeans(): List[List[List[Double]]] =
    centroidEvolution.toList.transpose

  def predict(X: List[List[Double]]): List[Int] = {
    val nFeatures = X.head.length
    val range = 2
    val centroids: List[List[Double]] = List.fill(k)(List.fill(nFeatures)((scala.util.Random.nextDouble - 0.5) * range))
    val maxIter = 100

    def getLoss(X: List[List[Double]], y: List[Int], centroids: List[List[Double]]): Double = {
      (for (c <- 0 until centroids.length) yield {
        val thisClusterX: List[List[Double]] = (X zip y).filter(_._2 == c).map(_._1)
        val thisClusterSquaredDist: List[List[Double]] = thisClusterX.map(x => Maths.minus(x, centroids(c)).map(Math.pow(_, 2)))
        val thisClusterLoss: Double = thisClusterSquaredDist.map(_.sum).sum
        thisClusterLoss
      }).sum
    }

    def clusterToCentroid(count: Int, X: List[List[Double]], y: List[Int], centroids: List[List[Double]], stop: Boolean): Tuple2[List[Int], List[List[Double]]] = {
      if (count >= maxIter || stop) {
        val loss = getLoss(X, y, centroids)
        lossEvolution += Tuple2(count.toDouble, loss)
        centroidEvolution += centroids
        println("Final% 4d with loss %.4e and centroids ".format(count, loss) +
          centroids.map(c => c.map(Maths.round(_, 3)).mkString("[", ", ", "]")))
        Tuple2(y, centroids)
      }
      else {
        val newy: List[Int] = X.map(x => (for (i <- 0 until centroids.length) yield Maths.distance(centroids(i), x)).zipWithIndex.min._2)
        if (count % 100 == 0 || (count < 50 && count % 10 == 0) || (count < 5)) {
          val loss = getLoss(X, newy, centroids)
          lossEvolution += Tuple2(count.toDouble, loss)
          centroidEvolution += centroids
          println("Step% 4d with loss %.4e and centroids ".format(count, loss) +
            centroids.map(c => c.map(Maths.round(_, 3)).mkString("[", ", ", "]")))
        }
        val newCentroids: List[List[Double]] = kMeans.getCentroids(X, newy, k)
        if (newCentroids.toSet != centroids.toSet)
          clusterToCentroid(count+1, X, newy, newCentroids, stop)
        else
          clusterToCentroid(count+1, X, newy, centroids, true)
      }
    }
    clusterToCentroid(0, X, Nil, centroids, false)._1
  }

  override def diagnostics(): Map[String, List[(Double, Double)]] = {
    Map("loss" -> lossEvolution.toList)
  }

}