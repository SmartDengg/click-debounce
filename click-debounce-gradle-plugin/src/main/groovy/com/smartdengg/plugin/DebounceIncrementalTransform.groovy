package com.smartdengg.plugin

import com.android.annotations.NonNull
import com.android.build.api.transform.*
import com.android.build.gradle.internal.pipeline.TransformManager
import com.android.utils.FileUtils
import com.smartdengg.compile.WeavedClass
import groovy.util.logging.Slf4j

import java.nio.file.Files
import java.nio.file.Path

import static com.google.common.base.Preconditions.checkNotNull

@Slf4j
class DebounceIncrementalTransform extends Transform {

  DebounceExtension debounceExt
  Map<String, List<WeavedClass>> weavedVariantClassesMap
  def isApp

  DebounceIncrementalTransform(debounceExt, weavedVariantClassesMap, isApp) {
    this.debounceExt = debounceExt
    this.weavedVariantClassesMap = weavedVariantClassesMap
    this.isApp = isApp
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
    if (isApp) return TransformManager.SCOPE_FULL_PROJECT
    return TransformManager.PROJECT_ONLY
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
        "Missing output object for processJarPath " + getName())
    if (!invocation.isIncremental()) outputProvider.deleteAll()

    invocation.inputs.each { inputs ->

      /**/
      inputs.jarInputs.each { jarInput ->

        Path inputRoot = jarInput.file.toPath()
        Path outputRoot = outputProvider.getContentLocation(//
            jarInput.name,
            jarInput.contentTypes,
            jarInput.scopes,
            Format.JAR).toPath()

        println """
            INPUT: ${inputRoot.toString()} 
            CHANGED: ${jarInput.status} 
            OUTPUT: ${outputRoot.toString()} 
            INCREMENTAL: ${invocation.isIncremental()}
        """

        if (invocation.isIncremental()) {

          switch (jarInput.status) {
            case Status.NOTCHANGED:
              break
            case Status.ADDED:
            case Status.CHANGED:
              FileUtils.delete(outputRoot)
              Processor.processJarPath(inputRoot, outputRoot, weavedClassesContainer)
              break
            case Status.REMOVED:
              FileUtils.delete(outputRoot)
              break
          }
        } else {
          Processor.processJarPath(inputRoot, outputRoot, weavedClassesContainer)
        }
      }

      /**/
      inputs.directoryInputs.each { directoryInput ->

        Path inputRoot = directoryInput.file.toPath()
        Path outputRoot = outputProvider.getContentLocation(//
            directoryInput.name,
            directoryInput.contentTypes,
            directoryInput.scopes,
            Format.DIRECTORY).toPath()

        println """
            INPUT: ${inputRoot.toString()} 
            CHANGED: ${directoryInput.changedFiles.size()} 
            OUTPUT: ${outputRoot.toString()} 
            INCREMENTAL: ${invocation.isIncremental()}
        """

        if (invocation.isIncremental()) {
          directoryInput.changedFiles.each { File inputFile, Status status ->

            Path inputPath = inputFile.toPath()
            Path outputPath = Utils.toOutputPath(outputRoot, inputRoot, inputPath)

            if (debounceExt.loggable) println "changed: ${inputFile.name}:${status}"

            switch (status) {
              case Status.NOTCHANGED:
                break
              case Status.ADDED:
              case Status.CHANGED:
                //direct process byte code
                Processor.processBytecode(inputPath, outputPath, weavedClassesContainer)
                break
              case Status.REMOVED:
                Files.delete(outputPath)
                break
            }
          }
        } else {
          Processor.processFilePath(inputRoot, outputRoot, weavedClassesContainer)
        }
      }
    }
  }
}
