package test.nodes

import org.apache.ignite.Ignition
import org.apache.ignite.cache.CacheMode
import org.apache.ignite.configuration.{CacheConfiguration, IgniteConfiguration}
import org.slf4j.LoggerFactory
import test.db.CacheJdbcStore

import scala.util.Random

object TestPostgresPersistence extends App {

  val logger = LoggerFactory.getLogger("IgniteLog")

  implicit val ec = scala.concurrent.ExecutionContext.global

  val config = new IgniteConfiguration()

  val JdbcPersistence = "device_ignite_table"
  val cacheCfg = new CacheConfiguration[String, Device](JdbcPersistence)

  import javax.cache.configuration.FactoryBuilder

  cacheCfg.setCacheStoreFactory(FactoryBuilder.factoryOf(classOf[CacheJdbcStore]))
  cacheCfg.setBackups(1)
  cacheCfg.setCacheMode(CacheMode.REPLICATED)
  cacheCfg.setReadThrough(true)
  cacheCfg.setWriteThrough(true)

  config.setCacheConfiguration(cacheCfg)

  val ignition = Ignition.start(config)
  val jdbcCache = ignition.getOrCreateCache[String, Device](JdbcPersistence)

  def random(min: Int, max: Int): Double = {
    val r = new Random
    min + (max - min) * r.nextDouble
  }

  //Insert to database
  for (i <- 1 to 10) {
    jdbcCache.put(i.toString, Device(i.toString, s"metadata $i", random(-90, 90), random(-180, 180)))
  }

  //delete record with id =1
  jdbcCache.remove("1")


}




