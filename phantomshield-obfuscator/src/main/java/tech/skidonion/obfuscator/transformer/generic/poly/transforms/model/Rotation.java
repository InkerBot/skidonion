package tech.skidonion.obfuscator.transformer.generic.poly.transforms.model;

public abstract class Rotation extends Transformation {
	private final int value;
	
	public Rotation(int value) {
		this.value = value;
	}
	
	public int getValue() {
		return value;
	}
	
	/* Both rotations use the same left/right hand sides */
	
	public int lhs() {
		return 32 - getValue();
	}
	
	public int rhs() {
		return getValue();
	}
}
