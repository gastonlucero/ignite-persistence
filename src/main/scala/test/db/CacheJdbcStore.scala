package test.db

import javax.cache.Cache

import org.apache.ignite.cache.store.CacheStoreAdapter
import org.apache.ignite.lang.IgniteBiInClosure
import test.dbconnections.CommonJdbcConnection
import test.nodes.User

import scala.util.{Failure, Success, Try}

class CacheJdbcStore extends CacheStoreAdapter[String, User] with CommonJdbcConnection with Serializable {

  val create = {
    println("create conection")
    val ps = connection.prepareStatement(s"CREATE TABLE IF NOT EXISTS users_table (id text,name text)")
    ps.executeUpdate()
  }

  override def loadCache(clo: IgniteBiInClosure[String, User], args: AnyRef*): Unit = {
    println("loadAllCache")
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
    println(s"load key $key")
    val ps = connection.prepareStatement(s"SELECT * FROM users_table where id = '$key'")
    val rs = ps.executeQuery()
    if (rs.next())
      User(rs.getString("id"), rs.getString("name"))
    else
      null
  }
}