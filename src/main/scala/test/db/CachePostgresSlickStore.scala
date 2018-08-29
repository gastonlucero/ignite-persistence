package test.db

import javax.cache.Cache

import com.typesafe.config.ConfigFactory
import org.apache.ignite.cache.store.CacheStoreAdapter
import org.apache.ignite.lang.IgniteBiInClosure
import org.slf4j.LoggerFactory
import slick.{dbio, lifted}
import test.dbconnections.PostgresSlickConnection
import test.nodes.User

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.util.{Failure, Success, Try}

class CachePostgresSlickStore extends CacheStoreAdapter[String, User] with PostgresSlickConnection with Serializable {

  import scala.concurrent.ExecutionContext.Implicits.global

  val config = ConfigFactory.load("application.conf")

  val log = LoggerFactory.getLogger("IgniteLog")

  val tableName = config.getString("user.tableName")

  import pgProfile._

  private def txHandler(dbioAction: DBIOAction[Unit, NoStream, Effect.All with Effect.Transactional]) = {
    val txHandler = dbioAction.asTry.flatMap {
      case Failure(e: Throwable) => {
        dbio.DBIO.failed(e)
      }
      case Success(s) => DBIO.successful(s)
    }
    txHandler
  }

  //Slick

  class UserTable(tag: Tag) extends Table[User](tag, Some("public"), tableName) {
    def id = column[String]("id")

    def name = column[String]("name")

    def * = (id, name) <> (User.tupled, User.unapply)

    def pk = primaryKey(s"pk_$tableName", id)

    def uniqueIndex = index(s"idx_$table", id, unique = true)
  }

  val table = lifted.TableQuery[UserTable]

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
        pgDatabase.run(txHandler(dbioAction))
      }
    }
  }

  override def loadCache(clo: IgniteBiInClosure[String, User], args: AnyRef*): Unit = {
    for {
      users <- pgDatabase.run(table.map(u => u).result) recoverWith { case _ => Future(Seq.empty[User]) }
    } yield {
      log.info(s"Loading cache $tableName")
      users.foreach(user => clo.apply(user.id, user))
    }
  }

  override def delete(key: Any) = Try {
    log.info(s"Delete from $tableName value $key")
    val dbioAction = DBIO.seq(
      table.filter(_.id === key.toString).delete
    ).transactionally
    pgDatabase.run(txHandler(dbioAction))
  }

  override def write(entry: Cache.Entry[_ <: String, _ <: User]): Unit = Try {
    log.info(s"Insert into $tableName value ${entry.getValue.toString}")
    val dbioAction = DBIO.seq(
      table.insertOrUpdate(entry.getValue)
    ).transactionally
    pgDatabase.run(txHandler(dbioAction))
  }


  override def load(key: String): User = {
    val loadedUser = pgDatabase.run(table.filter(_.id === key).result.headOption)
    Await.result(loadedUser, 10 second).getOrElse(null)
  }


}