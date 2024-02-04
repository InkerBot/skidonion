package tech.skidonion.obfuscator.utils.timer;

public class MSTimer {
    public long time;

    public MSTimer() {
        time = System.currentTimeMillis();
    }

    public boolean reached(long time) {
        return System.currentTimeMillis() - this.time >= time;
    }

    public void reset() {
        time = System.currentTimeMillis();
    }

}
