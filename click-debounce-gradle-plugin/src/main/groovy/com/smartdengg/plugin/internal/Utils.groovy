package com.smartdengg.plugin.internal

import com.android.SdkConstants
import com.android.build.gradle.AppExtension
import com.android.build.gradle.BaseExtension
import com.android.build.gradle.FeatureExtension
import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.api.BaseVariant
import com.android.utils.FileUtils

import java.nio.file.Path
import java.util.concurrent.TimeUnit

class Utils {

  static File toOutputFile(File outputDir, File inputDir, File inputFile) {
    return new File(outputDir, FileUtils.relativePossiblyNonExistingPath(inputFile, inputDir))
  }

  static Path toOutputPath(Path outputRoot, Path inputRoot, Path inputPath) {
    return outputRoot.resolve(inputRoot.relativize(inputPath))
  }

  static boolean isMatchCondition(String name) {
    return name.endsWith(SdkConstants.DOT_CLASS) && //
        !name.matches('.*/R\\$.*\\.class|.*/R\\.class') && //
        !name.matches('.*/BuildConfig\\.class')
  }

  static void forEachVariant(BaseExtension androidExtension, Closure<BaseVariant> closure) {

    if (androidExtension instanceof AppExtension) {
      androidExtension.applicationVariants.all(closure)
    }
    if (androidExtension instanceof LibraryExtension) {
      androidExtension.libraryVariants.all(closure)
    }
    if (androidExtension instanceof FeatureExtension) {
      androidExtension.featureVariants.all(closure)
    }
  }

  static Closure taskTimedConfigure = {
    def startTime
    doFirst { startTime = System.nanoTime() }
    doLast {
      ColoredLogger.logPurple(
          "====> COST: ${TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startTime)} ms")
    }
  }

  static void writeStatusToFile(Writer writer, input, status, output, boolean incremental) {
    writer.println "INPUT: ${input} "
    writer.println "STATUS: ${status} "
    writer.println "OUTPUT: ${output} "
    writer.println "INCREMENTAL: ${incremental}"
    writer.println()
  }
}