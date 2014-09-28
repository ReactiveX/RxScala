import bintray.BintrayCredentials.api
import bintray.Keys._

pgpSecretRing := file("key.private.F984A085.asc")

pgpPublicRing := file("key.public.F984A085.asc")

pgpPassphrase := sys.env get "PGP_ENCRYPTION_PASSWORD" map (_ toCharArray)

bintraySettings

repository in bintray := "RxJava"

name in bintray := "RxScala"

licenses += ("The Apache Software License, Version 2.0", url("https://www.apache.org/licenses/LICENSE-2.0.txt"))

bintrayOrganization in bintray := Some("reactivex")

lazy val storeBintrayCredentials = taskKey[Unit]("store bintray credentials")

storeBintrayCredentials := IO.write(credentialsFile.value, api.template(sys env "bintrayUser", sys env "bintrayKey"))

pomExtra := (
  <url>https://github.com/ReactiveX/RxScala</url>
  <scm>
    <url>git@github.com:ReactiveX/RxScala.git</url>
    <connection>scm:git:git@github.com:ReactiveX/RxScala.git</connection>
  </scm>
  <developers>
    <developer>
      <id>benjchristensen</id>
      <name>Ben Christensen</name>
      <email>benjchristensen@netflix.com</email>
    </developer>
    <developer>
      <id>jmhofer</id>
      <name>Joachim Hofer</name>
      <email>jmhofer.github@johoop.de</email>
    </developer>
    <developer>
      <id>samuelgruetter</id>
      <name>Samuel Grütter</name>
    </developer>
    <developer>
      <id>zsxwing</id>
      <name>Shixiong Zhu</name>
    </developer>
  </developers>
)
