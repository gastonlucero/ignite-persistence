package test.nodes

import org.apache.ignite.Ignition
import org.apache.ignite.cache.CacheMode
import org.apache.ignite.configuration.{CacheConfiguration, IgniteConfiguration}
import org.apache.ignite.lang.IgniteFuture
import org.slf4j.LoggerFactory
import test.db.CacheJdbcStore

import scala.concurrent.Promise
import scala.util.{Random, Try}

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

  for (i <- 1 to 100) {
    jdbcCache.put(i.toString, Device(i.toString, s"metadata $i", random(-90, 90), random(-180, 180)))
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

  //usersCache.removeAsync("6").toScalaFuture
  //  val dos = Future {
  //    f.foreach(println)
  //  }
  //
  //  val p = Promise[IgniteFuture[java.lang.Boolean]]()
  //
  //  val f = p.future
  //
  //  p.success(usersCache.removeAsync("6"))
  //  f onComplete {
  //    case Success(s) => s.get()
  //    case Failure(f) => f.printStackTrace()
  //  }
  //
  //
  //  usersCache.removeAsync("6").listenAsync(ic => {
  //    println(usersCache.get("6"))
  //    ic.get()
  //  }, ec)
  //
  //
  //
  //  Thread.sleep(10000)
  //  System.exit(1)
}




