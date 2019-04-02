package com.smartdengg.plugin

import com.android.annotations.NonNull
import com.android.build.api.transform.*
import com.android.build.gradle.internal.pipeline.TransformManager
import com.google.common.base.Joiner
import com.google.common.collect.ImmutableList
import com.smartdengg.compile.WeavedClass
import groovy.util.logging.Slf4j
import org.apache.commons.io.FileUtils
import org.gradle.api.Project
import proguard.util.PrintWriterUtil

import java.nio.file.Files
import java.nio.file.Path

import static com.android.builder.model.AndroidProject.FD_OUTPUTS
import static com.google.common.base.Preconditions.checkNotNull

@Slf4j
class DebounceIncrementalTransform extends Transform {

  Project project
  DebounceExtension debounceExt
  Map<String, List<WeavedClass>> weavedVariantClassesMap
  def isApp
  File debounceOutDir

  DebounceIncrementalTransform(Project project,
      Map<String, List<WeavedClass>> weavedVariantClassesMap, boolean isApp) {
    this.project = project
    this.debounceExt = project."${DebounceExtension.NAME}"
    this.weavedVariantClassesMap = weavedVariantClassesMap
    this.isApp = isApp
    this.debounceOutDir = new File(Joiner.on(File.separatorChar).join(project.buildDir,
        FD_OUTPUTS,
        'debounce',
        'logs'))
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
  Collection<File> getSecondaryDirectoryOutputs() {
    return ImmutableList.of(debounceOutDir)
  }

  @Override
  void transform(TransformInvocation invocation)
      throws TransformException, InterruptedException, IOException {

    def weavedClassesContainer = []
    weavedVariantClassesMap[invocation.context.variantName] = weavedClassesContainer

    TransformOutputProvider outputProvider = checkNotNull(invocation.getOutputProvider(),
        "Missing output object for run " + getName())
    if (!invocation.isIncremental()) outputProvider.deleteAll()

    File changedFiles = new File(debounceOutDir,
        Joiner.on(File.separatorChar).join(invocation.context.variantName, 'files.txt'))
    FileUtils.touch(changedFiles)
    PrintWriter writer = PrintWriterUtil.createPrintWriterOut(changedFiles)

    try {
      invocation.inputs.each { inputs ->
        inputs.jarInputs.each { jarInput ->

          Path inputPath = jarInput.file.toPath()
          Path outputPtah = outputProvider.getContentLocation(//
              jarInput.name,
              jarInput.contentTypes,
              jarInput.scopes,
              Format.JAR).toPath()

          /** *************************************************************/
          writer.println "INPUT: ${inputPath.toString()}"
          writer.println "CHANGED: ${jarInput.status} "
          writer.println "OUTPUT: ${outputPtah.toString()} "
          writer.println "INCREMENTAL: ${invocation.isIncremental()}"
          writer.println()
          /** *************************************************************/

          if (invocation.isIncremental()) {

            switch (jarInput.status) {
              case Status.NOTCHANGED:
                break
              case Status.ADDED:
              case Status.CHANGED:
                Files.deleteIfExists(outputPtah)
                Processor.run(inputPath, outputPtah, weavedClassesContainer, Processor.FileType.JAR)
                break
              case Status.REMOVED:
                Files.deleteIfExists(outputPtah)
                break
            }
          } else {
            Processor.run(inputPath, outputPtah, weavedClassesContainer, Processor.FileType.JAR)
          }
        }

        inputs.directoryInputs.each { directoryInput ->

          Path inputRoot = directoryInput.file.toPath()
          Path outputRoot = outputProvider.getContentLocation(//
              directoryInput.name,
              directoryInput.contentTypes,
              directoryInput.scopes,
              Format.DIRECTORY).toPath()

          /** *************************************************************/
          writer.println "INPUT: ${inputRoot.toString()} "
          writer.println "CHANGED: ${directoryInput.changedFiles.size()} "
          writer.println "OUTPUT: ${outputRoot.toString()} "
          writer.println "INCREMENTAL: ${invocation.isIncremental()}"
          writer.println()
          /** *************************************************************/

          if (invocation.isIncremental()) {
            directoryInput.changedFiles.each { File inputFile, Status status ->

              Path inputPath = inputFile.toPath()
              Path outputPath = Utils.toOutputPath(outputRoot, inputRoot, inputPath)

              switch (status) {
                case Status.NOTCHANGED:
                  break
                case Status.ADDED:
                case Status.CHANGED:
                  //direct run byte code
                  Processor.directRun(inputPath, outputPath, weavedClassesContainer)
                  break
                case Status.REMOVED:
                  Files.deleteIfExists(outputPath)
                  break
              }
            }
          } else {
            Processor.run(inputRoot, outputRoot, weavedClassesContainer, Processor.FileType.FILE)
          }
        }
      }
    } finally {
      PrintWriterUtil.closePrintWriter(changedFiles, writer)
      ColoredLogger.logYellow(
          "SUCCESS: Printing files status to [" + PrintWriterUtil.fileName(changedFiles) + "]")
    }
  }
}
