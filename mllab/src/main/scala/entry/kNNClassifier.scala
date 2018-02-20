package mllab

import scala.collection.mutable.ListBuffer


class kNNClassifier(k: Int = 3) {

  var X_NN = new ListBuffer[List[Float]]()
  var y_NN = new ListBuffer[Int]()

  def train(X: List[List[Float]], y: List[Int]): Unit = {
    assert (X.length == y.length)
    assert (X.length >= k)
    X.copyToBuffer(X_NN)
    y.copyToBuffer(y_NN)
  }

  def predict(X: List[List[Float]]): List[Int] = {
    var result = new ListBuffer[Int]()
    for (instance <- X){
      var min_distance: Double = Double.MaxValue
      var min_distances = new ListBuffer[Double]()
      var min_indices = new ListBuffer[Int]()
      for (i <- 0 until k) {
        min_distances +=  Double.MaxValue
        min_indices += -1
      }

      def queue_new_minimum(index: Int, distance: Double): Unit = {
        min_distance = distance
        // replace entry with highest distance with new nearest
        val max = min_distances.max
        val max_index = min_distances.indexOf(max)
        min_distances(max_index) = distance
        min_indices(max_index) = index
      }

      def get_prediction(): Int = {

        assert (!min_indices.contains(-1))

        var prediction: Int = -1

        val strat: String = "majority"
        if (strat == "minimum"){
          val min = min_distances.min
          val min_index = min_distances.indexOf(min)
          val min_global_index = min_indices(min_index)
          val prediction = y_NN(min_global_index)
        }
        else if (strat == "majority"){
          var nearest_labels = for (i <- min_indices) yield y_NN(i)
          prediction = (nearest_labels.sum / k).round
        }
        prediction
      }

      for (i <- 0 until X_NN.length) {
        var squares = for ((x, y) <- X_NN(i) zip instance) yield Math.pow(x - y, 2)
        val distance = squares.sum
        if (distance < min_distance || min_indices.contains(-1)) {
          queue_new_minimum(i, distance)
        }
      }
      val prediction = get_prediction()
      result += prediction
    }
    result.toList
  }
}
