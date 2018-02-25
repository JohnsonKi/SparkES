package de.kp.spark.base

import com.typesafe.config.ConfigFactory
import java.util.Properties

object ConfigurationIot {

    /* Load configuration for router */
  val path = "application.conf"
  //val path = "application.properties"
  val config = ConfigFactory.load(path)

  def elastic():(String,String,String,String,String) = {
  
    val cfg = config.getConfig("elastic")
    val host = cfg.getString("host")
    val port = cfg.getString("port")
    val resource = cfg.getString("resource")
    val index = cfg.getString("index")
    val mapping = cfg.getString("mapping")

    (host,port,resource,index,mapping)
  
  }

  def kafka():Properties = {
    
    val cfg = config.getConfig("kafka")
    val host = cfg.getString("zkconnecthost")
    val port = cfg.getString("zkconnectport")
    
    val gid = config.getString("consumergroupid")

    val ctimeout = cfg.getString("consumertimeoutms")
    //val stimeout = cfg.getString("consumertimeoutms1")

    val ccommit = cfg.getString("consumercommitms")
    val aoffset = cfg.getString("autooffsetreset")
    
    val params = Map(
      "zookeeper.connect" -> (host + ":" + port),

      "group.id" -> gid,

      //"socket.timeout.ms" -> stimeout,
      "consumer.timeout.ms" -> ctimeout,

      "autocommit.interval.ms" -> ccommit,
      "auto.offset.reset" -> aoffset
    
    )

    val props = new Properties()
    params.map(kv => {
      props.put(kv._1,kv._2)
    })

    props
    
  }
  
  def router():(Int,Int,Int) = {
  
    val cfg = config.getConfig("router")
    val time    = cfg.getInt("time")
    val retries = cfg.getInt("retries")  
    val workers = cfg.getInt("workers")
    
    (time,retries,workers)

  }

  def topic() = config.getString("topic")
  
}