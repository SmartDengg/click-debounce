package com.smartdengg.compile;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * 创建时间:  2018/03/15 17:45 <br>
 * 作者:  SmartDengg <br>
 * 描述:
 */
class Utils implements Opcodes {

  private Utils() {
    throw new AssertionError("no instance");
  }

  static boolean isPrivate(int access) {
    return (access & ACC_PRIVATE) != 0;
  }

  static boolean isPublic(int access) {
    return (access & ACC_PUBLIC) != 0;
  }

  static boolean isStatic(int access) {
    return (access & ACC_STATIC) != 0;
  }

  static void weaveDebouncedAnno(MethodVisitor mv) {
    AnnotationVisitor annotationVisitor =
        mv.visitAnnotation("Lcom/smartdengg/clickdebounce/Debounced;", false);
    annotationVisitor.visitEnd();
  }
}
