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
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import androidx.annotation.NonNull;
import falcosc.locus.addon.tasker.intent.handler.LocusInfoRequest;
import falcosc.locus.addon.tasker.utils.TaskerField;
import locus.api.android.objects.LocusInfo;

/** @noinspection HardCodedStringLiteral*/
public class LocusInfoGettersTest {

    @Test
    public void testReadmeDocumentation() throws IOException {
        List<TaskerField> taskerFields = LocusInfoRequest.getFieldNames();
        List<String> missingFields = taskerFields.stream().map(taskerField -> taskerField.mTaskerName).collect(Collectors.toList());

        try (Stream<String> linesStream = Files.lines(Paths.get("../README.md").toAbsolutePath())) {
            // Collect all lines starting with "-" after "### Data Access"
            linesStream.dropWhile(line -> !line.contains("# Data Access")) // drop content before data access
                    .filter(line -> line.trim().startsWith("- "))
                    .forEach(field -> missingFields.remove(field.substring(2).trim()));
        }

        // Assert that all getters are used (for example purposes; modify as needed)
        Assert.assertTrue( missingFields.isEmpty(),
                "Following Fields need to be documented in README.md:\n" + String.join( "\n", missingFields));
    }

    @Test
    public void testUnusedGetters() throws Exception {
        Set<String> unusedGetters = new HashSet<>();
        Class<?> apiClass = LocusInfo.class;
        for (Method method : apiClass.getDeclaredMethods()) {
            String name = method.getName();
            if (name.startsWith("get") && !name.equals("getVersion")) {
                unusedGetters.add(apiClass.getName() + "." + method.getName().split("\\$")[0]);
            }
        }

        Set<String> calledMethods = getCalledMethods(apiClass.getName());
        unusedGetters.removeAll(calledMethods);

        // Assert that all getters are used (for example purposes; modify as needed)
        Assert.assertTrue( unusedGetters.isEmpty(),
                "There are unused getters in LocusInfo:\n" + String.join( "\n", unusedGetters));
    }

    @NonNull
    private static Set<String> getCalledMethods(String apiClassName) throws IOException {
        // Collect all methods in UpdateContainerFieldFactory
        ClassReader classReader = new ClassReader(LocusInfoRequest.class.getName());
        ClassNode classNode = new ClassNode();
        classReader.accept(classNode, 0);

        Set<String> calledMethods = new HashSet<>();
        for (MethodNode method : classNode.methods) {
            for (AbstractInsnNode insn : method.instructions) {
                if (insn.getOpcode() == Opcodes.INVOKEDYNAMIC) {
                    InvokeDynamicInsnNode dynamicInsn = (InvokeDynamicInsnNode) insn;
                    if (dynamicInsn.bsmArgs != null) {
                        for (Object bsmArg : dynamicInsn.bsmArgs) {
                            if (bsmArg instanceof Handle) {
                                Handle handle = (Handle) bsmArg;
                                String className = handle.getOwner().replace('/', '.');
                                if (apiClassName.equals(className)) {
                                    calledMethods.add(className + "." + handle.getName());
                                }
                            }
                        }
                    }
                }
                if (insn instanceof MethodInsnNode) {
                    MethodInsnNode methodInsn = (MethodInsnNode) insn;
                    String className = methodInsn.owner.replace('/', '.');
                    if (apiClassName.equals(className)) {
                        calledMethods.add(className + "." + methodInsn.name);
                    }
                }
            }
        }
        return calledMethods;
    }

}
