package com.smartdengg.plugin

import com.smartdengg.compile.WeavedClass
import org.apache.commons.io.FileUtils
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

import javax.inject.Inject

class OutputMappingTask extends DefaultTask {

  {
    group = 'debounce'
    description = 'write debounced mapping file'
  }

  @Input
  Map<String, List<WeavedClass>> weavedProjectClassesMap

  @OutputFile
  File targetMappingFile

  @Inject
  OutputMappingTask(Map<String, List<WeavedClass>> weavedProjectClassesMap) {
    this.weavedProjectClassesMap = weavedProjectClassesMap
  }

  @TaskAction
  void wrireMapping() {

    project.layout.files()

    FileUtils.touch(targetMappingFile)

    targetMappingFile.withWriter { writer ->

      weavedProjectClassesMap[project.name].findAll { weavedClass ->

        weavedClass.hasDebouncedMethod()
      }.each { touchedWeavedClass ->

        String className = touchedWeavedClass.className
        Set<String> debouncedMethods = touchedWeavedClass.debouncedMethods
        writer.writeLine "$className"

        for (def methodSignature in debouncedMethods) {
          writer.writeLine "    \u21E2  $methodSignature"
        }
      }
    }
    println "Success wrote TXT mapping report to file://${targetMappingFile}"
  }
}