package spark_elastic.SparkES

import org.apache.spark.SparkConf
import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._

import org.elasticsearch.spark._

/**
  * Created by cao on 16-3-25.
  */
object Demo1 {
  def main(args: Array[String]) {
    val conf = new SparkConf().setAppName("ESDemo1").setMaster("local[2]")
    conf.set("es.index.auto.create", "true")
    conf.set("es.nodes","192.168.3.115")
    conf.set("es.port","9200")
    val sc = new SparkContext(conf)

    val numbers = Map("one" -> 1, "two2" -> 2, "three" -> 3)
    val airports = Map("arrival" -> "Otopeni", "SFO" -> "San Fran")

    sc.makeRDD(Seq(numbers,airports)).saveToEs("sparkdemo/docs")
  }
}