import android.annotation.SuppressLint;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import androidx.annotation.NonNull;
import falcosc.locus.addon.tasker.uc.UpdateContainerFieldFactory;
import locus.api.android.features.periodicUpdates.UpdateContainer;
import locus.api.objects.extra.Location;
import locus.api.objects.extra.TrackStats;

public class UpdateContainerGettersTest {

    @Test
    @SuppressLint("HardcodedText")
    public void testUnusedGetters() throws Exception {
        // Collect all getters in UpdateContainer
        List<Class<?>> updateContainerClasses = new ArrayList<>();
        updateContainerClasses.add(UpdateContainer.class);
        updateContainerClasses.add(Location.class);
        updateContainerClasses.add(TrackStats.class);

        Set<String> unusedGetters = new HashSet<>();
        Set<String> apiClassNames = new HashSet<>();
        for(Class<?> updateContainerClass : updateContainerClasses) {
            apiClassNames.add(updateContainerClass.getName());
            Method[] updateContainerMethods = updateContainerClass.getDeclaredMethods();

            for (Method method : updateContainerMethods) {
                String name = method.getName();
                if (name.startsWith("get") && !name.equals("getVersion")) {
                    unusedGetters.add(updateContainerClass.getName() + "." + method.getName().split("\\$")[0]);
                }
            }
        }

        //No support for these API Getters
        unusedGetters.remove("locus.api.android.features.periodicUpdates.UpdateContainer.getContentGuideTrack");
        unusedGetters.remove("locus.api.android.features.periodicUpdates.UpdateContainer.getContentGuidePoint");
        unusedGetters.remove("locus.api.android.features.periodicUpdates.UpdateContainer.getDeviceBatteryValue");
        unusedGetters.remove("locus.api.android.features.periodicUpdates.UpdateContainer.getDeviceBatteryTemperature");
        unusedGetters.remove("locus.api.android.features.periodicUpdates.UpdateContainer.getOrientHeadingOpposit"); //already have heading
        unusedGetters.remove("locus.api.objects.extra.Location.getDataFloat");
        unusedGetters.remove("locus.api.objects.extra.Location.getSpeedOptimal"); //deprecated
        unusedGetters.remove("locus.api.objects.extra.Location.getDataDouble");
        unusedGetters.remove("locus.api.objects.extra.Location.getId");
        unusedGetters.remove("locus.api.objects.extra.Location.getDataString");
        unusedGetters.remove("locus.api.objects.extra.Location.getDataShort");
        unusedGetters.remove("locus.api.objects.extra.Location.getDataInt");
        unusedGetters.remove("locus.api.objects.extra.Location.getDataLong");
        unusedGetters.remove("locus.api.objects.extra.TrackStats.getTime"); //is a helper for with or without pause time
        unusedGetters.remove("locus.api.objects.extra.TrackStats.getLength"); //is a helper for with or without pause time

        Set<String> calledMethods = getCalledMethods(apiClassNames);
        unusedGetters.removeAll(calledMethods);

        // Assert that all getters are used (for example purposes; modify as needed)
        Assert.assertTrue( unusedGetters.isEmpty(),
                "There are unused getters in UpdateContainer:\n" + String.join( "\n", unusedGetters));
    }

    @NonNull
    private static Set<String> getCalledMethods(Set<String> apiClassNames) throws IOException {
        // Collect all methods in UpdateContainerFieldFactory
        ClassReader classReader = new ClassReader(UpdateContainerFieldFactory.class.getName());
        ClassNode classNode = new ClassNode();
        classReader.accept(classNode, 0);

        Set<String> calledMethods = new HashSet<>();
        for (MethodNode method : (List<MethodNode>) classNode.methods) {
            for (AbstractInsnNode insn : method.instructions) {
                if (insn.getOpcode() == Opcodes.INVOKEDYNAMIC) {
                    InvokeDynamicInsnNode dynamicInsn = (InvokeDynamicInsnNode) insn;
                    if (dynamicInsn.bsmArgs != null) {
                        for (Object bsmArg : dynamicInsn.bsmArgs) {
                            if (bsmArg instanceof Handle) {
                                Handle handle = (Handle) bsmArg;
                                String className = handle.getOwner().replace('/', '.');
                                if (apiClassNames.contains(className)) {
                                    calledMethods.add(className + "." + handle.getName());
                                }
                            }
                        }
                    }
                }
                if (insn instanceof MethodInsnNode) {
                    MethodInsnNode methodInsn = (MethodInsnNode) insn;
                    String className = methodInsn.owner.replace('/', '.');
                    if (apiClassNames.contains(className)) {
                        calledMethods.add(className + "." + methodInsn.name);
                    }
                }
            }
        }
        return calledMethods;
    }

}
