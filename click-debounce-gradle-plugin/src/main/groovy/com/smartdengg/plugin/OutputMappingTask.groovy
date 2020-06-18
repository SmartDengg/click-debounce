package com.smartdengg.plugin

import com.google.common.base.Charsets
import com.google.common.io.Files
import com.smartdengg.compile.WovenClass
import com.smartdengg.plugin.api.DebounceExtension
import com.smartdengg.plugin.internal.ColoredLogger
import com.smartdengg.plugin.internal.Utils
import org.apache.commons.io.FileUtils
import org.gradle.api.DefaultTask
import org.gradle.api.file.FileCollection
import org.gradle.api.file.FileType
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.work.FileChange
import org.gradle.work.Incremental
import org.gradle.work.InputChanges
import proguard.util.PrintWriterUtil

class OutputMappingTask extends DefaultTask {

  {
    group = 'debounce'
    description = 'write debounced mapping file'
  }

  @InputFiles
  @Incremental
  @SkipWhenEmpty
  FileCollection inputFiles

  @Input
  Property<String> variantName = project.objects.property(String.class)

  @OutputFile
  RegularFileProperty mappingFile = project.objects.fileProperty()

  @Internal
  MapProperty<String, List<WovenClass>> classes = project.
      objects.mapProperty(String.class, List.class)

  @TaskAction
  void writeMapping(InputChanges inputChanges) {

    boolean loggable = (project.extensions["$DebounceExtension.NAME"] as DebounceExtension).loggable
    File file = mappingFile.get().asFile
    List<WovenClass> wovenClasses = (List<WovenClass>) classes.get()[variantName.get()]

    if (loggable && inputChanges.isIncremental()) {
      inputChanges.getFileChanges(inputFiles).findAll {
        it.fileType == FileType.FILE && Utils.isMatchCondition(it.file.name)
      }.each { FileChange fileChange ->
        ColoredLogger.logGreen("[${fileChange.changeType.name()}]: ${fileChange.file.name}")
      }
    }

    FileUtils.touch(file)
    Files.asCharSink(file, Charsets.UTF_8).write("")
    PrintWriter writer = PrintWriterUtil.createPrintWriterOut(file)

    try {
      wovenClasses.findAll {
        it.hasDebouncedMethod()
      }.each { wovenClass ->
        writer.println "${wovenClass.className}:"
        wovenClass.debouncedMethods.each { method ->
          if (loggable) ColoredLogger.logBlue("[ADD]: $wovenClass.className:$method")
          writer.println "\t -> $method"
        }
      }
    } finally {
      PrintWriterUtil.closePrintWriter(file, writer)
      ColoredLogger.log(
          "Wrote TXT report to file://${PrintWriterUtil.fileName(file)}")
    }
  }
}