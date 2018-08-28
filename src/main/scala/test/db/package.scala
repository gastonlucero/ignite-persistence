package test

import java.sql.DriverManager

import slick.jdbc.PostgresProfile
import slick.jdbc.PostgresProfile.api._


package object dbconnections {


  trait PostgresSlickConnection {

    val pgProfile = PostgresProfile.api

    val pgDatabase = Database.forConfig("ignitePostgres")

    val tableName: String
  }

  trait CommonJdbcConnection {
    Class.forName("org.postgresql.Driver")

    val connection = {
      val c = DriverManager.getConnection("jdbc:postgresql://localhost:5432/postgres?user=postgres")
      c.setAutoCommit(true)
      c
    }
  }


}
