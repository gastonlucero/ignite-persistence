package test.nodes

import org.apache.ignite.Ignition
import org.apache.ignite.cache.CacheMode
import org.apache.ignite.configuration.{CacheConfiguration, IgniteConfiguration}
import org.apache.ignite.lang.IgniteFuture
import org.slf4j.LoggerFactory
import test.db.CacheJdbcStore

import scala.concurrent.Promise
import scala.util.{Failure, Random, Success, Try}

object TestPostgresSlickPersistence extends App {

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

  implicit class IgniteFutureUtils[T](igniteFuture: IgniteFuture[T]) {
    def toScalaFuture = {
      val promise = Promise[T]()
      igniteFuture.listen { k =>
        promise.tryComplete(Try(k.get))
      }
      promise.future
    }
  }

  for (i <- 1 to 100) {
    jdbcCache.put(i.toString, Device(i.toString, s"metadata $i", random(-90, 90), random(-180, 180)))
  }

  for (i <- 1 to 100) {
    println(jdbcCache.get(i.toString))
  }

  jdbcCache.removeAsync("1").toScalaFuture onComplete {
    case Success(removed) => println(s"Removed = $removed")
    case Failure(f) => println("Remove fail")
  }
}




