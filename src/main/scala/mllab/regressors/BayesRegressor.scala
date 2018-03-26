package regressors

import breeze.linalg._

import datastructures._
import plotting._
import utils._


/** Bayes regressor
 *
 * following https://stats.stackexchange.com/questions/252577/bayes-regression-how-is-it-done-in-comparison-to-standard-regression
 * @param degree Order of polynomial features to add to the instances (1 for no addition)
 */
class BayesRegressor(degree: Int=1) extends Regressor {

  val name: String = "BayesRegressor"

  var paramA: Double = 0
  var paramB: Double = 0
  var paramS: Double = 0

  // Parameter likelihood assumptions
  val meanA: Double = 1.0
  val sigmaA: Double = 2.0
  val meanB: Double = 0.0
  val sigmaB: Double = 3.0
  val sigmaLike: Double = 2


  def finiteLog(x: Double): Double =
    if (x == 0) -10000 else Math.log(x)

  def optimize(func: (Double, Double, Double) => Double): Tuple3[Double, Double, Double] = {
    val nSteps = 1000
    val numberDimensions: Int = 3

    def maximize(count: Int, maximum: Double, params: Tuple3[Double, Double, Double], ranges: List[List[Double]]): Tuple3[Double, Double, Double] =
      if (count == nSteps) {
        println(s"- final step $count: optimum %.3f, params ".format(maximum) +
          "(%.3f, %.3f, %.3f)".format(params._1, params._2, params._3)
        )
        params
      }
      else {
        if (count % 100 == 0 || (count < 50 && count % 10 == 0) || (count < 5))
          println(s"- optimization step $count: optimum %.3e, params ".format(maximum) +
            "(%.3f, %.3f, %.3f)".format(params._1, params._2, params._3)
          )
        val dimension: Int = scala.util.Random.nextInt(numberDimensions)
        val sign: Int = scala.util.Random.nextInt(2) * 2 - 1
        val step: Double = 1.0 * sign * (ranges(dimension)(1) - ranges(dimension).head) / 100
        // println(s"Step $count: step %.3f in dimension $dimension".format(step))
        val newParams =
          if (dimension == 0) Tuple3(params._1 + step, params._2, params._3)
          else if (dimension == 1) Tuple3(params._1, params._2 + step, params._3)
          else Tuple3(params._1, params._2, params._3 + step)
        val newMaximum = func(newParams._1, newParams._2, newParams._3)
        // if (newMaximum > maximum) println("New maximum " + maximum + " at " + params)
        if (newMaximum > maximum) maximize(count+1, newMaximum, newParams, ranges)
        else maximize(count+1, maximum, params, ranges)
      }

    val intervals: Int = 3
    val rangeA: List[Double] = List(meanA - intervals * sigmaA, meanA + intervals * sigmaA)
    val rangeB: List[Double] = List(meanB - intervals * sigmaB, meanB + intervals * sigmaB)
    val rangeS: List[Double] = List(0, sigmaLike)
    val startParams = (Maths.mean(rangeA), Maths.mean(rangeB), Maths.mean(rangeS))
    val ranges: List[List[Double]] = List(rangeA, rangeB, rangeS)
    maximize(0, Double.MinValue, startParams, ranges)
  }

  def _train(X: List[List[Double]], y: List[Double]): Unit = {
    require(X.length == y.length, "both arguments must have the same length")

    // restrict regression to one feature
    val oneFeatureX = X.transpose.head

    // gaussian priors for linear parameters
    val priorA = (a: Double) => {Maths.normal(a, meanA, sigmaA)}
    val priorB = (b: Double) => {Maths.normal(b, meanB, sigmaB)}
    // rectangular prior for prior sigma
    val priorS = (s: Double) => {Maths.rectangular(s, 0, sigmaLike)}
    // likelihood
    val likelihood = (a: Double, b: Double, s: Double) => {
      (oneFeatureX zip y).map{case (xi, yi) => finiteLog(Maths.normal(yi, a + b * xi, s))}.sum
    }
    // posterior
    val posterior = (a: Double, b: Double, s: Double) => {
      likelihood(a, b, s) + finiteLog(priorA(a)) + finiteLog(priorB(b)) + finiteLog(priorS(s))
    }
    // determine maximum likelihood parameters
    val (maxA: Double, maxB: Double, maxS: Double) = optimize(posterior)
    paramA = maxA
    paramB = maxB
    paramS = maxS

    println("Final estimated parameter means for y <- N(A + B * x, S):")
    println("A = %.3f, B = %.3f, S = %.3f".format(paramA, paramB, paramS))

    // get equidistant points in this feature for line plotting
    val intervals = 3.0
    val minX = min(meanA - intervals * sigmaA, meanB - intervals * sigmaB, 0 - intervals * sigmaLike)
    val maxX = max(meanA + intervals * sigmaA, meanB + intervals * sigmaB, 0 + intervals * sigmaLike)
    val equiVec: DenseVector[Double] = linspace(minX, maxX, 200)
    val xEqui: List[Double] = (for (i <- 0 until equiVec.size) yield equiVec(i)).toList
    // plot some distributions
    val valsA = xEqui zip (xEqui.map(priorA(_)))
    val valsB = xEqui zip (xEqui.map(priorB(_)))
    val valsS = xEqui zip (xEqui.map(priorS(_)))
    Plotting.plotCurves(List(valsA, valsB, valsS), List("A", "B", "S"), xlabel= "Value", name= "plots/reg_Bayes_priors.pdf")
    val valsPosteriorA = xEqui zip (xEqui.map(eq => posterior(eq, paramB, paramS)))
    val valsPosteriorB = xEqui zip (xEqui.map(eq => posterior(paramA, eq, paramS)))
    val valsPosteriorS = xEqui zip (xEqui.map(eq => posterior(paramA, paramB, eq)))
    Plotting.plotCurves(List(valsPosteriorA, valsPosteriorB, valsPosteriorS), List("Posterior(A)", "Posterior(B)", "Posterior(S)"), xlabel= "Value", name= "plots/reg_Bayes_posterior_dep.pdf")
    val valsLikelihoodA = xEqui zip (xEqui.map(eq => likelihood(eq, paramB, paramS)))
    val valsLikelihoodB = xEqui zip (xEqui.map(eq => likelihood(paramA, eq, paramS)))
    val valsLikelihoodS = xEqui zip (xEqui.map(eq => likelihood(paramA, paramB, eq)))
    Plotting.plotCurves(List(valsLikelihoodA, valsLikelihoodB, valsLikelihoodS), List("Likelihood(A)", "Likelihood(B)", "Likelihood(S)"), xlabel= "Value", name= "plots/reg_Bayes_likelihood_dep.pdf")
  }

  def _predict(X: List[List[Double]]): List[Double] = {
    // restrict regression to one feature
    val oneFeatureX = X.transpose.head
    for (x <- oneFeatureX) yield paramA + paramB * x
  }

  def predict(X: List[List[Double]]): List[Double] =
    _predict(DataTrafo.addPolyFeatures(X, degree))

  def train(X: List[List[Double]], y: List[Double]): Unit =
    _train(DataTrafo.addPolyFeatures(X, degree), y)

}
