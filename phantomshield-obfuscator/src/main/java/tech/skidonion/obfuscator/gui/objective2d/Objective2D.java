package tech.skidonion.obfuscator.gui.objective2d;

public abstract class Objective2D {

    protected int x, y;

    public int getX() {
        return x;
    }

    public void setX(int xIn) {
        x = xIn;
    }

    public int getY() {
        return y;
    }

    public void setY(int yIn) {
        y = yIn;
    }

    public Objective2D(int xIn, int yIn) {
        x = xIn;
        y = yIn;
    }

    public abstract void draw(long nvgContext);

    public abstract boolean contains(int anotherX, int anotherY);
}
