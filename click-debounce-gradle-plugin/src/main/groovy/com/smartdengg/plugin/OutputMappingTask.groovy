package com.smartdengg.plugin

import com.smartdengg.compile.WeavedClass
import org.apache.commons.io.FileUtils
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

import javax.inject.Inject

class OutputMappingTask extends DefaultTask {

  {
    group = 'debounce'
    description = 'write debounced mapping file'
  }

  Map<String, List<WeavedClass>> weavedVariantClassesMap
  String variantName

  @OutputFile
  File targetMappingFile

  @Inject
  OutputMappingTask(String variantName, Map<String, List<WeavedClass>> weavedVariantClassesMap) {
    this.variantName = variantName
    this.weavedVariantClassesMap = weavedVariantClassesMap
  }

  @TaskAction
  void wrireMapping() {

    final DebounceExtension debounceExt = project.extensions.getByName(DebounceExtension.NAME)

    FileUtils.touch(targetMappingFile)

    targetMappingFile.withWriter { writer ->

      weavedVariantClassesMap[variantName].findAll { weavedClass ->

        weavedClass.hasDebouncedMethod()
      }.each { touchedWeavedClass ->

        String className = touchedWeavedClass.className
        Set<String> debouncedMethods = touchedWeavedClass.debouncedMethods
        writer.writeLine "$className"

        if (debounceExt.loggable) println className

        for (def methodSignature in debouncedMethods) {
          writer.writeLine "    \u21E2  $methodSignature"
          if (debounceExt.loggable) println "    \u21E2  $methodSignature"
        }
      }
    }
    println "Success wrote TXT mapping report to file://${targetMappingFile}"
  }
}