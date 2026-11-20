# IRtekUVCSDK Android SDK

本文档包用于 Android 应用集成 IRtek UVC 热像设备。发布包包含两个 AAR：
`IRtekUVCSDK.aar` 负责 UVC 设备搜索、USB 登录、帧回调和设备参数控制；
`IRtekFileSDK.aar` 负责原始帧成像和温度计算。

## 目录说明

```text
IRtekUVCSDK_Android/
├── library/
│   ├── aar/
│   │   ├── IRtekUVCSDK.aar
│   │   └── IRtekFileSDK.aar
│   └── header/
│       ├── IRtekUVCSDK.h
│       └── IRtekFileSDK.h
├── docs/
│   ├── 01_快速开始_Quick Start.md
│   ├── 02_接口说明_API Reference.md
│   ├── 03_Demo说明_Demo Guide.md
│   └── 04_常见问题_FAQ.md
└── demo/
    └── IRtekUVCSDKDemo/
```

## 推荐阅读顺序

1. 阅读 `docs/01_快速开始_Quick Start.md` 完成最小集成。
2. 阅读 `docs/02_接口说明_API Reference.md` 理解 UVC 与 IRtekFileSDK 的职责边界。
3. 打开 `demo/IRtekUVCSDKDemo` 运行示例工程。
4. 遇到授权、ABI 或转图测温问题时查看 `docs/04_常见问题_FAQ.md`。

---

# IRtekUVCSDK Android SDK

This documentation package is for integrating IRtek UVC thermal imaging devices into Android applications. The release package includes two AARs:
`IRtekUVCSDK.aar` handles UVC device discovery, USB login, frame callbacks, and device parameter control;
`IRtekFileSDK.aar` handles raw-frame imaging and temperature calculation.

## Directory Overview

```text
IRtekUVCSDK_Android/
├── library/
│   ├── aar/
│   │   ├── IRtekUVCSDK.aar
│   │   └── IRtekFileSDK.aar
│   └── header/
│       ├── IRtekUVCSDK.h
│       └── IRtekFileSDK.h
├── docs/
│   ├── 01_快速开始_Quick Start.md
│   ├── 02_接口说明_API Reference.md
│   ├── 03_Demo说明_Demo Guide.md
│   └── 04_常见问题_FAQ.md
└── demo/
    └── IRtekUVCSDKDemo/
```

## Recommended Reading Order

1. Read `docs/01_快速开始_Quick Start.md` to complete the minimum integration.
2. Read `docs/02_接口说明_API Reference.md` to understand the responsibility boundary between UVC and IRtekFileSDK.
3. Open `demo/IRtekUVCSDKDemo` and run the sample project.
4. If you encounter authorization, ABI, or image conversion / temperature measurement issues, see `docs/04_常见问题_FAQ.md`.
