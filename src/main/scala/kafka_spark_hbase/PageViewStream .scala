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


object PageViewStream {
  def main(args: Array[String]): Unit = {
    var masterUrl = "local[2]"
    if (args.length > 0) {
      masterUrl = args(0)
    }

    // Create a StreamingContext with the given master URL
    val conf = new SparkConf().setMaster(masterUrl).setAppName("PageViewStream")
    val ssc = new StreamingContext(conf, Seconds(5))
    
    //ssc.checkpoint("/tmp/checkpoint/")
    
    // Kafka configurations
    val topics = Set("user_events")
    //本地虚拟机ZK地址
    //val brokers = "node5:9092"
    val brokers = "ubuntu:9092"/////////////////////////////////////////////////////ai
    val kafkaParams = Map[String, String](
      "metadata.broker.list" -> brokers,
      "serializer.class" -> "kafka.serializer.StringEncoder")

    // Create a direct stream
    val kafkaStream = KafkaUtils.createDirectStream[String, String, StringDecoder, StringDecoder](ssc, kafkaParams, topics)

    val events = kafkaStream.flatMap(line => {
      val data = JSONObject.fromObject(line._2)
      Some(data)
    })
    events.print()
    // Compute user click times
    val userClicks = events.map(x => (x.getString("uid"), x.getInt("click_count"))).reduceByKey(_ + _)
    userClicks.print()
    userClicks.foreachRDD(rdd => {
      rdd.foreachPartition(partitionOfRecords => {
        partitionOfRecords.foreach(pair => {
          println("----1111111111-----")
          //Hbase配置
          val tableName = "PageViewStream1"
          val hbaseConf = HBaseConfiguration.create()
          hbaseConf.set("hbase.zookeeper.quorum", "ubuntu:2181")/////////////////////////////////////ai
          hbaseConf.set("hbase.zookeeper.property.clientPort", "2181")
          hbaseConf.set("hbase.defaults.for.version.skip", "true")
          //用户ID
          val uid = pair._1
          //点击次数
          val click = pair._2
          //组装数据
          //create "PageViewStream1","Stat"   テーブルの作成
          val put = new Put(Bytes.toBytes(uid))
          put.add("Stat".getBytes, "ClickStat".getBytes, Bytes.toBytes(click))
          val StatTable = new HTable(hbaseConf, TableName.valueOf(tableName))
          println("----22222-------")
          StatTable.setAutoFlush(false, false)
          //写入数据缓存
          StatTable.setWriteBufferSize(3*1024*1024)
          StatTable.put(put)
          //提交
          StatTable.flushCommits()
        })
      })
    })
    ssc.start()
    ssc.awaitTermination()

  }

}
