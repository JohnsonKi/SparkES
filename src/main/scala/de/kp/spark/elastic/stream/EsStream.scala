package de.kp.spark.elastic.stream


import de.kp.spark.base.SparkBase

import scala.util.parsing.json._
import kafka.serializer.StringDecoder
import org.apache.spark.streaming.dstream.{DStream, InputDStream, ReceiverInputDStream}
import org.apache.spark.storage.StorageLevel
import org.apache.hadoop.conf.{Configuration => HConf}
import org.apache.hadoop.io.{MapWritable, NullWritable, Text}
import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.types.{StringType, StructField, StructType}
import org.apache.spark.sql.{DataFrame, SQLContext}
import org.apache.spark.streaming.{Seconds, StreamingContext}
import org.apache.spark.streaming.kafka.KafkaUtils
import org.elasticsearch.spark.rdd.EsSpark


import scala.collection.mutable
/**
 * EsStream provides base functionality for indexing transformed live streams 
 * from Apache Kafka with Elasticsearch; to appy a customized transformation,
 * the method 'transform' must be overwritten
 */
class EsStream(name:String,conf:HConf) extends SparkBase with Serializable {

  /* Elasticsearch configuration */
  val esResource = conf.get("es.resource")

  /* Kafka configuration */
  val (kc,topics) = getKafkaConf(conf)
  
  def run() {
    
    val sc = createSCLocal(name,conf)
    val ssc = new StreamingContext(sc, Seconds(5))
    /*
     * The KafkaInputDStream returns a Tuple where only the second component
     * holds the respective message; we therefore reduce to a DStream[String]
     */
    val stream: ReceiverInputDStream[(String, String)] = KafkaUtils.createStream[String,String,StringDecoder,StringDecoder](ssc,kc,topics,StorageLevel.MEMORY_AND_DISK)

    val stream1: DStream[String] = stream.map(_._2)
    stream1.print()
    val numbers: Map[String, Int] = Map("one" -> 1, "two" -> 2, "three" -> 3)
    val airports: Map[String, String] = Map("arrival" -> "Otopeni", "SFO" -> "San Fran")

    val rdd: RDD[Map[String, Any]] = sc.makeRDD(Seq(numbers, airports))
    val microbatches = mutable.Queue(rdd)
    val aa: InputDStream[Map[String, Any]] = ssc.queueStream(microbatches)
    //aa.saveToEs(esResource)

    //stream.saveToEs(esResource)
    //EsSparkStreaming.saveToEs(stream, esResource)

    println("OK!")

    stream.foreachRDD { rdd =>
      val sqlContext: SQLContext = org.apache.spark.sql.SQLContext.getOrCreate(SparkContext.getOrCreate())
      val wordCountdf: DataFrame = sqlContext.createDataFrame(rdd).toDF("word", "count")
      val aa: DataFrame = wordCountdf.toDF("word", "count")
      //val wordCountdf = sqlContext.createDataFrame(rdd).toDF("word", "count")
      wordCountdf.show()


            //Index the Word, Count attributes to ElasticSearch Index. You don't need to create any index in Elastic Search
      import org.elasticsearch.spark.sql._
      wordCountdf.saveToEs("aaa/blog")
    }


    //EsSparkStreaming.saveToEs(stream, esResource)
//    stream.foreachRDD { rdd =>
//      println(rdd.toString())
//      EsSpark.saveToEs(rdd, esResource)
//
//      val sc = rdd.context
//      import org.apache.spark.sql.SQLContext
//      val sqlContext = new SQLContext(sc)
//      val log: DataFrame = sqlContext.jsonRDD(rdd)
//
//        log.show()
//      import org.elasticsearch.spark.sql._
//      log.saveToEs(esResource)
//      println("OK!")
//    }





    ssc.start()
    ssc.awaitTermination()    

  }
  
  def transform(stream:DStream[String]) = stream

  
  private def getKafkaConf(config:HConf):(Map[String,String],Map[String,Int]) = {

    val cfg = Map(
      "group.id" -> config.get("kafka.group"),
      "zookeeper.connect" -> config.get("kafka.zklist"),
      "zookeeper.connection.timeout.ms" -> config.get("kafka.timeout")
    )

    val topics = config.get("kafka.topics").split(",").map((_,config.get("kafka.threads").toInt)).toMap   
    
    (cfg,topics)
    
  }
  
  private def prepare(message:String):(Object,Object) = {
      
    val m = JSON.parseFull(message) match {
      case Some(map) => map.asInstanceOf[Map[String,String]]
      case None => Map.empty[String,String]
    }

    val kw = NullWritable.get
    
    val vw = new MapWritable
    for ((k, v) <- m) vw.put(new Text(k), new Text(v))
    
    (kw, vw)
    
  }

}
