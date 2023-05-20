package cpw.mods.modlauncher;

import cpw.mods.modlauncher.serviceapi.ILaunchPluginService;
import org.objectweb.asm.Type;

import java.nio.file.Path;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

/**
 * @author xtrm
 */
public class FukkitLaunchPlugin implements ILaunchPluginService {
    @Override
    public String name() {
        return "fukkit";
    }

    @Override
    public EnumSet<Phase> handlesClass(Type classType, boolean isEmpty) {
        return EnumSet.noneOf(Phase.class);
    }

    @Override
    public void addResources(List<Map.Entry<String, Path>> resources) {
        FukkitHooks.overrideTransformationServicesHandler();
    }
}
