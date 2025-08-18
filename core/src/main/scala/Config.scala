package org.aulune


import com.comcast.ip4s.{Host, Port}
import org.http4s.Uri
import pureconfig.*
import pureconfig.error.ExceptionThrown


case class Config(
    app: Config.App,
    oauth: Config.OAuth2,
    postgres: Config.Postgres,
) derives ConfigReader


object Config:
  case class App(
      name: String,
      version: String,
      host: Host,
      port: Port,
      key: String,
      pagination: App.Pagination,
  )
  object App:
    case class Pagination(max: Int, default: Int)

  case class OAuth2(google: OAuth2.GoogleClient)
  object OAuth2:
    case class GoogleClient(clientId: String, secret: String, redirectUrl: String)


  case class Postgres(uri: String, user: String, password: String)

  given ConfigReader[Uri] = ConfigReader.fromString { str =>
    Uri.fromString(str) match
      case Right(v)  => Right(v)
      case Left(err) => Left(ExceptionThrown(err))
  }

  given ConfigReader[Host] = ConfigReader.fromString { str =>
    Host.fromString(str) match
      case Some(h) => Right(h)
      case None    => Left(ExceptionThrown(new Exception("Incorrect host app")))
  }

  given ConfigReader[Port] = ConfigReader.fromString { str =>
    Port.fromString(str) match
      case Some(p) => Right(p)
      case None    => Left(ExceptionThrown(new Exception("Incorrect port app")))
  }
