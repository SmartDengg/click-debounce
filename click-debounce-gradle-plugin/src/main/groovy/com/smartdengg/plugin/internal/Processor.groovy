package com.smartdengg.plugin.internal

import com.google.common.collect.ImmutableMap
import com.google.common.collect.Iterables
import com.smartdengg.compile.CheckAndCollectClassAdapter
import com.smartdengg.compile.DebounceModifyClassAdapter
import com.smartdengg.compile.MethodDescriptor
import com.smartdengg.compile.WovenClass
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter

import java.nio.file.*
import java.nio.file.attribute.BasicFileAttributes

class Processor {

  enum Input {
    JAR,
    FILE
  }

  static void run(Path input, Path output, List<WovenClass> wovenClasses,
      Map<String, List<String>> exclusion,
      Input type) throws IOException {

    switch (type) {

      case Input.JAR:
        processJar(input, output, wovenClasses, exclusion)
        break

      case Input.FILE:
        processFile(input, output, wovenClasses, exclusion)
        break
    }
  }

  private static void processJar(Path input, Path output, List<WovenClass> wovenClasses,
      Map<String, List<String>> exclusion) {

    Map<String, String> env = ImmutableMap.of('create', 'true')
    URI inputUri = URI.create("jar:file:$input")
    URI outputUri = URI.create("jar:file:$output")

    FileSystems.newFileSystem(inputUri, env).withCloseable { inputFileSystem ->
      FileSystems.newFileSystem(outputUri, env).withCloseable { outputFileSystem ->
        Path inputRoot = Iterables.getOnlyElement(inputFileSystem.rootDirectories)
        Path outputRoot = Iterables.getOnlyElement(outputFileSystem.rootDirectories)
        processFile(inputRoot, outputRoot, wovenClasses, exclusion)
      }
    }
  }

  private static void processFile(Path input, Path output, List<WovenClass> wovenClasses,
      Map<String, List<String>> exclusion) {

    Files.walkFileTree(input, new SimpleFileVisitor<Path>() {
      @Override
      FileVisitResult visitFile(Path inputPath, BasicFileAttributes attrs) throws IOException {
        Path outputPath = Utils.toOutputPath(output, input, inputPath)
        directRun(inputPath, outputPath, wovenClasses, exclusion)
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

  static void directRun(Path input, Path output,
      List<WovenClass> wovenClasses, Map<String, List<String>> exclusion) {
    if (Utils.isMatchCondition(input.toString())) {
      byte[] inputBytes = Files.readAllBytes(input)
      byte[] outputBytes = visitAndReturnBytecode(inputBytes, wovenClasses, exclusion)
      Files.write(output, outputBytes)
    } else {
      Files.copy(input, output)
    }
  }

  private static byte[] visitAndReturnBytecode(byte[] originBytes,
      List<WovenClass> wovenClasses, Map<String, List<String>> exclusion) {

    ClassReader classReader = new ClassReader(originBytes)
    //    ClassWriter classWriter =
    //        new CompactClassWriter(classReader,
    //            ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS)
    ClassWriter classWriter = new ClassWriter(classReader, 0)

    Map<String, List<MethodDescriptor>> map = checkAndCollect(originBytes, exclusion)
    DebounceModifyClassAdapter classAdapter = new DebounceModifyClassAdapter(classWriter, map)
    try {
      classReader.accept(classAdapter, ClassReader.EXPAND_FRAMES)
      //move to visit end?
      wovenClasses.add(classAdapter.getWovenClass())
      return classWriter.toByteArray()
    } catch (Exception e) {
      e.printStackTrace()
    }

    return originBytes
  }

  private static Map<String, List<MethodDescriptor>> checkAndCollect(byte[] bytes,
      Map<String, List<String>> exclusion) {

    CheckAndCollectClassAdapter visitor = new CheckAndCollectClassAdapter(exclusion)
    try {
      new ClassReader(bytes).accept(visitor, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES)
    } catch (Exception e) {
      e.printStackTrace()
    }

    return visitor.getUnWeavedClassMap()
  }
}