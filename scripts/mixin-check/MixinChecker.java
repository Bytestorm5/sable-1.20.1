import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Static mixin audit for the 1.20.1 port. The Mixin annotation processor only checks that targets can be mapped; this
 * checks what would otherwise only fail when the game loads the target class:
 * <ul>
 *     <li>injector target methods exist in the target class (by name and, if given, descriptor)</li>
 *     <li>{@code @Inject} handler parameters match the target method's parameters</li>
 *     <li>{@code @Redirect} / {@code @WrapOperation} handlers match the redirected call's shape</li>
 *     <li>{@code @At(INVOKE / FIELD / NEW)} targets actually occur in the target method's bytecode, with enough
 *     occurrences for the requested ordinal</li>
 *     <li>{@code @Shadow}, {@code @Accessor} and {@code @Invoker} members exist in the target class hierarchy</li>
 * </ul>
 * Run through the {@code checkMixins} Gradle task of the {@code :forge} project. Mixins whose target class is not on
 * the classpath (optional compat targets) are reported separately and not checked further.
 */
public final class MixinChecker {

    private static final String MIXIN = "Lorg/spongepowered/asm/mixin/Mixin;";
    private static final String SHADOW = "Lorg/spongepowered/asm/mixin/Shadow;";
    private static final String OVERWRITE = "Lorg/spongepowered/asm/mixin/Overwrite;";
    private static final String ACCESSOR = "Lorg/spongepowered/asm/mixin/gen/Accessor;";
    private static final String INVOKER = "Lorg/spongepowered/asm/mixin/gen/Invoker;";
    private static final String INJECT = "Lorg/spongepowered/asm/mixin/injection/Inject;";
    private static final String REDIRECT = "Lorg/spongepowered/asm/mixin/injection/Redirect;";
    private static final String WRAP_OPERATION = "Lcom/llamalad7/mixinextras/injector/wrapoperation/WrapOperation;";
    private static final String CALLBACK_INFO = "org/spongepowered/asm/mixin/injection/callback/CallbackInfo";
    private static final String CALLBACK_INFO_RETURNABLE = "org/spongepowered/asm/mixin/injection/callback/CallbackInfoReturnable";
    private static final String OPERATION = "com/llamalad7/mixinextras/injector/wrapoperation/Operation";
    private static final List<String> INJECTORS = List.of(
            INJECT, REDIRECT, WRAP_OPERATION,
            "Lorg/spongepowered/asm/mixin/injection/ModifyArg;",
            "Lorg/spongepowered/asm/mixin/injection/ModifyArgs;",
            "Lorg/spongepowered/asm/mixin/injection/ModifyVariable;",
            "Lorg/spongepowered/asm/mixin/injection/ModifyConstant;",
            "Lcom/llamalad7/mixinextras/injector/ModifyExpressionValue;",
            "Lcom/llamalad7/mixinextras/injector/ModifyReturnValue;",
            "Lcom/llamalad7/mixinextras/injector/ModifyReceiver;",
            "Lcom/llamalad7/mixinextras/injector/v2/WrapWithCondition;",
            "Lcom/llamalad7/mixinextras/injector/WrapWithCondition;",
            "Lcom/llamalad7/mixinextras/injector/wrapmethod/WrapMethod;");
    /** Parameter annotations that mark MixinExtras sugar parameters, which aren't part of the target signature. */
    private static final List<String> SUGAR = List.of(
            "Lcom/llamalad7/mixinextras/sugar/Local;",
            "Lcom/llamalad7/mixinextras/sugar/Share;",
            "Lcom/llamalad7/mixinextras/sugar/Cancellable;");
    private static final Pattern MEMBER_REF = Pattern.compile("^(?:L([^;]+);)?([^(:]+)(?:(\\(.*)|:(.+))?$");

    private final ClassLoader classpath;
    private final Map<String, ClassNode> cache = new HashMap<>();
    private final List<String> errors = new ArrayList<>();
    private final List<String> missingTargets = new ArrayList<>();
    private int checkedInjectors;

    private MixinChecker(final ClassLoader classpath) {
        this.classpath = classpath;
    }

