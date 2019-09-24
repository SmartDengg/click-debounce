package com.smartdengg.compile;

import java.util.List;
import java.util.Map;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import static com.smartdengg.compile.Utils.convertSignature;

/**
 * 创建时间: 2018/03/21 23:00 <br>
 * 作者: dengwei <br>
 * 描述:
 */
public class DebounceModifyClassAdapter extends ClassVisitor implements Opcodes {

  /*For debug*/
  private String className;
  private WovenClass wovenClass;
  private Map<String, List<MethodDescriptor>> unWovenClassMap;

  public DebounceModifyClassAdapter(ClassVisitor classVisitor,
      Map<String, List<MethodDescriptor>> unWovenClassMap) {
    super(Opcodes.ASM6, classVisitor);
    this.unWovenClassMap = unWovenClassMap;
  }

  @Override
  public void visit(int version, int access, String name, String signature, String superName,
      String[] interfaces) {
    super.visit(version, access, name, signature, superName, interfaces);
    this.wovenClass = new WovenClass(className = name);
  }

  @Override
  public MethodVisitor visitMethod(int access, final String name, String desc, String signature,
      String[] exceptions) {

    MethodVisitor methodVisitor = cv.visitMethod(access, name, desc, signature, exceptions);

    // android.view.View.OnClickListener.onClick(android.view.View)
    if (Utils.isViewOnclickMethod(access, name, desc) && isHit(access, name, desc)) {
      methodVisitor = new View$OnClickListenerMethodAdapter(methodVisitor);
      wovenClass.addDebouncedMethod(convertSignature(name, desc));
    }

    // android.widget.AdapterView.OnItemClickListener.onItemClick(android.widget.AdapterView,android.view.View,int,long)
    if (Utils.isListViewOnItemOnclickMethod(access, name, desc) && isHit(access, name, desc)) {
      methodVisitor = new ListView$OnItemClickListenerMethodAdapter(methodVisitor);
      wovenClass.addDebouncedMethod(convertSignature(name, desc));
    }

    return methodVisitor;
  }

  private boolean isHit(int access, String name, String desc) {
    if (unWovenClassMap == null || unWovenClassMap.size() == 0) return false;
    List<MethodDescriptor> methodDescriptors = unWovenClassMap.get(wovenClass.className);
    if (methodDescriptors != null) {
      for (MethodDescriptor delegateMethod : methodDescriptors) {
        if (delegateMethod.match(access, name, desc)) return true;
      }
    }

    return false;
  }

  public WovenClass getWovenClass() {
    return wovenClass;
  }
}
