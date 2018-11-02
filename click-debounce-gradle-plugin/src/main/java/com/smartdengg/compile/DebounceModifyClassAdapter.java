package com.smartdengg.compile;

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

  private WeavedClass weavedClass;

  public DebounceModifyClassAdapter(ClassVisitor classVisitor) {
    super(Opcodes.ASM5, classVisitor);
  }

  @Override
  public void visit(int version, int access, String name, String signature, String superName,
      String[] interfaces) {
    super.visit(version, access, name, signature, superName, interfaces);
    weavedClass = new WeavedClass(name);
  }

  @Override
  public MethodVisitor visitMethod(int access, final String name, String desc, String signature,
      String[] exceptions) {

    MethodVisitor methodVisitor = cv.visitMethod(access, name, desc, signature, exceptions);

    // android.view.View.OnClickListener.onClick(android.view.View)
    if ((Utils.isPublic(access) && !Utils.isStatic(access)) && //
        name.equals("onClick") && //
        desc.equals("(Landroid/view/View;)V")) {
      methodVisitor = new View$OnClickListenerMethodAdapter(methodVisitor);
      weavedClass.addDebouncedMethod(convertSignature(name, desc));
    }

    // android.widget.AdapterView.OnItemClickListener.onItemClick(android.widget.AdapterView,android.view.View,int,int)
    if ((Utils.isPublic(access) && !Utils.isStatic(access)) && //
        name.equals("onItemClick") && //
        desc.equals("(Landroid/widget/AdapterView;Landroid/view/View;IJ)V")) {
      methodVisitor = new ListView$OnItemClickListenerMethodAdapter(methodVisitor);
      weavedClass.addDebouncedMethod(convertSignature(name, desc));
    }

    return methodVisitor;
  }

  public WeavedClass getWeavedClass() {
    return weavedClass;
  }
}
