name := """todo-api"""
organization := "com.github.kamekoopa"

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.12.6"

resolvers += "elasticsearch" at "https://artifacts.elastic.co/maven"

libraryDependencies ++= Seq(
  guice,
  "com.squareup.okhttp3"     % "okhttp"                        % "3.8.1",
  "com.h2database"           %  "h2"                           % "1.4.+",
  "org.mariadb.jdbc"         %  "mariadb-java-client"          % "1.5.+",
  "org.scalikejdbc"          %% "scalikejdbc"                  % scalikejdbcVersion,
  "org.scalikejdbc"          %% "scalikejdbc-config"           % scalikejdbcVersion,
  "org.scalikejdbc"          %% "scalikejdbc-play-initializer" % scalikejdbcPlayVersion,
  "org.flywaydb"             %% "flyway-play"                  % "4.0.0",
  "org.elasticsearch"        %  "elasticsearch"                % esversion exclude("org.apache.logging.log4j", "log4j-slf4j-impl"),
  "org.elasticsearch.client" %  "transport"                    % esversion exclude("org.apache.logging.log4j", "log4j-slf4j-impl"),
  "org.elasticsearch.client" %  "x-pack-transport"             % esversion exclude("org.apache.logging.log4j", "log4j-slf4j-impl"),
  "org.scalikejdbc"          %% "scalikejdbc-test"             % scalikejdbcVersion  % Test,
  "org.scalatestplus.play"   %% "scalatestplus-play"           % "3.1.0"             % Test,
  "org.scalacheck"           %% "scalacheck"                   % "1.14.0"            % Test
)

lazy val scalikejdbcVersion = "3.2.+"

lazy val scalikejdbcPlayVersion = "2.6.0-scalikejdbc-3.2"

// lazy val esversion = "6.3.1"

lazy val esversion = "6.2.4"

