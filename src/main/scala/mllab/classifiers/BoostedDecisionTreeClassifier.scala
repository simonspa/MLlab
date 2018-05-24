package classifiers

import play.api.libs.json.JsValue

import json._


/** Companion object providing default parameters */
object BoostedDecisionTreeClassifier {
  val depth: Int = 3
  val criterion: String="gini"
  val minSamplesSplit: Int = 2
  val n_estimators: Int = 3
}

/** Decision tree classifier
 * @param depth Depth of the tree
 * @param criterion Function to measure the quality of a split
 * @param minSamplesSplit Minimum number of samples required to split an internal node
 */
class BoostedDecisionTreeClassifier(
  depth: Int = BoostedDecisionTreeClassifier.depth,
  criterion: String = BoostedDecisionTreeClassifier.criterion,
  minSamplesSplit: Int =  BoostedDecisionTreeClassifier.minSamplesSplit,
  n_estimators: Int =  BoostedDecisionTreeClassifier.n_estimators
) extends Classifier {
  def this(json: JsValue) = {
    this(
      depth = JsonMagic.toInt(json, "depth", BoostedDecisionTreeClassifier.depth),
      criterion = JsonMagic.toString(json, "criterion", BoostedDecisionTreeClassifier.criterion),
      minSamplesSplit = JsonMagic.toInt(json, "minSamplesSplit", BoostedDecisionTreeClassifier.minSamplesSplit),
      n_estimators = JsonMagic.toInt(json, "n_estimators", BoostedDecisionTreeClassifier.n_estimators)
      )
  }

  val name: String = "BoostedDecisionTreeClassifier"

  def train(X: List[List[Double]], y: List[Int]): Unit = {
    require(X.length == y.length, "number of training instances and labels is not equal")
    throw new NotImplementedError("BoostedDecisionTreeClassifier not implemented yet")
  }

  def predict(X: List[List[Double]]): List[Int] =
    for (instance <- X) yield 0

}
