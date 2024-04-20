package tech.skidonion.obfuscator.gui.utils;

import org.lwjgl.nanovg.NSVGImage;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

import static org.lwjgl.nanovg.NanoSVG.nsvgParse;

public final class NsvgUtil {

    public static NSVGImage parse(InputStream stream) {
        final StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(stream))) {
            br.lines().forEach(sb::append);
            return nsvgParse(sb.toString(), "px", 96);
        } catch (Exception ignored) {
        }
        return null;
    }
}
