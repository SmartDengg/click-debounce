package com.smartdengg.plugin

import com.android.build.api.transform.*
import com.android.build.gradle.AppExtension
import com.android.build.gradle.BaseExtension
import com.android.build.gradle.LibraryExtension
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
    } else if (extension instanceof LibraryExtension) {
      isLibrary = true
    }

    extension.registerTransform(this)

    project.afterEvaluate {

      println "project buildDir = " + project.buildDir
      println "project buildFile = " + project.buildFile

      println "finish......"
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
  void transform(Context context, Collection<TransformInput> inputs,
      Collection<TransformInput> referencedInputs, TransformOutputProvider outputProvider,
      boolean isIncremental) throws IOException, TransformException, InterruptedException {

    inputs.each { TransformInput input ->

      /**foreach jarinputs*/
      input.jarInputs.each { JarInput jarInput ->

        File file = jarInput.getFile()

        String jarName = jarInput.getName()
        String absolutePath = file.getAbsolutePath()
        String canonicalPath = file.getCanonicalPath()
        String md5Name = DigestUtils.md5Hex(absolutePath)

        println "==================="
        System.out.println("file = " + file.getName())
        System.out.println("jarName = " + jarName)
        System.out.println("absolutePath = " + absolutePath)
        System.out.println("canonicalPath = " + canonicalPath)
        System.out.println("md5Name = " + md5Name)

        if (jarName.endsWith(".jar")) jarName = jarName.substring(0, jarName.length() - 4)

        File dest = outputProvider.getContentLocation(jarName + md5Name,
            jarInput.contentTypes, jarInput.scopes, Format.JAR)

        System.out.println("dest = " + dest.getAbsolutePath())
        println "==================="

        def modifiedJar = Utils.modifyJar(jarInput.file, context.getTemporaryDir(), md5Name)

        FileUtils.copyFile(modifiedJar, dest)
      }

      /**foreach directoryInputs*/
      input.directoryInputs.each { DirectoryInput directoryInput ->

        File directory = directoryInput.getFile()

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
  }
}
