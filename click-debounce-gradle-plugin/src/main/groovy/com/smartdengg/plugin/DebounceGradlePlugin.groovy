package com.smartdengg.plugin

import com.android.build.gradle.*
import com.android.build.gradle.api.BaseVariant
import com.android.build.gradle.internal.pipeline.TransformTask
import com.android.builder.model.AndroidProject
import com.android.utils.FileUtils
import com.smartdengg.compile.WovenClass
import com.smartdengg.plugin.api.DebounceExtension
import com.smartdengg.plugin.internal.Utils
import groovy.util.logging.Slf4j
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.TaskProvider
import org.gradle.internal.reflect.Instantiator

import javax.inject.Inject

@Slf4j
class DebounceGradlePlugin implements Plugin<Project> {

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
        project.dependencies.create('com.github.SmartDengg:asm-clickdebounce-runtime:1.1.1'))

    project.extensions["${DebounceExtension.NAME}"] = instantiator.newInstance(DebounceExtension)
    def extension = project.extensions.getByName("android") as BaseExtension
    def variantWeavedClassesMap = new LinkedHashMap<String, List<WovenClass>>()

    extension.registerTransform(new DebounceIncrementalTransform(project, variantWeavedClassesMap))

    project.afterEvaluate {
      Utils.forEachVariant(extension) { BaseVariant variant ->
        createWriteMappingTask(project, variant, variantWeavedClassesMap)
      }
    }
  }

  static void createWriteMappingTask(Project project, BaseVariant variant,
      Map<String, List<WovenClass>> variantWovenClassesMap) {

    def mappingTaskName = "outputMappingFor${variant.name.capitalize()}"
    def transformTaskName = "transformClassesWith${DebounceIncrementalTransform.TASK_NAME.capitalize()}For${variant.name.capitalize()}"

    TaskProvider<TransformTask> debounceTask = project.tasks.named(transformTaskName)
    TaskProvider<OutputMappingTask> mappingTask = project.tasks.
        register("${mappingTaskName}", OutputMappingTask) { OutputMappingTask task ->
          task.classes = variantWovenClassesMap
          task.variantName = variant.name
          task.mappingFile =
              FileUtils.join(project.buildDir, AndroidProject.FD_OUTPUTS, 'debounce', 'logs',
                  variant.name, 'classes.txt')
          task.onlyIf { debounceTask.get().didWork }
          task.inputFiles = debounceTask.get().outputs.files
        }

    mappingTask.configure(Utils.taskTimedConfigure)
    debounceTask.configure(Utils.taskTimedConfigure)
    debounceTask.configure { Task task ->
      task.finalizedBy(mappingTask)
    }
  }
}


