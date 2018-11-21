package com.smartdengg.plugin

import com.android.SdkConstants
import com.android.build.gradle.AppExtension
import com.android.build.gradle.BaseExtension
import com.android.build.gradle.FeatureExtension
import com.android.build.gradle.LibraryExtension
import com.android.utils.FileUtils
import com.google.common.collect.Lists

import java.util.function.Function
import java.util.regex.Pattern
import java.util.stream.Collectors

class Utils {

  static boolean loggable = true

  /*'R\$.class' and 'BuildConfig.class' or some other class*/
  private static final List<String> EXCLUSIVECLASS = Collections.unmodifiableList(
      Arrays.asList("BuildConfig.class",
          "R.class",
          "R\$string.class",
          "R\$attr.class",
          "R\$anim.class",
          "R\$bool.class",
          "R\$color.class",
          "R\$dimen.class",
          "R\$drawable.class",
          "R\$mipmap.class",
          "R\$id.class",
          "R\$integer.class",
          "R\$layout.class",
          "R\$string.class ",
          "R\$style.class",
          "R\$styleable.class",))

  static List<Pattern> computeExcludeList() {
    List<String> excludes = Lists.newArrayListWithExpectedSize(5)

    // these must be regexp to match the zip entries
    excludes.add(".*/R.class\$")
    excludes.add(".*/R\$(.*).class\$")
    excludes.add(".*/Manifest.class\$")
    excludes.add(".*/Manifest\$(.*).class\$")

    excludes.add(".*/BuildConfig.class\$")

    // create Pattern Objects.
    return excludes.stream().map(new Function<String, Pattern>() {
      @Override
      Pattern apply(String regex) {
        return new Pattern(regex, 0)
      }
    }).collect(Collectors.toList())
  }

  static def path2Classname(String entryName) {
    entryName.replace(File.separator, ".").replace(".class", "")
  }

  static File toOutputFile(File outputDir, File inputDir, File inputFile) {
    return new File(outputDir, FileUtils.relativePossiblyNonExistingPath(inputFile, inputDir))
  }

  static boolean isMatchCondition(String name) {
    name.endsWith(SdkConstants.DOT_CLASS) && //
        !("${path2Classname(name).split("\\.").last()}.class".toString() in EXCLUSIVECLASS)
  }

  public static void forExtension(BaseExtension extension, Closure closure) {

    def findExtensionType
    if (closure.maximumNumberOfParameters == 3) findExtensionType = true

    if (extension instanceof AppExtension) {
      if (findExtensionType) {
        closure.call(true, false, false)
      } else {
        extension.applicationVariants.all(closure)
      }
    }
    if (extension instanceof LibraryExtension) {
      if (findExtensionType) {
        closure.call(false, true, false)
      } else {
        extension.libraryVariants.all(closure)
      }
    }
    if (extension instanceof FeatureExtension) {
      if (findExtensionType) {
        closure.call(false, false, true)
      } else {
        extension.featureVariants.all(closure)
      }
    }
  }
}