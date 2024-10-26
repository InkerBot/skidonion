package tech.skidonion.obfuscator.transformer.impl;

import lombok.Getter;
import tech.skidonion.obfuscator.asm.ClassWrapper;
import tech.skidonion.obfuscator.transformer.Transformer;
import tech.skidonion.obfuscator.transformer.impl.trashclasses.TrashClassGenerator;
import tech.skidonion.obfuscator.utils.RandomUtils;
import tech.skidonion.obfuscator.value.impls.NumberValue;
import tech.skidonion.obfuscator.value.impls.RangeValue;

import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import static tech.skidonion.obfuscator.PhantomShield.INFO;
import static tech.skidonion.obfuscator.PhantomShield.TRANSLATION;

public class TrashClassesInjector extends Transformer {

    private final NumberValue total_generated_classes = new NumberValue("total_generated_classes", 20);
    private final RangeValue fields_amount = new RangeValue("fields_amount", 3, 5);
    private final RangeValue methods_amount = new RangeValue("methods_amount", 3, 5);

    public TrashClassesInjector(String name) {
        super(name);
        addSettings(total_generated_classes, fields_amount, methods_amount);
    }

    private class Context {
        private final List<ClassWrapper> classNames;
        @Getter
        private final TrashClassGenerator generator;
        private final HashMap<String, Integer> conflictMap;

        public Context() {
            classNames = getFilteredClasses().collect(Collectors.toList());
            generator = new TrashClassGenerator(TrashClassesInjector.this.obfuscator);
            conflictMap = new HashMap<>();
        }

        public String randomClassName() {
            String first = classNames.get(RandomUtils.getRandomInt(classNames.size())).getName();
            String second = classNames.get(RandomUtils.getRandomInt(classNames.size())).getName();
            String name = first + '$' + second.substring(second.lastIndexOf("/") + 1);
            int index;
            if ((index = conflictMap.compute(name, (key, value) -> value == null ? -1 : value + 1)) != -1) {
                name += '$' + index;
            }
            return name;
        }


    }

    @Override
    public void preprocess() throws Exception {

    }

    @Override
    public void transform() throws Exception {

    }

    @Override
    public void postprocess() throws Exception {
        INFO(TRANSLATION("phantom-shield-x.trash-classes"));
        Context ctx = new Context();
        int total = total_generated_classes.getValue().intValue();

        int interfaces;
        int abstractions;
        int plain = total - (interfaces = abstractions = total * 2 / 5) * 2;

        for (int i = 0; i < interfaces; i++) {
            ctx.getGenerator().generateInterface(ctx.randomClassName(), methods_amount.getRandomValue(), fields_amount.getRandomValue());
        }

        for (int i = 0; i < abstractions; i++) {
            ctx.getGenerator().generateAbstraction(ctx.randomClassName(), methods_amount.getRandomValue(), fields_amount.getRandomValue());
        }

        for (int i = 0; i < plain; i++) {
            ctx.getGenerator().generate(ctx.randomClassName(), methods_amount.getRandomValue(), fields_amount.getRandomValue());
        }

        injectClassesAsResource(obfuscator.getGeneratedClassesEntryPrefix(), ctx.getGenerator().build());
    }

    @Override
    public String annotation() {
        return null;
    }

}
