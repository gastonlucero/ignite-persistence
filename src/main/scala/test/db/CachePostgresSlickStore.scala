package test.db

import javax.cache.Cache

import com.typesafe.config.ConfigFactory
import org.apache.ignite.cache.store.CacheStoreAdapter
import org.apache.ignite.lang.IgniteBiInClosure
import org.slf4j.LoggerFactory
import slick.jdbc.PostgresProfile
import slick.jdbc.PostgresProfile.api._
import slick.lifted
import test.nodes.Device

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.util.{Failure, Success, Try}

trait PostgresSlickConnection {

  val pgProfile = PostgresProfile.api

  val pgDatabase = Database.forConfig("ignitePostgres")

  val tableName: String
}

class CachePostgresSlickStore extends CacheStoreAdapter[String, Device] with PostgresSlickConnection with Serializable {

  import scala.concurrent.ExecutionContext.Implicits.global

  val config = ConfigFactory.load("application.conf")

  val log = LoggerFactory.getLogger("IgniteLog")

  val tableName = "device_ignite_slick_table"

  import pgProfile._

  //Slick

  class DeviceTable(tag: Tag) extends Table[Device](tag, Some("public"), tableName) {

    def id = column[String]("id")

    def metadata = column[String]("metadata")

    def lat = column[Double]("lat")

    def lon = column[Double]("lon")

    def * = (id, metadata, lat, lon) <> (Device.tupled, Device.unapply)

    def pk = primaryKey(s"pk_$tableName", id)

    def uniqueIndex = index(s"idx_$table", id, unique = true)
  }

  val table = lifted.TableQuery[DeviceTable]

  val startup = createSchema

  private def createSchema(): Unit = {
    pgDatabase.run(table.exists.result) onComplete {
      case Success(exists) =>
        log.info("Schema already exists")
      case Failure(e) => {
        log.info(s"Creating schema for $tableName")
        val dbioAction = (
          for {
            _ <- table.schema.create
          } yield ()
          ).transactionally
        pgDatabase.run(dbioAction)
      }
    }
  }

  override def loadCache(clo: IgniteBiInClosure[String, Device], args: AnyRef*): Unit = {
    for {
      devices <- pgDatabase.run(table.map(u => u).result) recoverWith { case _ => Future(Seq.empty[Device]) }
    } yield {
      log.info(s"Loading cache $tableName")
      devices.foreach(device => clo.apply(device.id, device))
    }
  }

  override def delete(key: Any) = Try {
    log.info(s"Delete from $tableName value $key")
    val dbioAction = DBIO.seq(
      table.filter(_.id === key.toString).delete
    ).transactionally
    pgDatabase.run(dbioAction)
  }

  override def write(entry: Cache.Entry[_ <: String, _ <: Device]): Unit = Try {
    log.info(s"Insert into $tableName value ${entry.getValue.toString}")
    val dbioAction = DBIO.seq(table.insertOrUpdate(entry.getValue)).transactionally
    pgDatabase.run(dbioAction)
  }

  override def load(key: String): Device = {
    val loadedUser = pgDatabase.run(table.filter(_.id === key).result.headOption)
    Await.result(loadedUser, 10 second).getOrElse(null)
  }


}