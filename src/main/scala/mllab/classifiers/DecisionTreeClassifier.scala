package classifiers

import play.api.libs.json.JsValue

import algorithms._
import json._
import utils._


/** Companion object providing default parameters */
object DecisionTreeClassifier {
  val depth: Int = 3
  val criterion: String="gini"
  val minSamplesSplit: Int = 2
  val verbose: Int = 1
}

/** Decision tree classifier
 * @param depth Depth of the tree
 * @param criterion Function to measure the quality of a split
 * @param minSamplesSplit Minimum number of samples required to split an internal node
 * @param verbose Verbosity of output
 */
class DecisionTreeClassifier(
  depth: Int = DecisionTreeClassifier.depth,
  criterion: String = DecisionTreeClassifier.criterion,
  minSamplesSplit: Int =  DecisionTreeClassifier.minSamplesSplit,
  verbose: Int =  DecisionTreeClassifier.verbose
) extends Classifier {
  def this(json: JsValue) = {
    this(
      depth = JsonMagic.toInt(json, "depth", DecisionTreeClassifier.depth),
      criterion = JsonMagic.toString(json, "criterion", DecisionTreeClassifier.criterion),
      minSamplesSplit = JsonMagic.toInt(json, "minSamplesSplit", DecisionTreeClassifier.minSamplesSplit),
      verbose = JsonMagic.toInt(json, "verbose", DecisionTreeClassifier.verbose)
      )
  }

  val name: String = "DecisionTreeClassifier"

  var decisionTree = new DecisionTree(depth)

  /** Calculates the purity for a cut at threshold at this node
   * @param featureX List of this feature for all instances at this node
   * @param yThisNode List of the corresponding labels
   * @param sampleWeight List of sample weights at this node
   * @param threshold The cut-off threshold to be applied
   * @param crit Function to measure the quality of a split
   * @return The purity of this split and a boolean flag indicating if signal region is greater than the threshold
   */
  def getPurity(featureX: List[Double], yThisNode: List[Int], wThisNode: List[Double], threshold: Double, crit: String): (Double, Boolean) = {
    val rightClasses: List[Tuple2[Int, Double]] =
      featureX.zip(yThisNode zip wThisNode).filter(tup => tup._1 > threshold).map(tup => tup._2)
    val leftClasses: List[Tuple2[Int, Double]] =
      featureX.zip(yThisNode zip wThisNode).filter(tup => tup._1 <= threshold).map(tup => tup._2)
    val rightSig: Double = rightClasses.filter(_._1 == 1).map(_._2).sum
    val rightBkg: Double = rightClasses.filter(_._1 == 0).map(_._2).sum
    val leftSig: Double = leftClasses.filter(_._1 == 1).map(_._2).sum
    val leftBkg: Double = leftClasses.filter(_._1 == 0).map(_._2).sum
    val greater: Boolean = (rightSig > leftSig)
    val purity =
      if (crit == "maxcorrect"){
        if (greater) 1.0 * (rightSig + leftBkg) / featureX.length
        else 1.0 * (leftSig + rightBkg) / featureX.length
      }
      else{
        val impFunc =
          if (crit == "gini") Maths.gini(_)
          else if (crit == "entropy") Maths.entropy(_)
          else throw new NotImplementedError("criterion " + crit + " not implemented")

        // CART cost function
        val m: Double = wThisNode.sum
        val mLeft: Double = leftClasses.map(_._2).sum
        val mRight: Double = rightClasses.map(_._2).sum
        val GLeft: Double =
          if (mLeft != 0) impFunc(List(1.0 * leftSig/mLeft, 1.0 * leftBkg/mLeft))
          else 0
        val GRight: Double =
          if (mRight != 0) impFunc(List(1.0 * rightSig/mRight, 1.0 * rightBkg/mRight))
          else 0
        val cost = GLeft * mLeft / m + GRight * mRight / m
        -cost
      }
    // println("threshold " + threshold + " purity " + purity)
    Tuple2(purity, greater)
  }

  /** Sets optimal feature and cut-off value for this node
   * @param X Feature vectors of instances at this node
   * @param y List of labels of instances at this node
   * @param decTree Decision tree to update
   * @param nodeIndex Index of the node to tune
   * @param sampleWeight Optional sample weights
   */
  def setOptimalCut(X: List[List[Double]], y: List[Int], sampleWeight: List[Double], decTree: DecisionTree, nodeIndex: Int): Unit = {
   // println("Tuning node " + nodeIndex)

   val thisSampleWeight =
     if (sampleWeight.isEmpty) List.fill(y.length)(1.0)
     else sampleWeight

   val nSteps: Int = 10
   val nZooms: Int = 3
   val mean: Double = 1.0 * y.sum / y.length
   val nSamples: Int = y.length

   def zoom(count: Int, min: Double, stepSize: Double, featureX: List[Double], iFeature: Int): Unit =
     if (count < nZooms) {

       def scanSteps(count: Int, purestSplit: Double, maxPurity: Double): Unit =
         if (count < nSteps) {
           val currThresh: Double = min + count * stepSize
           val (currPurity, currGreater) = getPurity(featureX, y, thisSampleWeight, currThresh, criterion)
           decTree.updateNode(nodeIndex, iFeature, currThresh, currGreater, mean, nSamples, currPurity)
           if (maxPurity < currPurity) scanSteps(count+1, currThresh, currPurity)
           else scanSteps(count+1, purestSplit, maxPurity)
         }

       scanSteps(0, 0, Double.MinValue)
       val newMin: Double = -stepSize
       val newStepSize: Double = 2.0 * stepSize / (nSteps - 1)
       zoom(count+1, newMin, newStepSize, featureX, iFeature)
     }

    if (X.length >= minSamplesSplit) {
      val nFeatures: Int = X.head.length
      for (iFeature <- 0 until nFeatures){
        val featureX: List[Double] = X.map(_.apply(iFeature))
        val featureMax: Double = featureX.max
        val featureMin: Double = featureX.min
        val range: Double = featureMax - featureMin
        val max: Double = featureMax + 0.01 * range
        val min: Double = featureMin - 0.01 * range
        val stepSize: Double = (max - min) / (nSteps - 1)
        zoom(0, min, stepSize, featureX, iFeature)
      }
    }
  }

  def train(X: List[List[Double]], y: List[Int], sampleWeight: List[Double] = Nil): Unit = {
    require(X.length == y.length, "number of training instances and labels is not equal")
    for (nodeIndex <- 0 until decisionTree.nNodes) {
      val (thisNodeX, yThisNode, wThisNode) = decisionTree.atNode(nodeIndex, X, y, sampleWeight)
      setOptimalCut(thisNodeX, yThisNode, wThisNode, decisionTree, nodeIndex)
    }
    if (verbose > 0) println(decisionTree)
  }

  def predict(X: List[List[Double]]): List[Int] =
    for (x <- X) yield decisionTree.classify(x)

}
