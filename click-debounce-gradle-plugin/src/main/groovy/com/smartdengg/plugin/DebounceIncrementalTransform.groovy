package com.smartdengg.plugin

import com.android.annotations.NonNull
import com.android.build.api.transform.*
import com.android.build.gradle.internal.pipeline.TransformManager
import com.android.utils.FileUtils
import com.smartdengg.compile.WeavedClass

import static com.google.common.base.Preconditions.checkNotNull

class DebounceIncrementalTransform extends Transform {

  DebounceExtension debounceExt
  Map<String, List<WeavedClass>> weavedVariantClassesMap
  def isApp
  def isLibrary
  def isFeature
  private Status status

  DebounceIncrementalTransform(debounceExt, weavedVariantClassesMap, isApp, isLibrary, isFeature) {
    this.debounceExt = debounceExt
    this.weavedVariantClassesMap = weavedVariantClassesMap
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

    def weavedClassesContainer = []
    weavedVariantClassesMap[invocation.context.variantName] = weavedClassesContainer

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

        //        if (debounceExt.loggable) println "jarinput = ${inputJar.path}"
        //        if (debounceExt.loggable) println "outputJar = ${outputJar.path}"

        if (invocation.isIncremental()) {

          status = jarInput.status

          if (status != Status.NOTCHANGED) {
            if (debounceExt.isLoggable()) println "changed jar = ${jarInput.name}:${status}"
          }

          switch (status) {
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

        //        if (debounceExt.loggable) println "directoryInputPath = ${inputDir.path}"
        //        if (debounceExt.loggable) println "directoryOutputPath = ${outputDir.path}"

        if (invocation.isIncremental()) {
          directoryInput.changedFiles.each { File inputFile, Status status ->

            if (debounceExt.loggable) println "changed file = ${inputFile.name}:${status}"

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
