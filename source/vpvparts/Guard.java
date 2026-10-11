package vpvparts;

import java.lang.reflect.Field;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

/** Fixed adaptive frustum guard. No settings, counters, disk access or network access. */
public final class Guard {
    // Only preserve a rescue from this thread's immediately preceding shown call.
    private static final ThreadLocal<Rescue> lastRescue=ThreadLocal.withInitial(Rescue::new);
    private static volatile boolean rollingReported;
    private static final class Rescue {
        Object pass,draws; int index=-1;
        void clear() { pass=null; draws=null; index=-1; }
        boolean remember(Object p,Object d,int i) { pass=p; draws=d; index=i; return true; }
    }
    public static boolean preserveRollingRescue(Object pass,Object draws,int index,boolean incoming,boolean result) {
        Rescue rescue=lastRescue.get();
        boolean restore=!disabled && !result && incoming && rescue.pass==pass && rescue.draws==draws && rescue.index==index;
        rescue.clear();
        if (restore && !rollingReported) reportRolling();
        return result || restore;
    }
    private static synchronized void reportRolling() {
        if (!rollingReported) System.out.println("[VPVParts] Rolling Hills compatibility active; vehicle-part rescue preserved");
        rollingReported=true;
    }
    private static final float PADDING=4f;
    private static final float NEAR_RANGE=12f;
    private static volatile boolean disabled;
    private static volatile boolean ready;
    private static volatile boolean started;
    private static Field keys, values, flags, parent, owner, frustum;
    private static int valueStride, sphereOffset;
    private static MethodHandle sphere;
    private static Class<?> submodel, vehicle;

    public static synchronized void start() {
        if (started) return;
        System.out.println("[VPVParts] 1.3 loaded; adaptive guard with Rolling Hills compatibility");
        started=true;
    }

    private static Field field(Class<?> type,String name) throws ReflectiveOperationException {
        Field f=type.getDeclaredField(name); f.setAccessible(true); return f;
    }

    private static synchronized void init(Object pass,Object draws) throws ReflectiveOperationException {
        if (ready) return;
        ClassLoader loader=draws.getClass().getClassLoader();
        if (!draws.getClass().getName().equals("viewpoint.render.ModelDraws") ||
            !pass.getClass().getName().equals("viewpoint.render.ModelPass"))
            throw new IllegalStateException("unexpected target");
        Class<?> dc=draws.getClass();
        valueStride=field(dc,"VALUES").getInt(null);
        sphereOffset=field(dc,"SPHERE").getInt(null);
        if ((valueStride!=60 && valueStride!=68) || sphereOffset!=24 ||
            field(dc,"COLOUR").getInt(null)!=1) throw new IllegalStateException("unsupported ModelDraws layout");
        keys=field(dc,"keys"); values=field(dc,"values"); flags=field(dc,"flags");
        Class<?> mi=Class.forName("zombie.core.skinnedmodel.model.ModelInstance",false,loader);
        parent=mi.getField("parent"); owner=mi.getField("object");
        submodel=Class.forName("zombie.core.skinnedmodel.model.VehicleSubModelInstance",false,loader);
        vehicle=Class.forName("zombie.vehicles.BaseVehicle",false,loader);
        frustum=field(pass.getClass(),"frustum");
        sphere=MethodHandles.publicLookup().unreflect(frustum.getType().getMethod(
            "testSphere",float.class,float.class,float.class,float.class)).asType(
            MethodType.methodType(boolean.class,Object.class,float.class,float.class,float.class,float.class));
        ready=true;
        System.out.println("[VPVParts] shown hook reached; layout verified; vehicle submodels only");
    }

    public static boolean shown(Object pass,Object draws,int index,boolean original) {
        Rescue rescue=lastRescue.get();
        rescue.clear();
        if (original || disabled) return original;
        if (!started) start();
        try {
            if (!ready) init(pass,draws);
            Object[] list=(Object[])keys.get(draws);
            if (index<0 || index>=list.length) return false;
            Object part=list[index];
            // Both type and current owner matter because the game pools model instances.
            if (!submodel.isInstance(part) || parent.get(part)==null || !vehicle.isInstance(owner.get(part)))
                return false;
            int[] fs=(int[])flags.get(draws);
            if ((fs[index]&1)==0) return false;
            float[] v=(float[])values.get(draws);
            int b=Math.addExact(Math.multiplyExact(index,valueStride),sphereOffset);
            float x=v[b],y=v[b+1],z=v[b+2],r=v[b+3];
            if (!Float.isFinite(x) || !Float.isFinite(y) || !Float.isFinite(z) || !Float.isFinite(r) || r<0)
                return false;
            if (near(x,y,z,r,NEAR_RANGE)) return rescue.remember(pass,draws,index);
            boolean visible=(boolean)sphere.invokeExact((Object)frustum.get(pass),x,y,z,r+PADDING);
            return visible && rescue.remember(pass,draws,index);
        } catch (Throwable e) {
            disable(e);
            return original;
        }
    }

    public static boolean near(float x,float y,float z,float radius,float range) {
        // VehicleCapture's bounds are relative to Frame.camX/Y/Z in the audited Viewpoint build.
        double reach=(double)range+radius;
        return (double)x*x+(double)y*y+(double)z*z<=reach*reach;
    }

    private static synchronized void disable(Throwable e) {
        if (!disabled) System.out.println("[VPVParts] DISABLED; original behaviour retained: "+e);
        disabled=true;
    }
}
