package com.smartdengg.compile;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import static com.smartdengg.compile.Utils.addDebouncedAnno;
import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.IFNE;
import static org.objectweb.asm.Opcodes.INVOKESTATIC;
import static org.objectweb.asm.Opcodes.RETURN;

/**
 * 创建时间:  2018/03/09 19:48 <br>
 * 作者:  SmartDengg <br>
 * 描述:
 */
class ListView$OnItemClickListenerMethodAdapter extends MethodVisitor {

  ListView$OnItemClickListenerMethodAdapter(MethodVisitor methodVisitor) {
    super(Opcodes.ASM6, methodVisitor);
  }

  @Override public void visitCode() {
    super.visitCode();

    addDebouncedAnno(mv);

    mv.visitVarInsn(ALOAD, 2);
    mv.visitMethodInsn(INVOKESTATIC, "com/smartdengg/clickdebounce/DebouncedPredictor",
        "shouldDoClick", "(Landroid/view/View;)Z", false);
    Label label = new Label();
    mv.visitJumpInsn(IFNE, label);
    mv.visitInsn(RETURN);
    mv.visitLabel(label);
  }
}
