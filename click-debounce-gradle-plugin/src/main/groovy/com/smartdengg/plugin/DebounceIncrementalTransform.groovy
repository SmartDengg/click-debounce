package com.smartdengg.plugin

import com.android.annotations.NonNull
import com.android.build.api.transform.*
import com.android.build.gradle.AppPlugin
import com.android.build.gradle.internal.pipeline.TransformManager
import com.google.common.base.Joiner
import com.google.common.collect.ImmutableList
import com.smartdengg.compile.WovenClass
import com.smartdengg.plugin.api.DebounceExtension
import com.smartdengg.plugin.internal.ColoredLogger
import com.smartdengg.plugin.internal.ForkJoinExecutor
import com.smartdengg.plugin.internal.Processor
import com.smartdengg.plugin.internal.Utils
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

  static final TASK_NAME = 'debounce'

  Project project
  DebounceExtension debounceExt
  Map<String, List<WovenClass>> wovenVariantClassesMap
  File debounceOutDir

  DebounceIncrementalTransform(Project project,
      Map<String, List<WovenClass>> wovenVariantClassesMap) {
    this.project = project
    this.debounceExt = project."${DebounceExtension.NAME}"
    this.wovenVariantClassesMap = wovenVariantClassesMap
    this.debounceOutDir = new File(Joiner.on(File.separatorChar).join(project.buildDir,
        FD_OUTPUTS,
        'debounce',
        'logs'))
  }

  @NonNull
  @Override
  String getName() {
    return TASK_NAME
  }

  @NonNull
  @Override
  Set<QualifiedContent.ContentType> getInputTypes() {
    return TransformManager.CONTENT_CLASS
  }

  @NonNull
  @Override
  Set<QualifiedContent.Scope> getScopes() {
    if (project.plugins.hasPlugin(AppPlugin)) return TransformManager.SCOPE_FULL_PROJECT
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

    ForkJoinExecutor executor = ForkJoinExecutor.instance

    def wovenClassesContainer = []
    wovenVariantClassesMap[invocation.context.variantName] = wovenClassesContainer

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

          /************************** Write to file START *************************************/
          Utils.writeStatusToFile(writer, inputPath.toString(), jarInput.status,
              outputPtah.toString(),
              invocation.isIncremental())
          /************************** Write to file END *************************************/

          if (invocation.isIncremental()) {

            switch (jarInput.status) {
              case Status.NOTCHANGED:
                break
              case Status.ADDED:
              case Status.CHANGED:
                Files.deleteIfExists(outputPtah)
                Processor.run(inputPath, outputPtah, wovenClassesContainer, debounceExt.exclusion,
                    Processor.Input.JAR)
                break
              case Status.REMOVED:
                Files.deleteIfExists(outputPtah)
                break
            }
          } else {
            executor.execute {
              Processor.run(inputPath, outputPtah, wovenClassesContainer, debounceExt.exclusion,
                  Processor.Input.JAR)
            }
          }
        }

        inputs.directoryInputs.each { directoryInput ->

          Path inputRoot = directoryInput.file.toPath()
          Path outputRoot = outputProvider.getContentLocation(//
              directoryInput.name,
              directoryInput.contentTypes,
              directoryInput.scopes,
              Format.DIRECTORY).toPath()

          /************************** Write to file START *************************************/
          Utils.writeStatusToFile(writer, inputRoot.toString(),
              directoryInput.changedFiles.entrySet().toString(),
              outputRoot.toString(), invocation.isIncremental())
          /************************** Write to file END *************************************/

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
                  Processor.directRun(inputPath, outputPath, wovenClassesContainer,
                      debounceExt.exclusion)
                  break
                case Status.REMOVED:
                  Files.deleteIfExists(outputPath)
                  break
              }
            }
          } else {
            executor.execute {
              Processor.run(inputRoot, outputRoot, wovenClassesContainer,
                  debounceExt.exclusion,
                  Processor.Input.FILE)
            }
          }
        }
      }
    } finally {
      executor.waitingForAllTasks()
      PrintWriterUtil.closePrintWriter(changedFiles, writer)
      ColoredLogger.log(
          "Wrote file status to file://${PrintWriterUtil.fileName(changedFiles)}")
    }
  }
}
