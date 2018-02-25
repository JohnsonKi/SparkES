package kafka_spark_hbase

import kafka.serializer.StringDecoder
import net.sf.json.JSONObject
import org.apache.hadoop.hbase.client.{HTable, Put}
import org.apache.hadoop.hbase.util.Bytes
import org.apache.hadoop.hbase.{HBaseConfiguration, TableName}
import org.apache.spark.SparkConf
import org.apache.spark.streaming.kafka.KafkaUtils
import org.apache.spark.streaming.{Seconds, StreamingContext}
import org.apache.spark.streaming.dstream.DStream.toPairDStreamFunctions
import org.apache.spark.sql._
import org.elasticsearch.spark.sql._
import org.apache.spark.SparkContext._
import org.apache.spark.SparkConf
import org.apache.spark._
import org.apache.spark.streaming.kafka.KafkaUtils
import org.apache.spark.streaming._
import org.apache.spark.streaming.StreamingContext._
import org.apache.spark.SparkContext._
import org.apache.spark.SparkConf
import org.apache.spark.sql._
import org.apache.spark.streaming.dstream.{DStream, InputDStream}
import org.elasticsearch.spark.sql._

object insert_hbase_elastic {
  def main(args: Array[String]): Unit = {
    var masterUrl = "local[2]"
    if (args.length > 0) {
      masterUrl = args(0)
    }

    // Create a StreamingContext with the given master URL
    val conf = new SparkConf().setMaster(masterUrl).setAppName("PageViewStream")
               .set("es.index.auto.create", "true")
               .set("es.nodes","ubuntu")////////////////////////////////////////////////ai////////////////////////////////////////////////
               .set("es.port","9200")
               .set("es.nodes.wan.only", "true")
    val ssc = new StreamingContext(conf, Seconds(5))
    val elasticResource = "apps0823suwa/blog"
    
    ssc.checkpoint("/tmp/checkpoint/")
    
    // Kafka configurations
    val topics = Set("user_events")
    //val topics = Set("OsoyooData")
    //本地虚拟机ZK地址
    val brokers = "ubuntu:9092"////////////////////////////////////////////////////////////////////////////////////
    val kafkaParams = Map[String, String](
      "metadata.broker.list" -> brokers,
      "serializer.class" -> "kafka.serializer.StringEncoder")

    // Create a direct stream
    val kafkaStream: InputDStream[(String, String)] = KafkaUtils.createDirectStream[String, String, StringDecoder, StringDecoder](ssc, kafkaParams, topics)

    val events: DStream[JSONObject] = kafkaStream.flatMap(line => {
      val data: JSONObject = JSONObject.fromObject(line._2)
      Some(data)
    })
    events.print(5)
    // Compute user click times
    //val userClicks: DStream[(String, String)] = events.map(x => (x.getString("vin"), x.getString("temperature"))).reduceByKey(_ + _)
    val userClicks = events.map(x => (x.getString("uid"), x.getInt("click_count"))).reduceByKey(_ + _)
    userClicks.print()
    userClicks.foreachRDD(rdd => {
      rdd.foreachPartition(partitionOfRecords => {
        partitionOfRecords.foreach(f = pair => {

          //Hbase配置
          val tableName = "PageViewStream1"
          val hbaseConf = HBaseConfiguration.create()
          hbaseConf.set("hbase.zookeeper.quorum", "ubuntu:2181")//////////////////////////////////////////////////////////////
          hbaseConf.set("hbase.zookeeper.property.clientPort", "2181")
          hbaseConf.set("hbase.defaults.for.version.skip", "true")
          //用户ID
          val uid: String = pair._1
          //点击次数
          val click = pair._2
          //组装数据
          //create "PageViewStream","Stat"   テーブルの作成
          val put = new Put(Bytes.toBytes(uid))
          put.add("Stat".getBytes, "ClickStat".getBytes, Bytes.toBytes(click))
          val StatTable = new HTable(hbaseConf, TableName.valueOf(tableName))

          StatTable.setAutoFlush(false, false)
          //写入数据缓存
          StatTable.setWriteBufferSize(3 * 1024 * 1024)
          StatTable.put(put)
          //提交
          StatTable.flushCommits()
        })
      })
    })
    
    val userClicks2: DStream[(String, String)] = events.map(x => (x.getString("uid"), x.getString("click_count"))).reduceByKeyAndWindow(_ + _, _ +  _, new Duration(150 * 1000), new Duration(10 * 1000), 2)
    //val userClicks2: DStream[(String, String)] = events.map(x => (x.getString("uid"), x.getInt("click_count")).reduceByKeyAndWindow(_ + _, _ +  _, new Duration(150 * 1000), new Duration(10 * 1000), 2)
    userClicks2.print()
    //kafkaStream.map(_._2).foreachRDD { rdd =>
    userClicks2.map(_._2).foreachRDD { rdd =>
      val sc = rdd.context
      val sqlContext = new SQLContext(sc)
      //val log = sqlContext.jsonRDD(rdd)
      val log: DataFrame = sqlContext.jsonRDD(rdd)
//      log.printSchema()
      log.show()
      log.saveToEs(elasticResource)
    }
    
    ssc.start()
    ssc.awaitTermination()

  }

}
