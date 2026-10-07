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
}
