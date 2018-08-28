package test.nodes

import org.apache.ignite.Ignition
import org.apache.ignite.cache.CacheMode
import org.apache.ignite.configuration.{CacheConfiguration, IgniteConfiguration}
import org.apache.ignite.lang.IgniteFuture
import org.slf4j.LoggerFactory
import test.db.CachePostgresSlickStore

import scala.concurrent.Promise
import scala.util.Try

case class User(id: String, name: String) {

  override def toString = s"User[id = $id - name = $name}"
}

object TestNode extends App {

  implicit class IgniteFutureUtils[T](igniteFuture: IgniteFuture[T]) {
    def toScalaFuture = {
      val promise = Promise[T]()
      igniteFuture.listen { k =>
        promise.tryComplete(Try(k.get))
      }
      promise.future
    }
  }

  val logger = LoggerFactory.getLogger("IgniteLog")

  implicit val ec = scala.concurrent.ExecutionContext.global


  val config = new IgniteConfiguration()
  val cacheCfg = new CacheConfiguration[String, User]("users_table")

  import javax.cache.configuration.FactoryBuilder

  cacheCfg.setCacheStoreFactory(FactoryBuilder.factoryOf(classOf[CachePostgresSlickStore]))
  cacheCfg.setReadThrough(true)
  cacheCfg.setWriteThrough(true)
  cacheCfg.setBackups(1)
  cacheCfg.setCacheMode(CacheMode.REPLICATED)
  cacheCfg.setWriteBehindEnabled(true)
  cacheCfg.setWriteBehindFlushThreadCount(Runtime.getRuntime.availableProcessors())


  config.setCacheConfiguration(cacheCfg)


  val ignition = Ignition.start(config)
  val usersCache = ignition.getOrCreateCache[String, User]("users_table")


  val users = Seq(User("3", "gaston"), User("5", "joako"), User("6", "natalia natalia"))
  users.foreach(user => usersCache.putIfAbsent(user.id, user))

  //usersCache.loadCache(null)
  println(usersCache.get("4"))
  println(usersCache.get("5"))
  println(usersCache.get("6"))
  println(usersCache.get("8"))



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




