package com.smartdengg.plugin

import com.google.common.collect.ImmutableMap
import com.google.common.collect.Iterables
import com.smartdengg.compile.*
import com.smartdengg.plugin.Utils
import groovy.transform.PackageScope
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter

import java.nio.file.*
import java.nio.file.attribute.BasicFileAttributes

class Processor {

  enum FileType {
    JAR,
    FILE
  }

  @PackageScope static void run(Path input, Path output, List<WeavedClass> weavedClasses,
      Map<String, List<String>> exclusion,
      FileType fileType) throws IOException {

    switch (fileType) {

      case FileType.JAR:
        processJar(input, output, weavedClasses, exclusion)
        break

      case FileType.FILE:
        processFile(input, output, weavedClasses, exclusion)
        break
    }
  }

  private static void processJar(Path input, Path output, List<WeavedClass> weavedClasses,
      Map<String, List<String>> exclusion) {

    Map<String, String> env = ImmutableMap.of('create', 'true')
    URI inputUri = URI.create("jar:file:$input")
    URI outputUri = URI.create("jar:file:$output")

    FileSystems.newFileSystem(inputUri, env).withCloseable { inputFileSystem ->
      FileSystems.newFileSystem(outputUri, env).withCloseable { outputFileSystem ->
        Path inputRoot = Iterables.getOnlyElement(inputFileSystem.rootDirectories)
        Path outputRoot = Iterables.getOnlyElement(outputFileSystem.rootDirectories)
        processFile(inputRoot, outputRoot, weavedClasses, exclusion)
      }
    }
  }

  private static void processFile(Path input, Path output, List<WeavedClass> weavedClasses,
      Map<String, List<String>> exclusion) {

    Files.walkFileTree(input, new SimpleFileVisitor<Path>() {
      @Override
      FileVisitResult visitFile(Path inputPath, BasicFileAttributes attrs) throws IOException {
        Path outputPath = Utils.toOutputPath(output, input, inputPath)
        directRun(inputPath, outputPath, weavedClasses, exclusion)
        return FileVisitResult.CONTINUE
      }

      @Override
      FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
        Path outputPath = Utils.toOutputPath(output, input, dir)
        Files.createDirectories(outputPath)
        return FileVisitResult.CONTINUE
      }
    })
  }

  @PackageScope static void directRun(Path input, Path output,
      List<WeavedClass> weavedClasses, Map<String, List<String>> exclusion) {
    if (Utils.isMatchCondition(input.toString())) {
      byte[] inputBytes = Files.readAllBytes(input)
      byte[] outputBytes = visitAndReturnBytecode(inputBytes, weavedClasses, exclusion)
      Files.write(output, outputBytes)
    } else {
      Files.copy(input, output)
    }
  }

  private static byte[] visitAndReturnBytecode(byte[] originBytes,
      List<WeavedClass> weavedClasses, Map<String, List<String>> exclusion) {

    ClassReader classReader = new ClassReader(originBytes)
    ClassWriter classWriter =
        new CompactClassWriter(classReader,
            ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS)

    Map<String, List<MethodDelegate>> map = preCheckAndRetrieve(originBytes, exclusion)
    DebounceModifyClassAdapter classAdapter = new DebounceModifyClassAdapter(classWriter, map)
    try {
      classReader.accept(classAdapter, ClassReader.EXPAND_FRAMES)
      //move to visit end?
      weavedClasses.add(classAdapter.getWovenClass())
      return classWriter.toByteArray()
    } catch (Exception e) {
      println "Exception occurred when visit code \n " + e.printStackTrace()
    }

    return originBytes
  }

  private static Map<String, List<MethodDelegate>> preCheckAndRetrieve(byte[] bytes,
      Map<String, List<String>> exclusion) {

    ClassReader classReader = new ClassReader(bytes)
    PreCheckVisitorAdapter preCheckVisitorAdapter = new PreCheckVisitorAdapter(exclusion)
    try {
      classReader.accept(preCheckVisitorAdapter, ClassReader.SKIP_FRAMES)
    } catch (Exception e) {
      println "Exception occurred when visit code \n " + e.printStackTrace()
    }

    return preCheckVisitorAdapter.getUnWeavedClassMap()
  }
}