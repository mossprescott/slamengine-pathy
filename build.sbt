import sbt._
import Keys._
import de.heikoseeberger.sbtheader.license.Apache2_0

lazy val buildSettings = Seq(
  organization := "com.slamdata",
  scalaVersion := "2.11.8",
  crossScalaVersions := Seq("2.10.6", "2.11.8")
)

lazy val compilerOptions = Seq(
  "-deprecation",
  "-encoding", "UTF-8",
  "-feature",
  "-language:existentials",
  "-language:higherKinds",
  "-unchecked",
  "-Xfatal-warnings",
  "-Xlint",
  "-Yno-adapted-args",
  "-Ywarn-dead-code",
  "-Ywarn-value-discard",
  "-Xfuture"
)

lazy val baseSettings = Seq(
  scalacOptions ++= compilerOptions ++ (
    CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((2, 11)) => Seq("-Ywarn-unused-import")
      case _             => Nil
    }
  ),
  scalacOptions in (Compile, console) := compilerOptions,
  headers := Map("scala" -> Apache2_0("2014 - 2015", "SlamData Inc."))
)

// Supply the absolute path to release credentials file via RELEASE_CREDS env var
lazy val releaseCreds: Seq[Credentials] =
  Option(System.getenv("RELEASE_CREDS"))
    .map(path => Credentials(new File(path)))
    .toSeq

/**
 * Set PGP_KEY_DIR to the absolute path of the directory containing the
 * "secring.pgp" and "pubring.pgp" keyrings to use for signing artifacts.
 *
 * Set PGP_PASSPHRASE to the passphrase to unlock the private pgp signing key
 *
 * Both options are optional, omiting the directory will result in the default
 * locations being used and omitting the passphrase will result in sbt prompting
 * for it.
 */
lazy val pgpSettings = {
  val keyrings = Option(System.getenv("PGP_KEY_DIR")).toSeq.flatMap(dir => Seq(
    pgpSecretRing := (file(dir) / "secring.pgp"),
    pgpPublicRing := (file(dir) / "pubring.pgp")
  ))

  val passphrase = Option(System.getenv("PGP_PASSPHRASE"))
    .map(s => pgpPassphrase := Some(s.toArray))

  keyrings ++ passphrase.toSeq
}

lazy val publishSettings = Seq(
  organizationName := "SlamData Inc.",
  organizationHomepage := Some(url("http://slamdata.com")),
  homepage := Some(url("https://github.com/slamdata/scala-pathy")),
  licenses := Seq("Apache 2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0")),
  publishTo := {
    val nexus = "https://oss.sonatype.org/"
    if (isSnapshot.value)
      Some("snapshots" at nexus + "content/repositories/snapshots")
    else
      Some("releases"  at nexus + "service/local/staging/deploy/maven2")
  },
  credentials ++= releaseCreds,
  publishMavenStyle := true,
  publishArtifact in Test := false,
  pomIncludeRepository := { _ => false },
  releasePublishArtifactsAction := PgpKeys.publishSigned.value,
  releaseCrossBuild := true,
  autoAPIMappings := true,
  scmInfo := Some(
    ScmInfo(
      url("https://github.com/slamdata/scala-pathy"),
      "scm:git@github.com:slamdata/scala-pathy.git"
    )
  ),
  developers := List(
    Developer(
      id = "slamdata",
      name = "SlamData Inc.",
      email = "contact@slamdata.com",
      url = new URL("http://slamdata.com")
    )
  )
)

val scalazVersion = "7.2.1"
val specs2Version = "3.7.3-scalacheck-1.12"
val scalacheckVersion = "1.12.5"

lazy val allSettings =
  buildSettings ++ baseSettings ++ publishSettings ++ pgpSettings

lazy val noPublishSettings = Seq(
  publish := (),
  publishLocal := (),
  publishArtifact := false
)

lazy val root = (project in file("."))
  .aggregate(core, scalacheck, tests)
  .settings(allSettings)
  .settings(noPublishSettings)
  .settings(Seq(
    name := "pathy"
  ))

lazy val core = (project in file("core"))
  .enablePlugins(AutomateHeaderPlugin)
  .settings(allSettings)
  .settings(Seq(
    name := "pathy-core",
    initialCommands in console := "import pathy._, Path._",
    libraryDependencies ++= Seq(
      "org.scalaz" %% "scalaz-core" % scalazVersion
    )
  ))

lazy val scalacheck = (project in file("scalacheck"))
  .dependsOn(core)
  .enablePlugins(AutomateHeaderPlugin)
  .settings(allSettings)
  .settings(Seq(
    name := "pathy-scalacheck",
    libraryDependencies ++= Seq(
      "org.scalaz" %% "scalaz-core" % scalazVersion,
      "org.scalacheck" %% "scalacheck" % scalacheckVersion
    )
  ))

lazy val specs2 = (project in file("specs2"))
  .dependsOn(scalacheck)
  .enablePlugins(AutomateHeaderPlugin)
  .settings(allSettings)
  .settings(Seq(
    name := "pathy-specs2",
    libraryDependencies ++= Seq(
      "org.specs2" %% "specs2-core" % specs2Version,
      "org.specs2" %% "specs2-scalacheck" % specs2Version
    )
  ))

lazy val tests = (project in file("tests"))
  .dependsOn(core, scalacheck, specs2)
  .enablePlugins(AutomateHeaderPlugin)
  .settings(buildSettings ++ baseSettings)
  .settings(noPublishSettings)
  .settings(Seq(
    name := "pathy-tests",
    libraryDependencies ++= Seq(
      "org.scalaz" %% "scalaz-core" % scalazVersion % "test",
      "org.specs2" %% "specs2-core" % specs2Version % "test",
      "org.specs2" %% "specs2-scalacheck" % specs2Version % "test",
      "org.scalacheck" %% "scalacheck" % scalacheckVersion % "test"
    )
  ))
