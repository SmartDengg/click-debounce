package com.smartdengg.compile;

import java.util.Arrays;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * 创建时间: 2018/03/21 23:00 <br>
 * 作者: dengwei <br>
 * 描述:
 */
public class DebounceModifyClassAdapter extends ClassVisitor implements Opcodes {

  public DebounceModifyClassAdapter(ClassVisitor classVisitor) {
    super(Opcodes.ASM5, classVisitor);
  }

  @Override
  public MethodVisitor visitMethod(int access, final String name, String desc, String signature,
      String[] exceptions) {

    System.out.println("==========visitMethod start===========");
    System.out.println("name = " + name);
    System.out.println("desc = " + desc);
    System.out.println("signature = " + signature);
    System.out.println("exceptions = " + Arrays.toString(exceptions));
    System.out.println("==========visitMethod end===========");
    System.out.printf("\n");

    MethodVisitor methodVisitor = cv.visitMethod(access, name, desc, signature, exceptions);

    // android.view.View.OnClickListener.onClick(android.view.View)
    if ((Utils.isPublic(access) && !Utils.isStatic(access)) && //
        name.equals("onClick") && //
        desc.equals("(Landroid/view/View;)V")) {
      methodVisitor = new View$OnClickListenerMethodAdapter(methodVisitor);
    } else

      // android.widget.AdapterView.OnItemClickListener.onItemClick(android.widget.AdapterView,android.view.View,int,int)
      if ((Utils.isPublic(access) && !Utils.isStatic(access)) && //
          name.equals("onItemClick") && //
          desc.equals("(Landroid/widget/AdapterView;Landroid/view/View;IJ)V")) {
        methodVisitor = new ListView$OnItemClickListenerMethodAdapter(methodVisitor);
      } /*else {

        methodVisitor = new MethodVisitor(Opcodes.ASM5, methodVisitor) {
          @Override
          public void visitFieldInsn(int opcode, String owner, String fName, String fDesc) {

            if (owner.contains("com/smartdengg/") && owner.contains("R$")) {

              System.out.println("opcode = ["
                  + opcode
                  + "], owner = ["
                  + owner
                  + "], fName = ["
                  + fName
                  + "], fDesc = ["
                  + fDesc
                  + "]");
              super.visitFieldInsn(opcode,
                  "com/smartdengg/clickdebounce/" + "R$" + owner.substring(owner.indexOf("R$") + 2),
                  fName, fDesc);
            } else {
              super.visitFieldInsn(opcode, owner, fName, fDesc);
            }
          }
        };
      }*/

    return methodVisitor;
  }
}
