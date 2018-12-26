package com.smartdengg.plugin

import com.android.SdkConstants
import com.android.build.gradle.AppExtension
import com.android.build.gradle.BaseExtension
import com.android.build.gradle.FeatureExtension
import com.android.build.gradle.LibraryExtension
import com.android.utils.FileUtils

import java.nio.file.Path

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

  static void forExtension(BaseExtension extension, Closure closure) {

    switch (extension) {
      case AppExtension:
        extension.applicationVariants.all(closure)
        break
      case LibraryExtension:
        extension.libraryVariants.all(closure)
        break
      case FeatureExtension:
        extension.featureVariants.all(closure)
        break
    }
  }
}