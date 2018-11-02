package com.smartdengg.plugin

import com.google.common.io.Files
import com.smartdengg.compile.CompactClassWriter
import com.smartdengg.compile.DebounceModifyClassAdapter
import com.smartdengg.compile.WeavedClass
import groovy.transform.PackageScope
import org.apache.commons.io.IOUtils
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter

import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class Processor {

  @PackageScope static void transformJar(File inputJar, File outputJar,
      List<WeavedClass> weavedClasses) throws IOException {
    Files.createParentDirs(outputJar)

    new ZipOutputStream(new FileOutputStream(outputJar)).withCloseable { outputStream ->

      new ZipInputStream(new FileInputStream(inputJar)).withCloseable { inputStream ->

        ZipEntry entry
        while ((entry = inputStream.nextEntry) != null) {

          if (!entry.isDirectory() && Utils.isMatchCondition(entry.name)) {

            byte[] newContent = visitAndReturnBytecode(entry.name,
                IOUtils.toByteArray(inputStream), weavedClasses)

            outputStream.putNextEntry(new ZipEntry(entry.name))
            outputStream.write(newContent)
            outputStream.closeEntry()
          }
        }
      }
    }
  }

  static void transformFile(File inputFile, File outputFile, List<WeavedClass> weavedClasses)
      throws IOException {

    Files.createParentDirs(outputFile)
    byte[] newContent = visitAndReturnBytecode(inputFile.name, inputFile.bytes, weavedClasses)

    outputFile.withOutputStream {
      it.write(newContent)
    }
  }

  @PackageScope static byte[] visitAndReturnBytecode(String name, byte[] bytes,
      List<WeavedClass> weavedClasses) {

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