package tech.skidonion.sdk.event.events;

import org.objectweb.asm.tree.ClassNode;
import tech.skidonion.sdk.data.EventSequence;
import tech.skidonion.sdk.event.impl.CancellableEvent;

public class TransformEvent extends CancellableEvent {
    private final EventSequence sequence;
    private final ClassNode classNode;

    public TransformEvent(EventSequence sequence, ClassNode classNode) {
        this.sequence = sequence;
        this.classNode = classNode;
    }

    public EventSequence getSequence() {
        return sequence;
    }

    public ClassNode getClassNode() {
        return classNode;
    }
}
