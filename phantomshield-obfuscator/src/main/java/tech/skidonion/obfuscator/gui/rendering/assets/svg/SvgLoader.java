package tech.skidonion.obfuscator.gui.rendering.assets.svg;

import org.lwjgl.nanovg.NSVGImage;
import org.lwjgl.system.MemoryStack;
import tech.skidonion.obfuscator.gui.utils.NsvgUtil;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.lwjgl.nanovg.NanoSVG.*;
import static org.lwjgl.nanovg.NanoVG.*;
import static org.lwjgl.system.MemoryStack.stackPush;

public final class SvgLoader {

    private final Map<String, Integer> svgMap = new HashMap<>();

    private final List<String> blocked = new ArrayList<>();

    public boolean available(long nvgContext, String svgPath, int renderingWidth, int renderingHeight) {
        final String identityString = createIdentityString(svgPath, renderingWidth, renderingHeight);

        if (blocked.contains(identityString)) {
            return false;
        }

        if (!svgMap.containsKey(identityString)) {

            try (MemoryStack stack = stackPush()) {

                final NSVGImage svg = NsvgUtil.parse(getClass().getResourceAsStream(svgPath));

                if (svg == null) {
                    blocked.add(identityString);
                    return false;
                }

                final long rasterizer = nsvgCreateRasterizer();

                int svgWidth = (int) svg.width();
                int svgHeight = (int) svg.height();

                final float scale = Math.max(renderingWidth / svgWidth, renderingHeight / svgHeight);

                svgWidth = (int) (svgWidth * scale);
                svgHeight = (int) (svgHeight * scale);

                final ByteBuffer image = stack.malloc(svgWidth * svgHeight * 4);
                nsvgRasterize(rasterizer, svg, 0, 0, scale, image, svgWidth, svgHeight, svgWidth * 4);

                nsvgDeleteRasterizer(rasterizer);
                nsvgDelete(svg);

                svgMap.put(identityString, nvgCreateImageRGBA(nvgContext, svgWidth, svgHeight, NVG_IMAGE_REPEATX | NVG_IMAGE_REPEATY | NVG_IMAGE_GENERATE_MIPMAPS, image));
                return true;
            } catch (Exception e) {
                blocked.add(identityString);
                return false;
            }
        }
        return true;
    }

    public int get(String svgPath, int renderingWidth, int renderingHeight) {
        return svgMap.get(createIdentityString(svgPath, renderingWidth, renderingHeight));
    }

    private String createIdentityString(String svgPath, int renderingWidth, int renderingHeight) {
        return String.format("%s-%s-%s", svgPath, renderingWidth, renderingHeight);
    }
}
