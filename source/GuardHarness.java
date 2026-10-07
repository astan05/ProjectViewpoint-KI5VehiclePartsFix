import java.lang.reflect.*;
import org.joml.*;
import zombie.core.skinnedmodel.model.*;
import zombie.vehicles.BaseVehicle;
import vpvparts.Guard;

/** Uses actual installed Viewpoint/PZ classes without starting the game or an OpenGL context. */
public final class GuardHarness {
    static final sun.misc.Unsafe U;
    static { try {
        Field f=sun.misc.Unsafe.class.getDeclaredField("theUnsafe"); f.setAccessible(true);
        U=(sun.misc.Unsafe)f.get(null);
    } catch(Exception e) { throw new RuntimeException(e); } }
    static void put(Object o,String n,Object v) throws Exception {
        Field f=o.getClass().getDeclaredField(n); f.setAccessible(true); f.set(o,v);
    }
    static void check(boolean b,String name) { if(!b) throw new AssertionError(name); }
    public static void main(String[] args) throws Exception {
        Guard.start();

        Object pass=U.allocateInstance(Class.forName("viewpoint.render.ModelPass"));
        Object draws=U.allocateInstance(Class.forName("viewpoint.render.ModelDraws"));
        ModelInstance root=(ModelInstance)U.allocateInstance(ModelInstance.class);
        VehicleSubModelInstance part=(VehicleSubModelInstance)U.allocateInstance(VehicleSubModelInstance.class);
        part.parent=root; part.object=(BaseVehicle)U.allocateInstance(BaseVehicle.class);
        ModelInstance[] list={root,part};
        float[] v=new float[120]; v[84]=2f; v[87]=0.1f;
        int[] flags={1,1};
        put(draws,"instances",new ModelInstance[2]); put(draws,"keys",list); put(draws,"values",v); put(draws,"flags",flags);
        FrustumIntersection fr=new FrustumIntersection(new Matrix4f());
        put(pass,"frustum",fr);
        check(!fr.testSphere(2,0,0,.1f),"fixture rejected by actual frustum");
        check(Guard.shown(pass,draws,1,false),"near rejected part rescued");
        check(!Guard.shown(pass,draws,0,false),"body unchanged");
        flags[1]=0; check(!Guard.shown(pass,draws,1,false),"colour flag preserved"); flags[1]=1;
        part.parent=null; check(!Guard.shown(pass,draws,1,false),"root excluded"); part.parent=root;
        part.object=null; check(!Guard.shown(pass,draws,1,false),"recycled instance excluded");
        part.object=(BaseVehicle)U.allocateInstance(BaseVehicle.class);
        v[84]=100f; check(!Guard.shown(pass,draws,1,false),"far part still culled");
        v[84]=Float.NaN; check(!Guard.shown(pass,draws,1,false),"invalid bounds unchanged");
        check(Guard.shown(pass,draws,1,true),"accepted draw unchanged");
        list[1]=root; check(!Guard.shown(pass,draws,1,false),"nonvehicle unchanged");
        list[1]=part;

        v[84]=-8; v[85]=0; v[86]=0; v[87]=.1f;
        check(Guard.shown(pass,draws,1,false),"near rejected part keeps bypass behaviour");
        v[84]=100;
        check(!Guard.shown(pass,draws,1,false),"adaptive far part remains culled");
        flags[1]=0; v[84]=0;
        check(!Guard.shown(pass,draws,1,false),"adaptive preserves colour flags");
        flags[1]=1; v[84]=Float.NaN;
        check(!Guard.shown(pass,draws,1,false),"adaptive preserves invalid bounds");
        check(Guard.near(12.05f,0,0,.1f,12),"sphere overlap included");
        check(!Guard.near(12.2f,0,0,.1f,12),"outside near range excluded");
        v[84]=13f; v[87]=.1f; put(pass,"frustum",new FrustumIntersection(new Matrix4f().translation(-9f,0,0)));
        check(Guard.shown(pass,draws,1,false),"far adaptive sphere expansion used");
        v[84]=15f; check(!Guard.shown(pass,draws,1,false),"beyond expanded frustum stays culled");
        System.out.println("PASS: 17 guard assertions with installed Viewpoint and PZ classes");
    }
}


