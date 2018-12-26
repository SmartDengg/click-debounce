package com.smartdengg.plugin

import com.google.common.collect.ImmutableMap
import com.google.common.collect.Iterables
import com.smartdengg.compile.CompactClassWriter
import com.smartdengg.compile.DebounceModifyClassAdapter
import com.smartdengg.compile.WeavedClass
import groovy.transform.PackageScope
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter

import java.nio.file.*
import java.nio.file.attribute.BasicFileAttributes

class Processor {

  @PackageScope static void processJarPath(Path inputPath, Path outputPath,
      List<WeavedClass> weavedClasses) throws IOException {

    Map<String, String> env = ImmutableMap.of('create', 'true')
    URI inputUri = URI.create("jar:file:$inputPath")
    URI outputUri = URI.create("jar:file:$outputPath")

    FileSystems.newFileSystem(inputUri, env).withCloseable { inputZFS ->
      FileSystems.newFileSystem(outputUri, env).withCloseable { outputZFS ->
        Path inputRoot = Iterables.getOnlyElement(inputZFS.rootDirectories)
        Path outputRoot = Iterables.getOnlyElement(outputZFS.rootDirectories)
        processFilePath(inputRoot, outputRoot, weavedClasses)
      }
    }
  }

  @PackageScope static void processFilePath(Path inputRoot, Path outputRoot,
      List<WeavedClass> weavedClasses) {

    Files.walkFileTree(inputRoot, new SimpleFileVisitor<Path>() {
      @Override
      FileVisitResult visitFile(Path inputPath, BasicFileAttributes attrs) throws IOException {
        Path outputPath = Utils.toOutputPath(outputRoot, inputRoot, inputPath)
        processBytecode(inputPath, outputPath, weavedClasses)
        return FileVisitResult.CONTINUE
      }

      @Override
      FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
        Path outputPath = Utils.toOutputPath(outputRoot, inputRoot, dir)
        Files.createDirectories(outputPath)
        return FileVisitResult.CONTINUE
      }
    })
  }

  static void processBytecode(Path inputPath, Path outputPath, List<WeavedClass> weavedClasses) {
    if (Utils.isMatchCondition(inputPath.toString())) {
      byte[] inputBytes = Files.readAllBytes(inputPath)
      byte[] outputBytes = visitAndReturnBytecode(inputBytes, weavedClasses)
      Files.write(outputPath, outputBytes)
    } else {
      Files.copy(inputPath, outputPath)
    }
  }

  static byte[] visitAndReturnBytecode(byte[] bytes, List<WeavedClass> weavedClasses) {

    def weavedBytes = bytes

    ClassReader classReader = new ClassReader(bytes)
    ClassWriter classWriter =
        new CompactClassWriter(classReader,
            ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS)
    DebounceModifyClassAdapter classAdapter = new DebounceModifyClassAdapter(classWriter)
    try {
      classReader.accept(classAdapter, ClassReader.EXPAND_FRAMES)
      weavedBytes = classWriter.toByteArray()
      weavedClasses.add(classAdapter.getWeavedClass())
    } catch (Exception e) {
      println "Exception occurred when visit code \n " + e.printStackTrace()
    }

    return weavedBytes
  }
}