package com.smartdengg.plugin

import com.android.build.gradle.*
import com.android.build.gradle.api.BaseVariant
import com.android.builder.model.AndroidProject
import com.android.utils.FileUtils
import com.smartdengg.compile.WeavedClass
import groovy.util.logging.Slf4j
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.internal.reflect.Instantiator

import javax.inject.Inject
import java.util.concurrent.TimeUnit

/***
 *
 * ./gradlew clean build -Dorg.gradle.daemon=false -Dorg.gradle.debug=true
 *
 * */

@Slf4j
class DebounceGradlePlugin implements Plugin<Project> {

  def isApp
  def isLibrary
  def isFeature

  private final Instantiator instantiator

  @Inject
  DebounceGradlePlugin(Instantiator instantiator) {
    this.instantiator = instantiator
  }

  @Override void apply(Project project) {

    def androidPlugin = [AppPlugin, LibraryPlugin, FeaturePlugin]
        .collect { project.plugins.findPlugin(it) as BasePlugin }
        .find { it != null }

    log.debug('Found Plugin: {}', androidPlugin)

    if (!androidPlugin) {
      throw new GradleException(
          "'com.android.application' or 'com.android.library' or 'com.android.feature' plugin required.")
    }

    project.repositories.maven {
      url "https://jitpack.io"
    }

    //    project.configurations.implementation.dependencies.add(
    //        project.dependencies.create(project.rootProject.findProject("click-debounce-runtime")))

    project.configurations.implementation.dependencies.add(
        project.dependencies.create('com.github.SmartDengg:asm-clickdebounce-runtime:1.0.0'))

    project.extensions["${DebounceExtension.NAME}"] = project.objects.newInstance(DebounceExtension)

    def extension = project.extensions.getByName("android") as BaseExtension

    Utils.forExtension(extension) { isApp, isLibrary, isFeature ->
      this.isApp = isApp
      this.isLibrary = isLibrary
      this.isFeature = isFeature
    }

    def weavedVariantClassesMap = new LinkedHashMap<String, List<WeavedClass>>()

    extension.registerTransform(
        new DebounceIncrementalTransform(project["${DebounceExtension.NAME}"],
            weavedVariantClassesMap, isApp, isLibrary, isFeature))

    project.afterEvaluate {

      Utils.forExtension(extension) { variant ->

        createWriteMappingTask(project, variant, weavedVariantClassesMap)
      }
    }
  }

  static void createWriteMappingTask(Project project, BaseVariant variant,
      Map<String, List<WeavedClass>> weavedVariantClassesMap) {

    def mappingTaskName = "outputMappingFor${variant.name.capitalize()}"
    Task debounceTask = project.tasks["transformClassesWithDebounceFor${variant.name.capitalize()}"]

    debounceTask.configure {
      def startTime
      doFirst {
        startTime = System.nanoTime()
      }
      doLast {
        println()
        println " --> COST: ${TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime)} ms"
        println()
      }
    }

    Task outputMappingTask = project.tasks.create(//
        name: "${mappingTaskName}",
        type: OutputMappingTask) {

      classes = weavedVariantClassesMap

      variantName = variant.name
      outputMappingFile =
          FileUtils.join(project.buildDir, AndroidProject.FD_OUTPUTS, 'debounce', 'mapping',
              variant.name, 'debouncedMapping.txt')
    }

    debounceTask.finalizedBy(outputMappingTask)

    outputMappingTask.onlyIf { debounceTask.didWork }
    outputMappingTask.dependsOn(debounceTask)
  }
}
