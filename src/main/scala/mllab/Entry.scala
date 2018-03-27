package mllab

import org.rogach.scallop._

import classifiers._
import data._
import evaluation._
import plotting._
import regressors._


/** The current entry point for MLlab
 *
 * Currently, this is a collection of usage examples
 *
 * @todo generalize to allow simple integration in other projects
 */
object Mllab {

  /** Parse the input arguments */
  class Conf(arguments: Seq[String]) extends ScallopConf(arguments) {
    val task = opt[String](
      default = Some("clf"),
      descr = "task to execute",
      validate = (s: String) => List("clf", "reg").contains(s)
    )
    val algo = opt[String](
      default = Some("Random"),
      descr = "algorithm to apply"
    )
    val input = opt[String](
      default = Some("src/test/resources"),
      descr = "directory containing the input data"
    )
    val output = opt[String](
      default = Some("plots"),
      descr = "directory to save output"
    )
    val suffix = opt[String](
      default = Some(""),
      descr = "suffix to append on all figure names"
    )
    val format = opt[String](
      default = Some("pdf"),
      descr = "figure format",
      validate = (s: String) => List("pdf", "png").contains(s)
    )
    verify()
  }

  /** Runs the algorithms */
  def main(args: Array[String]): Unit = {
    val conf = new Conf(args)

    println("Execute MLlab!")

    val suff = if (conf.suffix() != "") "_" + conf.suffix() else ""

    if (conf.task() == "clf") {
      println{"Train the classifier"}

      val trainReader = new Reader(conf.input() + "/clf_train.csv", label= -1, index=0)
      trainReader.loadFile()
      val X_train = trainReader.getX()
      val y_train = trainReader.getY().map(_.toInt)

      val testReader = new Reader(conf.input() + "/clf_test.csv", label= -1, index=0)
      testReader.loadFile()
      val X_test = testReader.getX()
      val y_test = testReader.getY().map(_.toInt)

      val clf =
        if (conf.algo() == "Random") new RandomClassifier()
        else if (conf.algo() == "kNN") new kNNClassifier(k=3)
        else if (conf.algo() == "DecisionTree") new DecisionTreeClassifier(depth=3, criterion= "gini")
        else if (conf.algo() == "Perceptron") new PerceptronClassifier(alpha=1, degree=1)
        else if (conf.algo() == "NeuralNetwork") new NeuralNetworkClassifier(alpha=0.01, activation= "tanh", layers=List(2, 10, 10, 2), regularization=0.05)
        else if (conf.algo() == "LogisticRegression") new LogisticRegressionClassifier(alpha=1, maxIter=1000, degree=1)
        else if (conf.algo() == "NaiveBayes") new NaiveBayesClassifier(model= "gaussian")
        else if (conf.algo() == "SVM") new SVMClassifier()
        else throw new IllegalArgumentException("algorithm " + conf.algo() + " not implemented.")
      clf.train(X_train, y_train)

      // println("Check prediction on training set")
      // clf.predict(X_train)

      println{"Apply to test set"}

      println("Now do prediction on test set")
      val y_pred = clf.predict(X_test)
      assert (y_pred.length == y_test.length)
      // println("Predicted values:")
      // for (i <- 0 until Math.min(y_pred.length, 10)) {
      //   println("Test instance " + i + ": prediction " + y_pred(i) + " true value " + y_test(i))
      // }

      println("Evaluate the model")
      Evaluation.matrix(y_pred, y_test)
      println("Precision: %.2f".format(Evaluation.precision(y_pred, y_test)))
      println("Recall: %.2f".format(Evaluation.recall(y_pred, y_test)))
      println("f1: %.2f".format(Evaluation.f1(y_pred, y_test)))

      println("Visualize the data")
      Plotting.plotClfData(X_train, y_train, name= conf.output() + "/clf_" + conf.algo() + "_data" + suff + "." + conf.format())
      Plotting.plotClf(X_train, y_train, clf, name= conf.output() + "/clf_" + conf.algo() + "_clf" + suff + "." + conf.format())
      Plotting.plotClfGrid(X_train, clf, name= conf.output() + "/clf_" + conf.algo() + "_grid" + suff + "." + conf.format())

      for (diag <- clf.diagnostics)
        Plotting.plotCurves(List(diag._2), List(diag._1), name= conf.output() + "/clf_" + conf.algo() + "_" + diag._1 + "" + suff + "." + conf.format())
    }
    else if (conf.task() == "reg") {
      println{"Train the regressor"}

      val trainReader = new Reader(conf.input() + "/reg_train.csv", label= -1, index=0)
      trainReader.loadFile()
      val X_train = trainReader.getX()
      val y_train = trainReader.getY()

      val testReader = new Reader(conf.input() + "/reg_test.csv", label= -1, index=0)
      testReader.loadFile()
      val X_test = testReader.getX()
      val y_test = testReader.getY()

      println("Test feature vector: " + X_train.head + " with label " + y_train.head)

      val reg =
        if (conf.algo() == "Random") new RandomRegressor()
        else if (conf.algo() == "Linear") new LinearRegressor(maxIter=100, degree=1)
        else if (conf.algo() == "DecisionTree") new DecisionTreeRegressor(depth=6)
        else if (conf.algo() == "Bayes") new BayesRegressor(degree=1, model= "gaussian", savePlots= true)
        else throw new IllegalArgumentException("algorithm " + conf.algo() + " not implemented.")
      reg.train(X_train, y_train)

      println{"Apply to test set"}
      val y_pred: List[Double] = reg.predict(X_test)

      println("Evaluate the model")
      println("Mean Squared Error (MSE): %.2f".format(Evaluation.MSE(y_pred, y_test)))
      println("Mean Asolute Error (MAE): %.2f".format(Evaluation.MAE(y_pred, y_test)))
      println("Median Asolute Error (MAE): %.2f".format(Evaluation.MedAE(y_pred, y_test)))
      println("Explained variance score: %.2f".format(Evaluation.explainedVariance(y_pred, y_test)))
      println("R squared score: %.2f".format(Evaluation.RSqared(y_pred, y_test)))
      println("Mean Squared Log Error (MSLE): %.2f".format(Evaluation.MSLE(y_pred, y_test)))

      println("Visualize the data")
      Plotting.plotRegData(X_train, y_train, name= conf.output() + "/reg_" + conf.algo() + "_data" + suff + "." + conf.format())
      Plotting.plotReg(X_train, y_train, reg, name= conf.output() + "/reg_" + conf.algo() + "_reg" + suff + "." + conf.format())

      for (diag <- reg.diagnostics)
        Plotting.plotCurves(List(diag._2), List(diag._1), name= conf.output() + "/reg_" + conf.algo() + "_" + diag._1 + "" + suff + "." + conf.format())
    }
    else throw new IllegalArgumentException("task " + conf.task() + " not implemented. Chose 'clf' or 'reg'.")
  }
}
