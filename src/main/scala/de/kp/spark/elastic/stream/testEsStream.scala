package de.kp.spark.elastic.stream
import org.apache.hadoop.conf.Configuration
import java.util.{Properties, UUID}

import de.kp.spark.base.ConfigurationIot

object testEsStream {
    def main(args: Array[String]) {
        val (host,port,resource,index,mapping)  = ConfigurationIot.elastic()
        val kafkaPropertyMap: Properties = ConfigurationIot.kafka()
        val kafkaGroup = kafkaPropertyMap.get("group.id").toString
        val kafkaZklist = kafkaPropertyMap.get("zookeeper.connect").toString
        val topics = ConfigurationIot.topic()

        val conf = new Configuration()
        conf.set("es.nodes",host)
        conf.set("es.port",port)
        conf.set("es.resource",resource)


        conf.set("spark.master","local")
        conf.set("spark.batch.duration","5")

        conf.set("kafka.topics",topics)
        conf.set("kafka.threads","1")
        conf.set("kafka.group", kafkaGroup)
        conf.set("kafka.zklist",kafkaZklist)
        conf.set("kafka.timeout","12000")

      val aa = new EsStream("EsStream",conf)
      aa.run()
  }
}