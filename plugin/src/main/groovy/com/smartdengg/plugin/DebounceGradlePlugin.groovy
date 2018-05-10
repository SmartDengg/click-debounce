package com.smartdengg.plugin

import com.android.build.api.transform.*
import com.android.build.gradle.AppExtension
import com.android.build.gradle.BaseExtension
import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.api.ApplicationVariant
import com.android.build.gradle.api.BaseVariant
import com.android.build.gradle.internal.pipeline.TransformManager
import org.apache.commons.codec.digest.DigestUtils
import org.apache.commons.io.FileUtils
import org.gradle.api.Plugin
import org.gradle.api.Project

public class DebounceGradlePlugin extends Transform implements Plugin<Project> {

  def isApp
  def isLibrary

  @Override public void apply(Project project) {

    def extension = project.extensions.getByType(BaseExtension)

    if (extension instanceof AppExtension) {
      isApp = true

      extension.applicationVariants.each { ApplicationVariant applicationVariant ->

        def dx = project.tasks.findByName("dex${applicationVariant.name.capitalize()}")
        Set<File> inputFiles = dx.inputs.files.files

        inputFiles.each { File file -> println "fileName = ${file.name}"
        }
      }
    } else if (extension instanceof LibraryExtension) {
      isLibrary = true
    }

    extension.registerTransform(this)

    project.afterEvaluate {

      if (isApp) {
        project.android.applicationVariants.each { variant -> runCopyMapping(project, variant)
        }
      }
      if (isLibrary) {
        project.android.libraryVariants.each { variant -> runCopyMapping(project, variant)
        }
      }
    }
  }

  @Override
  String getName() {
    return "debounce"
  }

  @Override
  Set<QualifiedContent.ContentType> getInputTypes() {
    return TransformManager.CONTENT_CLASS
  }

  private static void createAndRunCopyMappingTask(Project project, BaseVariant variant) {
    def copyDebounceMapping = "copyDebounceMappingFor${variant.name.capitalize()}"

    String variantName = variant.name

    def debounceTask = project.tasks.findByName(
        "transformClassesWithDebounceFor${variant.name.capitalize()}")
    def dexTask = project.tasks.findByName("transformClassesWithDexFor${variant.name.capitalize()}")
    def assembleTask = project.tasks.findByName("assemble${variant.name.capitalize()}")

    project.task(copyDebounceMapping) << {
      //    debounceTask.doLast {

      def mappingFile = new File("${debounceTask.temporaryDir}/debouncedMapping.txt")
      def newMappingDir = new File("${project.buildDir}/outputs/debounce/mapping/${variantName}")

      FileUtils.touch(mappingFile)
      if (mappingFile.exists()) {
        FileUtils.copyFileToDirectory(mappingFile, newMappingDir, true)
        mappingFile.delete()
      }

      println "print mapping to ${newMappingDir}/debouncedMapping.txt"
    }

    def copyDebounceMappingTask = project.tasks.findByName(copyDebounceMapping)
    copyDebounceMappingTask.dependsOn assembleTask.taskDependencies.getDependencies(assembleTask)
    assembleTask.dependsOn copyDebounceMappingTask
  }

  @Override
  Set<QualifiedContent.Scope> getScopes() {

    if (isLibrary) return TransformManager.SCOPE_FULL_LIBRARY

    return TransformManager.SCOPE_FULL_PROJECT
  }

  @Override
  boolean isIncremental() {
    return false
  }

  @Override
  void transform(TransformInvocation transformInvocation)
      throws TransformException, InterruptedException, IOException {
    super.transform(transformInvocation)
  }

  @Override
  void transform(Context context, Collection<TransformInput> inputs,
      Collection<TransformInput> referencedInputs, TransformOutputProvider outputProvider,
      boolean isIncremental) throws IOException, TransformException, InterruptedException {

    if (!isIncremental) outputProvider.deleteAll()

    inputs.each { TransformInput input ->

      /**foreach jarinputs*/
      input.jarInputs.each { JarInput jarInput ->

        File file = jarInput.getFile()

        String fileName = file.getName()
        String jarName = jarInput.getName()
        String absolutePath = file.getAbsolutePath()
        String canonicalPath = file.getCanonicalPath()
        String md5Name = DigestUtils.md5Hex(jarName)

        println "==================="
        System.out.println("fileName = " + fileName)
        System.out.println("jarName = " + jarName)
        System.out.println("absolutePath = " + absolutePath)
        System.out.println("canonicalPath = " + canonicalPath)
        System.out.println("md5Name = " + md5Name)

        File dest = outputProvider.getContentLocation(md5Name,
            jarInput.contentTypes, jarInput.scopes, Format.JAR)

        System.out.println("dest = " + dest.getAbsolutePath())
        println "==================="

        def modifiedJar = Utils.modifyJar(jarInput.file, context.getTemporaryDir(), md5Name)

        FileUtils.copyFile(modifiedJar, dest)
        modifiedJar.delete()
      }

      /**foreach directoryInputs*/
      input.directoryInputs.each { DirectoryInput directoryInput ->

        File directory = directoryInput.file

        if (directory.isDirectory()) {
          directory.eachFileRecurse { File file -> Utils.modifyFile(file)
          }
        }

        File dest = outputProvider.getContentLocation(directoryInput.name,
            directoryInput.contentTypes, directoryInput.scopes,
            Format.DIRECTORY)

        FileUtils.copyDirectory(directoryInput.file, dest)
      }
    }

    File mappingFile = new File(context.getTemporaryDir(), "debouncedMapping.txt")
    Utils.saveMappingFile(mappingFile)
  }
}
