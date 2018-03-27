package com.smartdengg.plugin

import com.smartdengg.compile.DebounceModifyClassAdapter
import com.smartdengg.compile.CompactClassWriter
import org.apache.commons.io.IOUtils
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter

import java.util.jar.JarEntry
import java.util.jar.JarFile
import java.util.jar.JarOutputStream
import java.util.zip.ZipEntry

class Utils {

  static void modifyFile(File file) {

    String name = file.name
    if (name.endsWith(".class") && !name.startsWith("R\$") &&
        !"R.class".equals(name) &&
        !"BuildConfig.class".equals(name)) {

      System.out.println(name + "is changing...")

      byte[] code = com.smartdengg.plugin.Utils.visitAndReturnCode(file.getBytes())

      FileOutputStream fos = new FileOutputStream(
          file.parentFile.absolutePath + File.separator + name)
      fos.write(code)
      fos.close()
    }
  }

  static File modifyJar(File jarFile, File tempDir, String hexedName) {

    def file = new JarFile(jarFile)
    def outputJar = new File(tempDir, hexedName + "_" + jarFile.name)

    println "outputJar name = " + outputJar.getName()

    JarOutputStream jarOutputStream = new JarOutputStream(new FileOutputStream(outputJar))
    Enumeration enumeration = file.entries()

    while (enumeration.hasMoreElements()) {

      JarEntry jarEntry = (JarEntry) enumeration.nextElement()
      InputStream inputStream = file.getInputStream(jarEntry)

      String entryName = jarEntry.getName()

      println path2Classname(entryName) + " is changing ......"

      ZipEntry zipEntry = new ZipEntry(entryName)

      jarOutputStream.putNextEntry(zipEntry)

      byte[] modifiedClassBytes = null
      byte[] sourceClassBytes = IOUtils.toByteArray(inputStream)

      if (entryName.endsWith(".class")) {
        modifiedClassBytes = visitAndReturnCode(sourceClassBytes)
      }

      if (modifiedClassBytes == null) {
        jarOutputStream.write(sourceClassBytes)
      } else {
        jarOutputStream.write(modifiedClassBytes)
      }

      jarOutputStream.closeEntry()
    }
    jarOutputStream.close()
    file.close()


    return outputJar
  }

  static String path2Classname(String entryName) {
    entryName.replace(File.separator, ".").replace(".class", "")
  }

  static byte[] visitAndReturnCode(byte[] bytes) {

    def weavedBytes = bytes

    ClassReader classReader = new ClassReader(bytes)
    ClassWriter classWriter =
        new CompactClassWriter(classReader,
            ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS)

    DebounceModifyClassAdapter classAdapter = new DebounceModifyClassAdapter(classWriter)
    try {
      classReader.accept(classAdapter, ClassReader.EXPAND_FRAMES)
      weavedBytes = classWriter.toByteArray()
    } catch (Exception e) {
      println "Exception occured when visit code \n " + e.printStackTrace()
    }

    return weavedBytes
  }
}