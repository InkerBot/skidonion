<h1 align="center">Phantom-Shield-X</h1>
<h4 align="center">Heavy duty to protect my jar</h4>

## 构建
将 phantomshield-loader 项目构建后的 jar 包重命名为 `loader.bin` 放入 `phantomshield-obfuscator/src/resources/binaries/` 目录下，然后执行 `Build All` 即可。

# 测试
运行 `TestRun` 运行参数

## 设置 Transformers 配置
你可以这么构造一个Transformer

```java
class DummyTransformer extends Transformer {
    
    // if you wanna process with other transformers, you can create a getter to access the field
    private final BooleanValue dummy_setting = new BooleanValue("dummy_setting", true);

    public DummyTransformer(String name) {
        super(name, /* 如果你想让他强制开启，即当 config 文件中不包含该变压器的参数块时也启用该变压器时设为true */ false);
        addSettings(dummy_setting);
    }
    
    @Override
    public void transform() {
        // TODO
    }
    
    @Override
    public void preprocess() {
        // TODO
    }

}

```

然后在 `TransformerManager` 中注册你的变压器

```java

    public TransformerRegister() {
        this.register(new StringObfuscation("string_obfuscation"));
        this.register(new NativeObfuscation("native_obfuscation"));
        
        /* ... */
        
        this.register(new DummyTransformer("dummy_transformer"));
    }

```

此时就可以在 `config.json` 中配置你的变压器了

```json
{
  "dummy_transformer": {
    "dummy_setting": true,
    "filter": [
      "+top/fl0wowp4rty/phantomshield/**"
    ]
  }
}
```

当然你必须为 `ConfigBuilder` 添加你变压器的配置代码
您可以直接跳转到 `ConfigBuilder` 查看注释