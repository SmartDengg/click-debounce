package com.smartdengg.compile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * 创建时间:  2019/01/04 15:43 <br>
 * 作者:  SmartDengg <br>
 * 描述:
 */
public class PreCheckVisitorAdapter extends ClassVisitor implements Opcodes {

  private String className;
  private Map<String, List<MethodDelegate>> unWeavedClassMap = new HashMap<>();

  public PreCheckVisitorAdapter() {
    super(Opcodes.ASM6);
  }

  @Override
  public void visit(int version, int access, String name, String signature, String superName,
      String[] interfaces) {
    super.visit(version, access, name, signature, superName, interfaces);
    this.className = name;
  }

  @Override public MethodVisitor visitMethod(int access, String name, String desc, String signature,
      String[] exceptions) {

    if (Utils.isViewOnclickMethod(access, name, desc) || Utils.isListViewOnItemOnclickMethod(access,
        name, desc)) {
      return new MethodNodeAdapter(api, access, name, desc, signature, exceptions, className,
          unWeavedClassMap);
    }

    return super.visitMethod(access, name, desc, signature, exceptions);
  }

  public Map<String, List<MethodDelegate>> getUnWeavedClassMap() {
    return unWeavedClassMap;
  }

  static class MethodNodeAdapter extends MethodNode {

    private String className;
    private Map<String, List<MethodDelegate>> map;

    MethodNodeAdapter(int api, int access, String name, String desc, String signature,
        String[] exceptions, String className, Map<String, List<MethodDelegate>> map) {
      super(api, access, name, desc, signature, exceptions);
      this.className = className;
      this.map = map;
    }

    @Override public void visitEnd() {
      if (hasInvokeOperation()) {
        List<MethodDelegate> methodDelegates = map.get(className);
        if (methodDelegates == null) methodDelegates = new ArrayList<>();
        methodDelegates.add(new MethodDelegate(access, name, desc));
        map.put(className, methodDelegates);
      }
    }

    private boolean hasInvokeOperation() {
      for (ListIterator<AbstractInsnNode> iterator = instructions.iterator();
          iterator.hasNext(); ) {
        AbstractInsnNode node = iterator.next();
        int opcode = node.getOpcode();
        if (opcode == -1) continue;
        if (opcode >= INVOKEVIRTUAL && opcode <= INVOKEDYNAMIC) {
          return true;
        }
      }
      return false;
    }
  }
}
