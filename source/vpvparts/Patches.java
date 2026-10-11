package vpvparts;

import me.zed_0xff.zombie_buddy.Patch;

public final class Patches {
    @Patch(className="viewpoint.render.ModelPass", methodName="shown")
    public static class Shown {
        @Patch.OnExit(suppress=Throwable.class)
        public static void exit(@Patch.This Object pass, @Patch.Argument(0) Object draws,
                                @Patch.Argument(1) int index,
                                @Patch.Return(readOnly=false) boolean shown) {
            shown=Guard.shown(pass,draws,index,shown);
        }
    }
    // Optional target: absent when Rolling Hills is not installed.
    @Patch(className="wt.rollinghills.VehiclePose", methodName="shown")
    public static class RollingHillsShown {
        @Patch.OnExit(suppress=Throwable.class)
        public static void exit(@Patch.Argument(0) Object pass, @Patch.Argument(1) Object draws,
                                @Patch.Argument(2) int index, @Patch.Argument(3) boolean incoming,
                                @Patch.Return(readOnly=false) boolean shown) {
            shown=Guard.preserveRollingRescue(pass,draws,index,incoming,shown);
        }
    }
}
