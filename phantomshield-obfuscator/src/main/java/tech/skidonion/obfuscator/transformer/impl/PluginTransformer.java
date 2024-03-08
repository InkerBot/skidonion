package tech.skidonion.obfuscator.transformer.impl;

import tech.skidonion.obfuscator.transformer.Transformer;
import tech.skidonion.sdk.PhantomShieldSDK;
import tech.skidonion.sdk.data.EventSequence;
import tech.skidonion.sdk.event.events.TransformEvent;

public class PluginTransformer extends Transformer {
    private final boolean pre;
    public PluginTransformer(String name,boolean pre) {
        super(name,true);
        this.pre = pre;
    }

    @Override
    public void transform() {
        getFilteredClasses().forEach(classWrapper -> {
            TransformEvent event = new TransformEvent(pre ? EventSequence.PRE : EventSequence.POST,classWrapper.getClassNode());
            PhantomShieldSDK.EVENT_BUS.call(event);
            if(event.isCancelled())
                getClassWrappers().remove(classWrapper);
        });
    }

    @Override
    public void preprocess() throws Exception {

    }

    @Override
    public String annotation() {
        return null;
    }
}
