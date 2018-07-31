package test.db

import java.sql.DriverManager

trait PostgresConnection {

  Class.forName("org.postgresql.Driver")

  val  connection = {
    val c = DriverManager.getConnection("jdbc:postgresql://localhost:5432/postgres?user=postgres")
    c.setAutoCommit(true)
    c
  }
}
