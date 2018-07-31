package test.nodes

import javax.cache.Cache
import scala.concurrent.{Future, Promise}
import scala.util.{Failure, Success, Try}

import org.apache.ignite.cache.store.CacheStoreAdapter
import org.apache.ignite.lang.IgniteBiInClosure
import test.db.PostgresConnection

case class User(id: String, name: String) {

  override def toString = s"User[id = $id - name = $name}"
}

object TestNode extends App {

//  val config = new IgniteConfiguration()
//  val cacheCfg = new CacheConfiguration[String, User]("users_table")
//
//  import javax.cache.configuration.FactoryBuilder
//
//  cacheCfg.setCacheStoreFactory(FactoryBuilder.factoryOf(classOf[CachePostgresStore]))
//
//  cacheCfg.setReadThrough(true)
//  cacheCfg.setWriteThrough(true)
//  cacheCfg.setWriteBehindEnabled(true)
//  config.setCacheConfiguration(cacheCfg)
//
//  val ignition = Ignition.start(config)
//  val usersCache = ignition.getOrCreateCache[String, User]("users_table")

  //  val users = Seq(User("4", "gaston"), User("5", "joako"), User("6", "natalia natalia"))
  //  for {
  //    user <- users
  //  } yield {
  //    usersCache.put(user.id, user)
  //  }

  //  usersCache.getAll(Seq("1", "2", "3", "4", "5", "6").toSet.asJava).asScala.foreach(println)

  // usersCache.localLoadCache(null)
  implicit val ec = scala.concurrent.ExecutionContext.global

  val p = Promise[String]()
  val f = p.future

  val uno = Future {
    p success "1"

  }
  f onComplete {
    case Success(s) =>  println(s)
    case Failure(f) => println(f)
  }

//  val dos = Future {
//    f.foreach(println)
//  }

//  val p = Promise[IgniteFuture[java.lang.Boolean]]()
//
//  val f = p.future

//  p.success(usersCache.removeAsync("6"))
//  f onComplete{
//    case Success(s) => s.get()
//    case Failure(f) => f.printStackTrace()
//  }
//
//
//  usersCache.removeAsync("6").listenAsync(ic => {
//    println(usersCache.get("6"))
//    ic.get()
//  }, ec)

  Thread.sleep(10000)
  //  System.exit(1)
}

class CachePostgresStore extends CacheStoreAdapter[String, User] with PostgresConnection {

  val create = {
    val ps = connection.prepareStatement(s"CREATE TABLE IF NOT EXISTS users_table (id text,name text)")
    ps.executeUpdate()
  }

  override def loadCache(clo: IgniteBiInClosure[String, User], args: AnyRef*): Unit = {
    val ps = connection.prepareStatement(s"SELECT * FROM users_table")
    val rs = ps.executeQuery()
    while (rs.next())
      clo.apply(rs.getString("id"), User(rs.getString("id"), rs.getString("name")))
  }

  override def delete(key: Any) = Try {
    val ps = connection.prepareStatement(s"DELETE FROM users_table WHERE id = '$key'")
    ps.executeUpdate()
  } match {
    case Success(_) => println(s"Value delete from table")
    case Failure(f) => println(s"Delete error $f")
  }

  override def write(entry: Cache.Entry[_ <: String, _ <: User]): Unit = Try {
    val ps = connection.prepareStatement("INSERT  INTO users_table (id,name) VALUES (?,?)")
    ps.setString(1, entry.getKey)
    ps.setString(2, entry.getValue.name)
    ps.executeUpdate()
  } match {
    case Success(_) => println(s"Value put in table")
    case Failure(f) => println(s"Insert error $f")
  }

  override def load(key: String): User = {
    val ps = connection.prepareStatement(s"SELECT * FROM users_table where id = '$key'")
    val rs = ps.executeQuery()
    if (rs.next())
      User(rs.getString("id"), rs.getString("name"))
    else
      null
  }
}


