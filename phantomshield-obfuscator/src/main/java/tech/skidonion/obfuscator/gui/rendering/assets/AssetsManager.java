package tech.skidonion.obfuscator.gui.rendering.assets;

import tech.skidonion.obfuscator.gui.rendering.assets.svg.SvgLoader;

public final class AssetsManager {

    public static AssetsManager INSTANCE = new AssetsManager();

    private final SvgLoader svgLoader = new SvgLoader();

    public SvgLoader getSvgLoader() {
        return svgLoader;
    }
}
