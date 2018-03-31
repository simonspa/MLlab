import org.rogach.scallop._
import play.api.libs.json.{JsValue, Json}

import human._
import json._


object CatEncounter {
  class Conf(arguments: Seq[String]) extends ScallopConf(arguments) {
    val hyper = opt[String](
      default = Some(""),
      descr = "hyperparameters to pass to the algorithm"
    )
    verify()
  }

  def main(args: Array[String]): Unit = {
    val conf = new Conf(args)
    val json = JsonMagic.jsonify(conf.hyper())
    val humjson = new Human(json)
    println(humjson)
    humjson.ask()
  }
}
