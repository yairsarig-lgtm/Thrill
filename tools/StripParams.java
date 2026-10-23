import org.objectweb.asm.*;
import java.nio.file.*;

/** Removes MethodParameters attributes (newer javac emits them; the old d8 chokes on them). */
public class StripParams {
    public static void main(String[] args) throws Exception {
        for (String a : args) {
            Path p = Paths.get(a);
            ClassReader r = new ClassReader(Files.readAllBytes(p));
            ClassWriter w = new ClassWriter(0);
            r.accept(new ClassVisitor(Opcodes.ASM7, w) {
                @Override
                public MethodVisitor visitMethod(int acc, String n, String d, String s, String[] e) {
                    return new MethodVisitor(Opcodes.ASM7, super.visitMethod(acc, n, d, s, e)) {
                        @Override public void visitParameter(String name, int access) {}
                    };
                }
            }, 0);
            Files.write(p, w.toByteArray());
        }
    }
}
