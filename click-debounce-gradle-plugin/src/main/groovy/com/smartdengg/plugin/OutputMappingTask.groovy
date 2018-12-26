package com.smartdengg.plugin

import com.smartdengg.compile.WeavedClass
import groovy.xml.MarkupBuilder
import org.apache.commons.io.FileUtils
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

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

  boolean loggable

  @TaskAction
  void wrireMapping() {

    loggable = (project.extensions["$DebounceExtension.NAME"] as DebounceExtension).loggable
    def mappingFile = outputMappingFile.get().asFile

    if (!mappingFile.exists()) {
      createMappingFile(mappingFile)
    } else {
      updateMappingFile(mappingFile)
    }

    println "Success wrote TXT mapping report to file://${outputMappingFile.get()}"
  }

  void createMappingFile(File mappingFile) {

    FileUtils.touch(mappingFile)

    def markupBuilder = new MarkupBuilder(mappingFile.newPrintWriter())

    markupBuilder."debounce-classes"() {

      classes.get()[variantName.get()].findAll { WeavedClass weavedClass ->

        weavedClass.hasDebouncedMethod()
      }.each { touchedWeavedClass ->

        String className = touchedWeavedClass.className
        Set<String> debouncedMethods = touchedWeavedClass.debouncedMethods

        markupBuilder.'class'(name: "$className") {
          for (def methodSignature in debouncedMethods) {
            markupBuilder.'method'("$methodSignature")
          }
        }
      }
    }
  }

  void updateMappingFile(File mappingFile) {

    Node rootElements = new XmlParser().parse(mappingFile)

    classes.get()[variantName.get()].findAll {
      it.hasDebouncedMethod()
    }.each { weavedClass ->
      rootElements.findAll {
        it.'@name' == weavedClass.className
      }.each { Node node ->

        while (node.method.size() != 0) {
          node.method[0].replaceNode {}
        }

        for (int i = 0; i < weavedClass.debouncedMethods.size(); i++) {
          if (loggable) println "ADD ${node.'@name'.replace('/', '.')} : ${weavedClass.debouncedMethods[i]}"
          node.appendNode("method", weavedClass.debouncedMethods[i])
        }
      }
    }

    XmlNodePrinter nodePrinter = new XmlNodePrinter(mappingFile.newPrintWriter())
    nodePrinter.setPreserveWhitespace(true)
    nodePrinter.print(rootElements)
  }
}