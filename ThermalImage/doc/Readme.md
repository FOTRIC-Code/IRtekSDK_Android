# 使用说明

## 一 引入相关库

### 1.AAR/JAR 引入

下载aar /jar 包

放入module 下面的libs 文件夹，在build.gradlew 引入

```java
implementation fileTree(includes: ['*.jar', '*.aar'], dir: 'libs')

```

## 二 使用接口文档

在当前目录下 doc 有完整的接口说明 打开doc/index.html

