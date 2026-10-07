import java.io.InputStream;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.jar.asm.*;
import static net.bytebuddy.matcher.ElementMatchers.named;

/** Verifies ByteBuddy bindings against real targets; does not install an agent or change game files. */
public final class AdviceHarness {
    static String annotation(String d) {
        String base="Lme/zed_0xff/zombie_buddy/Patch$";
        if (!d.startsWith(base)) return d;
        String n=d.substring(base.length(),d.length()-1);
        if (n.equals("OnExit")) n="OnMethodExit";
        return "Lnet/bytebuddy/asm/Advice$"+n+";";
    }
    static Class<?> transform(String name) throws Exception {
        byte[] bytes;
        try(InputStream in=AdviceHarness.class.getClassLoader().getResourceAsStream(name.replace('.','/')+".class")) {
            if(in==null) throw new IllegalStateException(name);
            bytes=in.readAllBytes();
        }
        ClassWriter writer=new ClassWriter(0);
        new ClassReader(bytes).accept(new ClassVisitor(Opcodes.ASM9,writer) {
            @Override public MethodVisitor visitMethod(int a,String n,String d,String s,String[] e) {
                return new MethodVisitor(Opcodes.ASM9,super.visitMethod(a,n,d,s,e)) {
                    @Override public AnnotationVisitor visitAnnotation(String d,boolean v) {
                        return super.visitAnnotation(annotation(d),v);
                    }
                    @Override public AnnotationVisitor visitParameterAnnotation(int p,String d,boolean v) {
                        return super.visitParameterAnnotation(p,annotation(d),v);
                    }
                };
            }
        },0);
        return new ClassLoader(AdviceHarness.class.getClassLoader()) {
            Class<?> define() { byte[] b=writer.toByteArray(); return defineClass(name,b,0,b.length); }
        }.define();
    }
    public static void main(String[] args) throws Exception {
        String[][] targets={
            {"vpvparts.Patches$Shown","viewpoint.render.ModelPass","shown"}

        };
        for(String[] t:targets) {
            Class<?> advice=transform(t[0]);
            byte[] changed=new ByteBuddy().redefine(Class.forName(t[1]))
                .visit(Advice.to(advice).on(named(t[2]))).make().getBytes();
            if(changed.length==0) throw new AssertionError(t[1]);
            System.out.println("PASS: offline advice binding "+t[1]+"."+t[2]);
        }
    }
}

