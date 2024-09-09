name := "todo-backend-redis"
scalaVersion := "3.5.0"

val http4sVersion = "0.23.27"
val munitVersion = "1.0.1"
libraryDependencies ++= Seq(
  "org.http4s" %% "http4s-ember-server" % http4sVersion,
  "org.http4s" %% "http4s-dsl"          % http4sVersion,
  "org.scalameta" %% "munit" % munitVersion % Test,
)