    public static void main(final String[] args) throws IOException {
        final MixinChecker checker = new MixinChecker(MixinChecker.class.getClassLoader());
        for (final String config : args) {
            checker.checkConfig(Path.of(config));
        }

        System.out.println("Checked " + checker.checkedInjectors + " injectors");
        if (!checker.missingTargets.isEmpty()) {
            System.out.println("\nMixins whose target class is not on the compile classpath (not checked):");
            checker.missingTargets.forEach(line -> System.out.println("  " + line));
        }
        if (!checker.errors.isEmpty()) {
            System.out.println("\n" + checker.errors.size() + " problem(s):");
            checker.errors.forEach(line -> System.out.println("  " + line));
            System.exit(1);
        }
        System.out.println("No problems found");
    }

    private void checkConfig(final Path config) throws IOException {
        final String json = Files.readString(config);
        final Matcher pkg = Pattern.compile("\"package\"\\s*:\\s*\"([^\"]+)\"").matcher(json);
        if (!pkg.find()) {
            throw new IllegalArgumentException("No package in " + config);
        }
        final String mixinPackage = pkg.group(1);
        final Matcher lists = Pattern.compile("\"(mixins|client|server)\"\\s*:\\s*\\[([^]]*)]").matcher(json);
        while (lists.find()) {
            final Matcher entries = Pattern.compile("\"([^\"]+)\"").matcher(lists.group(2));
            while (entries.find()) {
                this.checkMixin((mixinPackage + "." + entries.group(1)).replace('.', '/'));
            }
        }
    }

