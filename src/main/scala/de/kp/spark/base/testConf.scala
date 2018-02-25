package de.kp.spark.base

object testConf {
    def main(args: Array[String]): Unit = {

      val elastic = ConfigurationIot.elastic()
      println("fffffff" + elastic.toString())

      val kafka = ConfigurationIot.kafka()
      println("kafka " + kafka.toString()) 
 
      val topic = ConfigurationIot.topic()
      println("fffffff" + topic.toString())
      
      val router = ConfigurationIot.router()
      println("fffffff" + router.toString())
  }
}