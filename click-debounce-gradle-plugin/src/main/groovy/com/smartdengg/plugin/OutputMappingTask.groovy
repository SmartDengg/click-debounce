package com.smartdengg.plugin

import com.google.common.base.Charsets
import com.google.common.io.Files
import com.smartdengg.compile.WeavedClass
import org.apache.commons.io.FileUtils
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.incremental.IncrementalTaskInputs
import proguard.util.PrintWriterUtil

class OutputMappingTask extends DefaultTask {

  {
    group = 'debounce'
    description = 'write debounced mapping file'
  }

  @Input
  Property<String> variantName = project.objects.property(String.class)

  @OutputFile
  RegularFileProperty outputMappingFile = newOutputFile()

  @Internal
  Property<Map> classes = project.objects.property(Map.class)

  @TaskAction
  void wrireMapping(IncrementalTaskInputs inputs) {

    boolean loggable = (project.extensions["$DebounceExtension.NAME"] as DebounceExtension).loggable
    def mappingFile = outputMappingFile.get().asFile
    List<WeavedClass> weavedClasses = (List<WeavedClass>) classes.get()[variantName.get()]

    inputs.outOfDate { change ->
      if (change.file.directory) return
      if (loggable && Utils.isMatchCondition(change.file.name)) {
        String state
        if (change.added) {
          state = 'ADDED'
        } else if (change.modified) {
          state = 'MODIFIED'
        } else {
          state = 'FIRST RUN'
        }
        ColoredLogger.logBlue("OUT OF DATE: ${change.file.name}:$state")
      }
    }

    inputs.removed { change ->
      if (change.file.directory) return
      if (loggable && Utils.isMatchCondition(change.file.name)) {
        ColoredLogger.logBlue("REMOVED: ${change.file.name}")
      }
    }

    FileUtils.touch(mappingFile)
    Files.asCharSink(mappingFile, Charsets.UTF_8).write("")
    PrintWriter writer = PrintWriterUtil.createPrintWriterOut(mappingFile)

    try {
      weavedClasses.findAll {
        it.hasDebouncedMethod()
      }.each { weavedClass ->
        writer.println "${weavedClass.className}:"
        weavedClass.debouncedMethods.each { method ->
          if (loggable) ColoredLogger.logBlue("ADD: $weavedClass.className : $method")
          writer.println "\t -> $method"
        }
      }
    } finally {
      PrintWriterUtil.closePrintWriter(mappingFile, writer)
      ColoredLogger.logGreen(
          "SUCCESSFUL: Writing txt mapping report to [" + PrintWriterUtil.fileName(mappingFile) +
              "]")
    }
  }
}