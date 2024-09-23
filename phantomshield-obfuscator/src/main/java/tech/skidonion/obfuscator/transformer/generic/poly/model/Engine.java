package tech.skidonion.obfuscator.transformer.generic.poly.model;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

public interface Engine {
    Context transform(byte[] bytes) throws Exception;

    Context generateChain() throws Exception;
}
