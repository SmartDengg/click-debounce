package com.smartdengg.plugin

import com.smartdengg.compile.CompactClassWriter
import com.smartdengg.compile.DebounceModifyClassAdapter
import com.smartdengg.compile.WeavedClass
import org.apache.commons.io.FileUtils
import org.apache.commons.io.IOUtils
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter

import java.util.jar.JarEntry
import java.util.jar.JarFile
import java.util.jar.JarOutputStream
import java.util.zip.ZipEntry

class Utils {

  static Map<String, WeavedClass> weavedClassMap = new LinkedHashMap<>()

  private static final List<String> EXCLUSIVECLASS = Collections.unmodifiableList(
      Arrays.asList("BuildConfig.class",
          "R.class",
          "R\$string",
          "R\$attr.class",
          "R\$anim.class",
          "R\$bool.class",
          "R\$color.class",
          "R\$dimen.class",
          "R\$drawable.class",
          "R\$id.class",
          "R\$integer.class",
          "R\$layout.class",
          "R\$string.class ",
          "R\$style.class",
          "R\$styleable.class",))

  static void modifyFile(File file) {

    String name = file.name
    if (name.endsWith(".class")) {

      byte[] code
      def src = file.getBytes()

      if (EXCLUSIVECLASS.contains(name)) {
        code = src
      } else {
        System.out.println(name + " is changing...")
        code = visitAndReturnCode(src)
      }

      FileOutputStream fos = new FileOutputStream(
          file.parentFile.absolutePath + File.separator + name)
      fos.write(code)
      fos.close()
    }
  }

  static File modifyJar(File jarFile, File tempDir, String hexedName) {

    def file = new JarFile(jarFile)
    def outputJar = new File(tempDir, hexedName + "_" + jarFile.name + ".tmp")

    println "outputJar name = " + outputJar.getName()

    JarOutputStream jarOutputStream = new JarOutputStream(new FileOutputStream(outputJar))
    Enumeration enumeration = file.entries()

    while (enumeration.hasMoreElements()) {

      JarEntry jarEntry = (JarEntry) enumeration.nextElement()
      if (jarEntry.isDirectory()) continue

      String entryName = jarEntry.getName()
      println path2Classname(entryName) + " is changing ......"

      ZipEntry zipEntry = new ZipEntry(entryName)
      jarOutputStream.putNextEntry(zipEntry)

      InputStream inputStream = file.getInputStream(jarEntry)
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

  private static String path2Classname(String entryName) {
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

      WeavedClass weavedClass = classAdapter.getWeavedClass()
      weavedClassMap.remove(weavedClass.className)
      weavedClassMap.put(weavedClass.className, weavedClass)
    } catch (Exception e) {
      println "Exception occured when visit code \n " + e.printStackTrace()
    }

    return weavedBytes
  }

  static void saveMappingFile(File targetFile) {

    FileUtils.touch(targetFile)
    FileUtils.deleteQuietly(targetFile)

    OutputStream fileOutputStream = null

    try {
      fileOutputStream = new FileOutputStream(targetFile)

      for (Map.Entry<String, WeavedClass> entry : weavedClassMap.entrySet()) {

        WeavedClass weavedClass = entry.value
        if (!weavedClass.hasDebouncedMethod()) continue

        String className = weavedClass.className
        Set<String> debouncedMethods = weavedClass.debouncedMethods

        IOUtils.write(className + '\n', fileOutputStream)

        for (Iterator<String> iterator = debouncedMethods.iterator(); iterator.hasNext();) {
          String methodSignature = iterator.next()
          IOUtils.write("    \u21E2 " + methodSignature + "\n", fileOutputStream)
        }
      }
    } finally {
      if (fileOutputStream != null) fileOutputStream.close()
    }
  }
}