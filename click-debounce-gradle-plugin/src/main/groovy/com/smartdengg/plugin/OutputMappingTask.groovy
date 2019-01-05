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
  void wrireMapping() {

    boolean loggable = (project.extensions["$DebounceExtension.NAME"] as DebounceExtension).loggable
    def mappingFile = outputMappingFile.get().asFile
    List<WeavedClass> weavedClasses = (List<WeavedClass>) classes.get()[variantName.get()]

    FileUtils.touch(mappingFile)
    Files.asCharSink(mappingFile, Charsets.UTF_8).write("")
    PrintWriter writer = PrintWriterUtil.createPrintWriterOut(mappingFile)

    try {
      weavedClasses.findAll {
        it.hasDebouncedMethod()
      }.each { weavedClass ->
        writer.println "${weavedClass.className}:"
        weavedClass.debouncedMethods.each { method ->
          writer.println "\t -> $method"
        }
      }
    } finally {
      PrintWriterUtil.closePrintWriter(mappingFile, writer)
      println "Success wrote TXT mapping report to [" + PrintWriterUtil.fileName(mappingFile) + "]"
    }
  }
}