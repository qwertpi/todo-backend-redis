name                 := "todo-backend-redis"
scalaVersion         := "3.5.0"
Compile / run / fork := true
scalacOptions ++= Seq(
    "-deprecation",
    "-explain",
    "-feature",
    "-new-syntax",
    "-unchecked",
    "-Wsafe-init",
    "-Wunused:all",
    "-Wvalue-discard",
)

val http4sVersion  = "0.23.27"
val jedisVersion   = "5.1.5"
val logbackVersion = "1.5.8"
val munitVersion   = "1.0.1"
val sttpVersion    = "3.9.8"
val circeVersion   = "0.14.10"
libraryDependencies ++= Seq(
    "ch.qos.logback"                 % "logback-classic"     % logbackVersion,
    "com.softwaremill.sttp.client3" %% "core"                % sttpVersion,
    "io.circe"                      %% "circe-generic"       % circeVersion,
    "io.circe"                      %% "circe-parser"        % circeVersion,
    "org.http4s"                    %% "http4s-circe"        % http4sVersion,
    "org.http4s"                    %% "http4s-ember-server" % http4sVersion,
    "org.http4s"                    %% "http4s-dsl"          % http4sVersion,
    "org.scalameta" %% "munit" % munitVersion % Test,
    "redis.clients"  % "jedis" % jedisVersion,
)
