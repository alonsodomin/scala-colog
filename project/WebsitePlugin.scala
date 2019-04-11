import sbt._
import sbt.Keys._
import sbtunidoc.ScalaUnidocPlugin
import mdoc.DocusaurusPlugin

object WebsitePlugin extends AutoPlugin {

  object autoImport {
    val docusaurusApiFolderName = settingKey[String]("Docusarus destination folder for API docs")
    val copyAPIDocs             = taskKey[Unit]("Copies API docs into Docusaurus website")
  }
  import autoImport._
  import ScalaUnidocPlugin.autoImport._
  import DocusaurusPlugin.autoImport._

  override def requires: Plugins = ScalaUnidocPlugin && DocusaurusPlugin

  override def projectSettings: Seq[Def.Setting[_]] = Seq(
    docusaurusApiFolderName := "api",
    fileMappings := {
      (mappings in (ScalaUnidoc, packageDoc)).value.map {
        case (source, n) =>
          val destFile = docusarusuApiFolder.value / n
          source -> destFile
      }
    },
    copyAPIDocs := {
      val log       = streams.value.log
      val apiFolder = docusarusuApiFolder.value

      log.info(
        s"Copying API documentation into Docusaurus generated site at ${apiFolder.getAbsolutePath}"
      )
      IO.copy(fileMappings.value, CopyOptions().withOverwrite(true))
    },
    docusaurusCreateSite := docusaurusCreateSite.dependsOn(copyAPIDocs).value,
    docusaurusPublishGhpages := docusaurusPublishGhpages.dependsOn(copyAPIDocs).value,
    cleanFiles := {
      docusaurusBuildFolder.value +: cleanFiles.value
    }
  )

  private lazy val docusarusuApiFolder = Def.task {
    val docusaurusDestFolder = docusaurusBuildFolder.value / docusaurusProjectName.value / docusaurusApiFolderName.value
    docusaurusDestFolder.mkdirs()
    docusaurusDestFolder
  }

  private def docusaurusBuildFolder = Def.setting {
    (baseDirectory in ThisBuild).value / "website" / "build"
  }

}
