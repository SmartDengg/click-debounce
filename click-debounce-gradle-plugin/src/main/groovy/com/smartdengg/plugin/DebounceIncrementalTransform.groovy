package com.smartdengg.plugin

import com.android.annotations.NonNull
import com.android.build.api.transform.*
import com.android.build.gradle.internal.pipeline.TransformManager
import com.android.utils.FileUtils

import static com.google.common.base.Preconditions.checkNotNull

class DebounceIncrementalTransform extends Transform {

  def weavedClassesContainer = []
  def isApp
  def isLibrary
  def isFeature

  DebounceIncrementalTransform(weavedClassesContainer, isApp, isLibrary, isFeature) {
    this.weavedClassesContainer = weavedClassesContainer
    this.isApp = isApp
    this.isLibrary = isLibrary
    this.isFeature = isFeature
  }

  @NonNull
  @Override
  String getName() {
    return "debounce"
  }

  @NonNull
  @Override
  Set<QualifiedContent.ContentType> getInputTypes() {
    return TransformManager.CONTENT_CLASS
  }

  @NonNull
  @Override
  Set<QualifiedContent.Scope> getScopes() {
    if (isLibrary || isFeature) return TransformManager.PROJECT_ONLY
    return TransformManager.SCOPE_FULL_PROJECT
  }

  @Override
  boolean isIncremental() {
    return true
  }

  @Override
  void transform(TransformInvocation invocation)
      throws TransformException, InterruptedException, IOException {

    TransformOutputProvider outputProvider = checkNotNull(invocation.getOutputProvider(),
        "Missing output object for transform " + getName())
    if (!invocation.isIncremental()) outputProvider.deleteAll()

    invocation.inputs.each { inputs ->

      /**/
      inputs.jarInputs.each { jarInput ->

        File inputJar = jarInput.file
        File outputJar = outputProvider.getContentLocation(//
            jarInput.name,
            jarInput.contentTypes,
            jarInput.scopes,
            Format.JAR)

        //        if (Utils.loggable) println "jarinput = ${inputJar.path}"
        //        if (Utils.loggable) println "outputJar = ${outputJar.path}"

        if (invocation.isIncremental()) {

          if (Utils.isLoggable()) println "inIncremental jar = ${jarInput.name}:${jarInput.status}"

          switch (jarInput.getStatus()) {
            case Status.NOTCHANGED:
              break
            case Status.ADDED:
            case Status.CHANGED:
              Processor.transformJar(inputJar, outputJar, weavedClassesContainer)
              break
            case Status.REMOVED:
              FileUtils.delete(outputJar)
              break
          }
        } else {
          Processor.transformJar(inputJar, outputJar, weavedClassesContainer)
        }
      }

      /**/
      inputs.directoryInputs.each { directoryInput ->

        File inputDir = directoryInput.file
        File outputDir = outputProvider.getContentLocation(//
            directoryInput.name,
            directoryInput.contentTypes,
            directoryInput.scopes,
            Format.DIRECTORY)

        //        if (Utils.isLoggable()) println "directoryInputPath = ${inputDir.path}"
        //        if (Utils.isLoggable()) println "directoryOutputPath = ${outputDir.path}"

        if (invocation.isIncremental()) {
          directoryInput.changedFiles.each { File inputFile, Status status ->

            if (Utils.isLoggable()) println "inIncremental file = ${inputFile.name}:${status}"

            switch (status) {
              case Status.NOTCHANGED:
                break
              case Status.ADDED:
              case Status.CHANGED:
                if (!inputFile.isDirectory() && Utils.isMatchCondition(inputFile.name)) {
                  File outputFile = Utils.toOutputFile(outputDir, inputDir, inputFile)
                  Processor.transformFile(inputFile, outputFile, weavedClassesContainer)
                }
                break
              case Status.REMOVED:
                File outputFile = Utils.toOutputFile(outputDir, inputDir, inputFile)
                FileUtils.deleteIfExists(outputFile)
                break
            }
          }
        } else {
          for (File inputFile : FileUtils.getAllFiles(inputDir)) {
            if (Utils.isMatchCondition(inputFile.name)) {
              File outputFile = Utils.toOutputFile(outputDir, inputDir, inputFile)
              Processor.transformFile(inputFile, outputFile, weavedClassesContainer)
            }
          }
        }
      }
    }
  }
}
