package tech.skidonion.obfuscator.transformer.generic.poly.visitors;


public abstract class AbstractLanguageVisitor implements Visitor<StringBuilder> {

	public String hex(int l) {
		return "0x"+Integer.toHexString(l);
	}
	
}
