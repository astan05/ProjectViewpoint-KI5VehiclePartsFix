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
        Field strideField=draws.getClass().getDeclaredField("VALUES"); strideField.setAccessible(true);
        int stride=strideField.getInt(null); int bounds=stride+24;
        float[] v=new float[stride*2]; v[bounds]=2f; v[bounds+3]=0.1f;
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
        v[bounds]=100f; check(!Guard.shown(pass,draws,1,false),"far part still culled");
        v[bounds]=Float.NaN; check(!Guard.shown(pass,draws,1,false),"invalid bounds unchanged");
        check(Guard.shown(pass,draws,1,true),"accepted draw unchanged");
        list[1]=root; check(!Guard.shown(pass,draws,1,false),"nonvehicle unchanged");
        list[1]=part;

        v[bounds]=-8; v[bounds+1]=0; v[bounds+2]=0; v[bounds+3]=.1f;
        check(Guard.shown(pass,draws,1,false),"near rejected part keeps bypass behaviour");
        v[bounds]=100;
        check(!Guard.shown(pass,draws,1,false),"adaptive far part remains culled");
        flags[1]=0; v[bounds]=0;
        check(!Guard.shown(pass,draws,1,false),"adaptive preserves colour flags");
        flags[1]=1; v[bounds]=Float.NaN;
        check(!Guard.shown(pass,draws,1,false),"adaptive preserves invalid bounds");
        check(Guard.near(12.05f,0,0,.1f,12),"sphere overlap included");
        check(!Guard.near(12.2f,0,0,.1f,12),"outside near range excluded");
        v[bounds]=13f; v[bounds+3]=.1f; put(pass,"frustum",new FrustumIntersection(new Matrix4f().translation(-9f,0,0)));
        check(Guard.shown(pass,draws,1,false),"far adaptive sphere expansion used");
        v[bounds]=15f; check(!Guard.shown(pass,draws,1,false),"beyond expanded frustum stays culled");
        v[bounds]=2f; v[bounds+1]=0;v[bounds+2]=0;v[bounds+3]=.1f;
        check(Guard.shown(pass,draws,1,false),"compat fixture rescued");
        check(Guard.preserveRollingRescue(pass,draws,1,true,false),"Rolling Hills cannot overwrite our rescue");
        check(!Guard.preserveRollingRescue(pass,draws,1,true,false),"rescue consumed once");
        Guard.shown(pass,draws,1,false);
        check(!Guard.preserveRollingRescue(pass,new Object(),1,true,false),"different frame not rescued");
        Guard.shown(pass,draws,1,false);
        check(!Guard.preserveRollingRescue(pass,draws,0,true,false),"different draw not rescued");
        Guard.shown(pass,draws,1,false); Guard.shown(pass,draws,0,true);
        check(!Guard.preserveRollingRescue(pass,draws,1,true,false),"subsequent accepted call clears stale rescue");
        Guard.shown(pass,draws,1,true);
        check(!Guard.preserveRollingRescue(pass,draws,1,true,false),"originally visible model not a rescue");
        flags[1]=0; Guard.shown(pass,draws,1,false);
        check(!Guard.preserveRollingRescue(pass,draws,1,true,false),"no-colour result not rescued"); flags[1]=1;
        check(!Guard.preserveRollingRescue(pass,draws,1,false,false),"Rolling Hills first has no pending rescue");
        check(Guard.shown(pass,draws,1,false),"reverse hook order still rescued by main guard");
        final boolean[] otherThread={true};
        Thread worker=new Thread(()->otherThread[0]=Guard.preserveRollingRescue(pass,draws,1,true,false));
        worker.start();worker.join();check(!otherThread[0],"rescue isolated by thread");
        check(Guard.preserveRollingRescue(pass,draws,1,true,false),"origin thread retains its rescue");
        check(Guard.preserveRollingRescue(pass,draws,1,false,true),"Rolling Hills accepted result retained");
        System.out.println("PASS: adaptive guard and Rolling Hills compatibility assertions");
    }
}


