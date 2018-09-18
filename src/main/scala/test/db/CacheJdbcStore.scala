package test.db

import java.sql.{DriverManager, ResultSet}
import javax.cache.Cache

import org.apache.ignite.cache.store.CacheStoreAdapter
import org.apache.ignite.lang.IgniteBiInClosure
import test.nodes.Device

import scala.util.{Failure, Success, Try}


trait CommonJdbcConnection {
  Class.forName("org.postgresql.Driver")

  val connection = {
    val c = DriverManager.getConnection("jdbc:postgresql://localhost:5432/postgres?user=postgres")
    c.setAutoCommit(true)
    c
  }
}

class CacheJdbcStore extends CacheStoreAdapter[String, Device] with CommonJdbcConnection with Serializable {

  val create = {
    println("create conection")
    val ps = connection.prepareStatement(s"CREATE TABLE IF NOT EXISTS device_ignite_table (id text,metadata text,lat double precision,lon double precision)")
    ps.executeUpdate()
  }

  override def loadCache(clo: IgniteBiInClosure[String, Device], args: AnyRef*): Unit = {
    println("loadAllCache")
    val ps = connection.prepareStatement(s"SELECT * FROM device_ignite_table")
    val rs = ps.executeQuery()
    while (rs.next())
      clo.apply(rs.getString("id"), rsToDevice(rs))
  }

  override def delete(key: Any) = Try {
    val ps = connection.prepareStatement(s"DELETE FROM device_ignite_table WHERE id = '$key'")
    ps.executeUpdate()
  } match {
    case Success(_) => println(s"Value delete from table")
    case Failure(f) => println(s"Delete error $f")
  }

  override def write(entry: Cache.Entry[_ <: String, _ <: Device]): Unit = Try {
    // Must be an UPSERT
    val ps = connection.prepareStatement("INSERT INTO device_ignite_table (id,metadata,lat,lon) VALUES (?,?,?,?)")
    ps.setString(1, entry.getKey)
    ps.setString(2, entry.getValue.metadata)
    ps.setDouble(3, entry.getValue.lat)
    ps.setDouble(4, entry.getValue.lon)
    ps.executeUpdate()
  } match {
    case Success(_) => println(s"Value put in table")
    case Failure(f) => println(s"Insert error $f")
  }

  override def load(key: String): Device = {
    println(s"load key $key")
    val ps = connection.prepareStatement(s"SELECT * FROM device_ignite_table where id = '$key'")
    val rs = ps.executeQuery()
    if (rs.next())
      rsToDevice(rs)
    else
      null
  }

  private def rsToDevice(rs:ResultSet) : Device = Device(rs.getString("id"), rs.getString("metadata"), rs.getDouble("lat"), rs.getDouble("lon"))
}