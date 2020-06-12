package com.smartdengg.plugin

import com.google.common.base.Charsets
import com.google.common.io.Files
import com.smartdengg.compile.WovenClass
import com.smartdengg.plugin.api.DebounceExtension
import com.smartdengg.plugin.internal.ColoredLogger
import com.smartdengg.plugin.internal.Utils
import org.apache.commons.io.FileUtils
import org.gradle.api.DefaultTask
import org.gradle.api.file.FileType
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.work.FileChange
import org.gradle.work.InputChanges
import proguard.util.PrintWriterUtil

class OutputMappingTask extends DefaultTask {

  {
    group = 'debounce'
    description = 'write debounced mapping file'
  }

  @Input
  Property<String> variantName = project.objects.property(String.class)

  @OutputFile
  RegularFileProperty outputMappingFile = project.objects.fileProperty()

  @Internal
  MapProperty<String, List<WovenClass>> classes =
          project.objects.mapProperty(String.class, List.class)

  @TaskAction
  void writeMapping(InputChanges inputChanges) {

    boolean loggable = (project.extensions["$DebounceExtension.NAME"] as DebounceExtension).loggable
    def mappingFile = outputMappingFile.get().asFile
    List<WovenClass> wovenClasses = (List<WovenClass>) classes.get()[variantName.get()]

    if (inputChanges.isIncremental()) {
      inputChanges.getFileChanges(inputs.files).each { FileChange change ->
        if (change.fileType == FileType.DIRECTORY) return
        if (loggable && Utils.isMatchCondition(change.file.name)) {
          ColoredLogger.logGreen("[${change.changeType.name()}]: ${change.file.name}")
        }
      }
    }

    //    inputs.outOfDate { change ->
    //      if (change.file.directory) return
    //      if (loggable && Utils.isMatchCondition(change.file.name)) {
    //        String state = change.added ? 'ADDED' : 'MODIFIED'
    //        ColoredLogger.logGreen("[OUT OF DATE]: ${change.file.name}:$state")
    //      }
    //    }
    //
    //    inputs.removed { change ->
    //      if (change.file.directory) return
    //      if (loggable && Utils.isMatchCondition(change.file.name)) {
    //        ColoredLogger.logGreen("[REMOVED]: ${change.file.name}")
    //      }
    //    }

    FileUtils.touch(mappingFile)
    Files.asCharSink(mappingFile, Charsets.UTF_8).write("")
    PrintWriter writer = PrintWriterUtil.createPrintWriterOut(mappingFile)

    try {
      wovenClasses.findAll {
        it.hasDebouncedMethod()
      }.each { wovenClass ->
        writer.println "${wovenClass.className}:"
        wovenClass.debouncedMethods.each { method ->
          if (loggable) ColoredLogger.logGreen("[ADD]: $wovenClass.className:$method")
          writer.println "\t -> $method"
        }
      }
    } finally {
      PrintWriterUtil.closePrintWriter(mappingFile, writer)
      ColoredLogger.log(
              "Wrote TXT report to file://${PrintWriterUtil.fileName(mappingFile)}")
    }
  }
}