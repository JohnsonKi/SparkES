package de.kp.spark.base

import org.apache.hadoop.conf.{Configuration => HadoopConfig}
import org.apache.spark.serializer.KryoSerializer
import org.apache.spark.streaming.{Seconds, StreamingContext}
import org.apache.spark.{SparkConf, SparkContext}

import scala.collection.JavaConversions._

trait SparkBase {
  
  protected def createSSCLocal(name:String,config:HadoopConfig):StreamingContext = {

    val sc = createSCLocal(name,config)
    
    /*
     * Batch duration is the time duration spark streaming uses to 
     * collect spark RDDs; with a duration of 5 seconds, for example
     * spark streaming collects RDDs every 5 seconds, which then are
     * gathered int RDDs    
     */
    val batch  = config.get("spark.batch.duration").toInt    
    new StreamingContext(sc, Seconds(batch))

  }
  
  protected def createSCLocal(name:String,config:HadoopConfig):SparkContext = {

      /* Extract Spark related properties from the Hadoop configuration */
      val iterator = config.iterator()
      for (prop <- iterator) {

        val k = prop.getKey()
        val v = prop.getValue()

        if (k.startsWith("spark."))System.setProperty(k,v)

      }

      val runtime = Runtime.getRuntime()
      runtime.gc()

      val cores = runtime.availableProcessors()

      val conf = new SparkConf()
      conf.setMaster("local["+cores+"]")

      conf.setAppName(name)
      conf.set("spark.serializer", classOf[KryoSerializer].getName)

         /* Set the Jetty port to 0 to find a random port */
      conf.set("spark.ui.port", "0")
      conf.set("es.index.auto.create", "true")
      conf.set("es.nodes",config.get("es.nodes"))
      conf.set("es.port",config.get("es.port"))
      conf.set("es.nodes.data.only","false")
      conf.set("es.nodes.wan.only", "true")

    new SparkContext(conf)
		
  }

  protected def createSSCRemote(name:String,config:HadoopConfig):SparkContext = {
    /* Not implemented yet */
    null
  }

  protected def createSCRemote(name:String,config:HadoopConfig):SparkContext = {
    /* Not implemented yet */
    null
  }

}