    private ClassNode load(final String internalName) {
        return this.cache.computeIfAbsent(internalName, name -> {
            try (final InputStream in = this.classpath.getResourceAsStream(name + ".class")) {
                if (in == null) {
                    return null;
                }
                final ClassNode node = new ClassNode();
                new ClassReader(in).accept(node, 0);
                return node;
            } catch (final IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private void checkMixin(final String mixinName) {
        final ClassNode mixin = this.load(mixinName);
        if (mixin == null) {
            this.errors.add(mixinName + ": listed in the mixin config but not compiled");
            return;
        }
        final AnnotationNode mixinAnnotation = annotation(mixin, MIXIN);
        if (mixinAnnotation == null) {
            this.errors.add(mixinName + ": no @Mixin annotation");
            return;
        }

        final List<String> targets = new ArrayList<>();
        for (final Object value : list(mixinAnnotation, "value")) {
            targets.add(((Type) value).getInternalName());
        }
        for (final Object value : list(mixinAnnotation, "targets")) {
            targets.add(((String) value).replace('.', '/'));
        }

        for (final String targetName : targets) {
            final ClassNode target = this.load(targetName);
            if (target == null) {
                this.missingTargets.add(simple(mixinName) + " -> " + targetName);
                continue;
            }
            this.checkAgainst(mixin, target);
        }
    }

    private void checkAgainst(final ClassNode mixin, final ClassNode target) {
        final String where = simple(mixin.name);

        // Mixin 0.8.5 (Forge 1.20.1) rejects interface mixins with non-public methods, including lambdas
        if ((mixin.access & Opcodes.ACC_INTERFACE) != 0) {
            for (final MethodNode method : mixin.methods) {
                if ((method.access & Opcodes.ACC_PUBLIC) == 0 && !method.name.equals("<clinit>")) {
                    this.errors.add(where + ": interface mixin has non-public method " + method.name + method.desc);
                }
            }
        }

        for (final FieldNode field : mixin.fields) {
            if (annotation(field, SHADOW) != null) {
                final String name = stripPrefix(field.name, annotation(field, SHADOW));
                if (this.findField(target, name, field.desc) == null) {
                    this.errors.add(where + ": @Shadow field " + name + " " + field.desc + " not found in " + target.name);
                }
            }
        }

        for (final MethodNode method : mixin.methods) {
            final AnnotationNode shadow = annotation(method, SHADOW);
            if (shadow != null) {
                final String name = stripPrefix(method.name, shadow);
                if (this.findMethod(target, name, method.desc) == null) {
                    this.errors.add(where + ": @Shadow method " + name + method.desc + " not found in " + target.name);
                }
            }
            if (annotation(method, OVERWRITE) != null && this.findMethod(target, method.name, method.desc) == null) {
                this.errors.add(where + ": @Overwrite " + method.name + method.desc + " not found in " + target.name);
            }
            this.checkAccessor(where, target, method);

            for (final String injector : INJECTORS) {
                final AnnotationNode annotation = annotation(method, injector);
                if (annotation != null) {
                    this.checkedInjectors++;
                    this.checkInjector(where, target, method, injector, annotation);
                }
            }
        }
    }

    private void checkAccessor(final String where, final ClassNode target, final MethodNode method) {
        final AnnotationNode accessor = annotation(method, ACCESSOR);
        if (accessor != null) {
            String name = (String) value(accessor, "value");
            final Type[] args = Type.getArgumentTypes(method.desc);
            final String fieldDesc = args.length == 0 ? Type.getReturnType(method.desc).getDescriptor() : args[args.length - 1].getDescriptor();
            if (name == null || name.isEmpty()) {
                name = decapitalize(method.name.replaceFirst("^(get|set|is)", ""));
            }
            if (this.findField(target, name, fieldDesc) == null) {
                this.errors.add(where + ": @Accessor field " + name + " " + fieldDesc + " not found in " + target.name);
            }
        }
        final AnnotationNode invoker = annotation(method, INVOKER);
        if (invoker != null) {
            String name = (String) value(invoker, "value");
            if (name == null || name.isEmpty()) {
                name = decapitalize(method.name.replaceFirst("^(call|invoke|new|create)", ""));
            }
            String desc = method.desc;
            if (name.equals("<init>")) {
                desc = Type.getMethodDescriptor(Type.VOID_TYPE, Type.getArgumentTypes(method.desc));
            }
            if (this.findMethod(target, name, desc) == null) {
                this.errors.add(where + ": @Invoker method " + name + desc + " not found in " + target.name);
            }
        }
    }

    private void checkInjector(final String where, final ClassNode target, final MethodNode handler, final String injector, final AnnotationNode annotation) {
        final String kind = injector.substring(injector.lastIndexOf('/') + 1, injector.length() - 1);
        final List<MethodNode> targetMethods = new ArrayList<>();
        final List<Object> selectors = list(annotation, "method");
        if (selectors.isEmpty()) {
            this.errors.add(where + "." + handler.name + ": @" + kind + " without a method selector");
            return;
        }
        // Alternative names for the same method (e.g. dev and production lambda names) are fine as long as one matches
        for (final Object selector : selectors) {
            targetMethods.addAll(this.selectMethods(target, (String) selector));
        }
        if (targetMethods.isEmpty()) {
            this.errors.add(where + "." + handler.name + ": @" + kind + " target method " + selectors + " not found in " + target.name);
        }

        final List<Object> ats = new ArrayList<>(list(annotation, "at"));
        final Object singleAt = value(annotation, "at");
        if (singleAt instanceof AnnotationNode) {
            ats.add(singleAt);
        }

        for (final MethodNode targetMethod : targetMethods) {
            final String label = where + "." + handler.name + " -> " + target.name + "." + targetMethod.name + targetMethod.desc;
            if (injector.equals(INJECT)) {
                this.checkInjectHandler(label, targetMethod, handler);
            }
        }

        // Like Mixin, an injection point only has to match in one of the selected methods
        for (final Object atObject : ats) {
            final AnnotationNode at = (AnnotationNode) atObject;
            int total = 0;
            int max = 0;
            for (final MethodNode targetMethod : targetMethods) {
                final String label = where + "." + handler.name + " -> " + target.name + "." + targetMethod.name + targetMethod.desc;
                final int found = this.checkAt(label, targetMethod, at, injector, handler);
                if (found < 0) {
                    total = -1;
                    break;
                }
                total += found;
                max = Math.max(max, found);
            }
            if (total == 0 && !targetMethods.isEmpty()) {
                this.errors.add(where + "." + handler.name + ": @At(" + value(at, "value") + ") target " + value(at, "target") + " does not occur in " + target.name + "." + selectors);
            }
            final Object ordinal = value(at, "ordinal");
            if (total > 0 && ordinal != null && (Integer) ordinal >= max && value(at, "slice") == null) {
                this.errors.add(where + "." + handler.name + ": @At(ordinal = " + ordinal + ") but " + value(at, "target") + " occurs at most " + max + " time(s) in " + target.name + "." + selectors);
            }
        }
    }

    private void checkInjectHandler(final String label, final MethodNode target, final MethodNode handler) {
        final List<Type> params = this.nonSugarParameters(handler);
        final Type[] targetParams = Type.getArgumentTypes(target.desc);
        int ci = -1;
        for (int i = 0; i < params.size(); i++) {
            final String name = params.get(i).getInternalName();
            if (params.get(i).getSort() == Type.OBJECT && (name.equals(CALLBACK_INFO) || name.equals(CALLBACK_INFO_RETURNABLE))) {
                ci = i;
                break;
            }
        }
        if (ci < 0) {
            this.errors.add(label + ": @Inject handler has no CallbackInfo parameter");
            return;
        }
        if (ci != 0 && ci != targetParams.length) {
            this.errors.add(label + ": @Inject handler takes " + ci + " target parameter(s), target has " + targetParams.length);
            return;
        }
        for (int i = 0; i < ci; i++) {
            if (!params.get(i).equals(targetParams[i])) {
                this.errors.add(label + ": @Inject parameter " + i + " is " + params.get(i) + ", target has " + targetParams[i]);
            }
        }
        final boolean returnable = params.get(ci).getInternalName().equals(CALLBACK_INFO_RETURNABLE);
        final boolean targetReturns = Type.getReturnType(target.desc).getSort() != Type.VOID && !target.name.equals("<init>");
        if (returnable != targetReturns && !(target.name.equals("<init>") || target.name.equals("<clinit>"))) {
            this.errors.add(label + ": @Inject uses " + (returnable ? "CallbackInfoReturnable" : "CallbackInfo") + " but target returns " + Type.getReturnType(target.desc));
        }
        if ((handler.access & Opcodes.ACC_STATIC) != (target.access & Opcodes.ACC_STATIC)) {
            this.errors.add(label + ": @Inject handler staticness differs from target");
        }
    }

    private int checkAt(final String label, final MethodNode target, final AnnotationNode at, final String injector, final MethodNode handler) {
        final String value = (String) value(at, "value");
        final String ref = (String) value(at, "target");
        if (value == null || ref == null || ref.isEmpty()) {
            return -1;
        }

        int found = 0;
        MethodInsnNode invoke = null;
        FieldInsnNode fieldInsn = null;
        final Matcher m = MEMBER_REF.matcher(ref);
        switch (value) {
            case "INVOKE", "INVOKE_ASSIGN", "INVOKE_STRING" -> {
                if (!m.matches()) {
                    return -1;
                }
                for (final AbstractInsnNode insn : target.instructions) {
                    if (insn instanceof final MethodInsnNode call
                            && (m.group(1) == null || call.owner.equals(m.group(1)))
                            && call.name.equals(m.group(2))
                            && (m.group(3) == null || call.desc.equals(m.group(3)))) {
                        found++;
                        invoke = call;
                    }
                }
            }
            case "FIELD" -> {
                if (!m.matches()) {
                    return -1;
                }
                for (final AbstractInsnNode insn : target.instructions) {
                    if (insn instanceof final FieldInsnNode field
                            && (m.group(1) == null || field.owner.equals(m.group(1)))
                            && field.name.equals(m.group(2))
                            && (m.group(4) == null || field.desc.equals(m.group(4)))) {
                        found++;
                        fieldInsn = field;
                    }
                }
            }
            case "NEW" -> {
                // Either the type ("Lpkg/Type;") or a constructor descriptor ("(DDD)Lpkg/Type;")
                final String type = ref.startsWith("(") ? Type.getReturnType(ref).getInternalName()
                        : ref.startsWith("L") && ref.endsWith(";") ? ref.substring(1, ref.length() - 1) : ref;
                for (final AbstractInsnNode insn : target.instructions) {
                    if (insn instanceof final TypeInsnNode typeInsn && insn.getOpcode() == Opcodes.NEW && typeInsn.desc.equals(type)) {
                        found++;
                    }
                }
            }
            default -> {
                return -1;
            }
        }

        if (invoke != null && (injector.equals(REDIRECT) || injector.equals(WRAP_OPERATION))) {
            this.checkCallHandler(label, invoke, handler, injector.equals(WRAP_OPERATION));
        }
        if (fieldInsn != null && injector.equals(REDIRECT)) {
            this.checkFieldRedirect(label, fieldInsn, handler);
        }
        return found;
    }

    private void checkCallHandler(final String label, final MethodInsnNode call, final MethodNode handler, final boolean wrapOperation) {
        final List<Type> expected = new ArrayList<>();
        if (call.getOpcode() != Opcodes.INVOKESTATIC) {
            expected.add(Type.getObjectType(call.owner));
        }
        expected.addAll(List.of(Type.getArgumentTypes(call.desc)));
        final List<Type> params = this.nonSugarParameters(handler);
        if (wrapOperation) {
            if (params.size() < expected.size() + 1 || !params.get(expected.size()).getInternalName().equals(OPERATION)) {
                this.errors.add(label + ": @WrapOperation handler should take " + expected + " followed by Operation");
                return;
            }
        } else if (params.size() < expected.size()) {
            this.errors.add(label + ": @Redirect handler should take " + expected + ", takes " + params);
            return;
        }
        for (int i = 0; i < expected.size(); i++) {
            if (!assignable(expected.get(i), params.get(i))) {
                this.errors.add(label + ": handler parameter " + i + " is " + params.get(i) + ", call has " + expected.get(i));
            }
        }
        final Type callReturn = Type.getReturnType(call.desc);
        final Type handlerReturn = Type.getReturnType(handler.desc);
        if (!callReturn.equals(handlerReturn) && !(callReturn.getSort() == Type.OBJECT && handlerReturn.getSort() == Type.OBJECT)) {
            this.errors.add(label + ": handler returns " + handlerReturn + ", call returns " + callReturn);
        }
    }

    private void checkFieldRedirect(final String label, final FieldInsnNode field, final MethodNode handler) {
        final Type fieldType = Type.getType(field.desc);
        final List<Type> params = this.nonSugarParameters(handler);
        final boolean isGet = field.getOpcode() == Opcodes.GETFIELD || field.getOpcode() == Opcodes.GETSTATIC;
        final boolean isStatic = field.getOpcode() == Opcodes.GETSTATIC || field.getOpcode() == Opcodes.PUTSTATIC;
        final int expectedCount = (isStatic ? 0 : 1) + (isGet ? 0 : 1);
        if (params.size() < expectedCount) {
            this.errors.add(label + ": field @Redirect handler has too few parameters");
            return;
        }
        if (isGet && !Type.getReturnType(handler.desc).equals(fieldType) && fieldType.getSort() != Type.OBJECT) {
            this.errors.add(label + ": field @Redirect returns " + Type.getReturnType(handler.desc) + ", field is " + fieldType);
        }
    }

    private List<Type> nonSugarParameters(final MethodNode method) {
        final Type[] args = Type.getArgumentTypes(method.desc);
        final List<Type> result = new ArrayList<>();
        for (int i = 0; i < args.length; i++) {
            boolean sugar = false;
            for (final List<AnnotationNode>[] annotations : List.of(
                    nullSafe(method.invisibleParameterAnnotations, args.length),
                    nullSafe(method.visibleParameterAnnotations, args.length))) {
                if (annotations[i] != null) {
                    for (final AnnotationNode node : annotations[i]) {
                        if (SUGAR.contains(node.desc)) {
                            sugar = true;
                        }
                    }
                }
            }
            if (!sugar) {
                result.add(args[i]);
            }
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private static List<AnnotationNode>[] nullSafe(final List<AnnotationNode>[] annotations, final int size) {
        return annotations != null ? annotations : new List[size];
    }

    private List<MethodNode> selectMethods(final ClassNode target, final String selector) {
        final List<MethodNode> result = new ArrayList<>();
        String sel = selector;
        if (sel.startsWith("L") && sel.contains(";")) {
            sel = sel.substring(sel.indexOf(';') + 1);
        }
        final String name;
        final String desc;
        if (sel.contains("(")) {
            name = sel.substring(0, sel.indexOf('('));
            desc = sel.substring(sel.indexOf('('));
        } else {
            name = sel;
            desc = null;
        }
        for (final MethodNode method : target.methods) {
            if ((name.equals("*") || method.name.equals(name)) && (desc == null || method.desc.equals(desc))) {
                result.add(method);
            }
        }
        return result;
    }

    private FieldNode findField(final ClassNode start, final String name, final String desc) {
        for (ClassNode node = start; node != null; node = node.superName != null ? this.load(node.superName) : null) {
            for (final FieldNode field : node.fields) {
                if (field.name.equals(name) && field.desc.equals(desc)) {
                    return field;
                }
            }
        }
        return null;
    }

    private MethodNode findMethod(final ClassNode start, final String name, final String desc) {
        final List<ClassNode> queue = new ArrayList<>();
        queue.add(start);
        while (!queue.isEmpty()) {
            final ClassNode node = queue.remove(0);
            for (final MethodNode method : node.methods) {
                if (method.name.equals(name) && method.desc.equals(desc)) {
                    return method;
                }
            }
            if (node.superName != null) {
                final ClassNode parent = this.load(node.superName);
                if (parent != null) {
                    queue.add(parent);
                }
            }
            for (final String itf : node.interfaces) {
                final ClassNode parent = this.load(itf);
                if (parent != null) {
                    queue.add(parent);
                }
            }
        }
        return null;
    }

    private boolean assignable(final Type expected, final Type actual) {
        if (expected.equals(actual)) {
            return true;
        }
        if (expected.getSort() != Type.OBJECT || actual.getSort() != Type.OBJECT) {
            return false;
        }
        // The handler may declare a supertype of the real argument
        final List<ClassNode> queue = new ArrayList<>();
        final ClassNode start = this.load(expected.getInternalName());
        if (start == null) {
            return true;
        }
        queue.add(start);
        while (!queue.isEmpty()) {
            final ClassNode node = queue.remove(0);
            if (node.name.equals(actual.getInternalName())) {
                return true;
            }
            if (node.superName != null && this.load(node.superName) != null) {
                queue.add(this.load(node.superName));
            }
            for (final String itf : node.interfaces) {
                if (this.load(itf) != null) {
                    queue.add(this.load(itf));
                }
            }
        }
        return actual.getInternalName().equals("java/lang/Object");
    }

    private static String stripPrefix(final String name, final AnnotationNode shadow) {
        final Object prefix = shadow != null ? value(shadow, "prefix") : null;
        final String p = prefix != null ? (String) prefix : "shadow$";
        return name.startsWith(p) ? name.substring(p.length()) : name;
    }

    private static AnnotationNode annotation(final ClassNode node, final String desc) {
        final AnnotationNode found = annotation(node.visibleAnnotations, desc);
        return found != null ? found : annotation(node.invisibleAnnotations, desc);
    }

    private static AnnotationNode annotation(final FieldNode node, final String desc) {
        final AnnotationNode found = annotation(node.visibleAnnotations, desc);
        return found != null ? found : annotation(node.invisibleAnnotations, desc);
    }

    private static AnnotationNode annotation(final MethodNode node, final String desc) {
        final AnnotationNode found = annotation(node.visibleAnnotations, desc);
        return found != null ? found : annotation(node.invisibleAnnotations, desc);
    }

    private static AnnotationNode annotation(final List<AnnotationNode> annotations, final String desc) {
        if (annotations == null) {
            return null;
        }
        for (final AnnotationNode node : annotations) {
            if (node.desc.equals(desc)) {
                return node;
            }
        }
        return null;
    }

    private static Object value(final AnnotationNode annotation, final String key) {
        if (annotation.values == null) {
            return null;
        }
        for (int i = 0; i < annotation.values.size(); i += 2) {
            if (annotation.values.get(i).equals(key)) {
                return annotation.values.get(i + 1);
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private static List<Object> list(final AnnotationNode annotation, final String key) {
        final Object value = value(annotation, key);
        if (value == null) {
            return List.of();
        }
        if (value instanceof List<?>) {
            return (List<Object>) value;
        }
        return List.of(value);
    }

    private static String decapitalize(final String name) {
        if (name.isEmpty()) {
            return name;
        }
        if (name.equals(name.toUpperCase(java.util.Locale.ROOT))) {
            return name; // Mixin keeps all-caps names, e.g. getURL -> URL
        }
        return Character.toLowerCase(name.charAt(0)) + name.substring(1);
    }

    private static String simple(final String internalName) {
        final String marker = "/mixin/";
        final int index = internalName.indexOf(marker);
        return index >= 0 ? internalName.substring(index + marker.length()).replace('/', '.') : internalName;
    }
}
