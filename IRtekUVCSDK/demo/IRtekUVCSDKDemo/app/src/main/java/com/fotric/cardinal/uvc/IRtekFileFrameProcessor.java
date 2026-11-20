package com.fotric.cardinal.uvc;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;

import com.fotric.holley.uvc.ImageParam;
import com.fotric.holley.uvc.LutTable;
import com.fotric.holley.uvc.ThermalParam;
import com.fotric.holley.uvc.UVCFrameRawData;

import java.io.File;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Uses IRtekFileSDK to convert UVC radiometric frames into thermal images and temperature values.
 *
 * <p>The demo calls IRtekFileSDK by reflection so the source can still open when the AAR is not
 * present. Runtime conversion requires IRtekFileSDK.aar and its native library.</p>
 */
public final class IRtekFileFrameProcessor {
    private static final int IR_FILE_THERMAL_PICTURE = 1;
    private static final int IR_FILE_RGBA = 2;
    private static final int IR_FILE_SPOT = 1;
    private static final int OK = 0;
    private static final String CENTER_MARKER_NAME = "CENTER";

    private final Context context;

    private Object irFile;
    private Class<?> irFileClass;
    private int thermalWidth;
    private int thermalHeight;
    private short imageFlipMode;
    private String setupError;
    private boolean useChinese;

    public IRtekFileFrameProcessor(Context context) {
        this.context = context.getApplicationContext();
    }

    public synchronized void setUseChinese(boolean useChinese) {
        this.useChinese = useChinese;
    }

    private String t(String english, String chinese) {
        return useChinese ? chinese : english;
    }

    public synchronized RenderResult configure(
            int width,
            int height,
            ThermalParam thermalParam,
            LutTable lutTable
    ) {
        thermalWidth = width;
        thermalHeight = height > 4 ? height - 4 : height;
        setupError = null;

        try {
            irFileClass = Class.forName("com.fotric.irtekfilesdk.IRtekFile");
            File file = new File(context.getCacheDir(), "uvc_preview_context.fjpg");
            Object created = invokeStatic(
                    irFileClass,
                    "createIrtekFile",
                    new Class<?>[]{String.class, int.class},
                    file.getAbsolutePath(),
                    IR_FILE_THERMAL_PICTURE
            );
            if (created == null) {
                return fail(t("IRtekFileSDK failed to create thermal context", "IRtekFileSDK 创建热像上下文失败"));
            }
            irFile = created;
            setThermalSize(thermalWidth, thermalHeight);
            applyThermalParams(thermalParam);
            setLut(lutTable);
            return RenderResult.message(t("IRtekFileSDK initialized: ", "IRtekFileSDK 已初始化: ") + thermalWidth + "x" + thermalHeight);
        } catch (ClassNotFoundException e) {
            return fail(t("IRtekFileSDK not found. Add IRtekFileSDK.aar to the demo dependencies", "未找到 IRtekFileSDK，请将 IRtekFileSDK.aar 加入 demo 依赖"));
        } catch (UnsatisfiedLinkError e) {
            return fail(t("IRtekFileSDK native library failed to load: ", "IRtekFileSDK native 库加载失败: ") + e.getMessage());
        } catch (Throwable e) {
            return fail(t("IRtekFileSDK initialization failed: ", "IRtekFileSDK 初始化失败: ") + e.getMessage());
        }
    }

    public synchronized RenderResult applyThermalParams(ThermalParam value) {
        if (value == null) return RenderResult.message(t("Device did not return thermal parameters", "设备未返回热像测温参数"));
        if (irFile == null) return fail(t("IRtekFileSDK is not initialized", "IRtekFileSDK 未初始化，无法写入测温参数"));
        try {
            setThermalParams(value);
            return RenderResult.message(String.format(
                    java.util.Locale.US,
                    t("Synced thermal params to FileSDK: emissivity=%.2f, distance=%.2f, refl=%.2f, atm=%.2f",
                            "已同步测温参数到 FileSDK: emissivity=%.2f, distance=%.2f, refl=%.2f, atm=%.2f"),
                    value.getEmissivity(),
                    value.getDistance(),
                    value.getReflTemp(),
                    value.getAtmTemp()
            ));
        } catch (Throwable e) {
            return fail(t("Failed to sync thermal params to FileSDK: ", "同步测温参数到 FileSDK 失败: ") + e.getMessage());
        }
    }

    public synchronized RenderResult applyImageParams(ImageParam value) {
        if (value == null) return RenderResult.message(t("Device did not return image parameters", "设备未返回图像参数"));
        if (irFile == null) return fail(t("IRtekFileSDK is not initialized", "IRtekFileSDK 未初始化，无法写入图像参数"));
        try {
            Object imageParams = newInstance("com.fotric.irtekfilesdk.IrFileImageParams");
            setField(imageParams, "contrast", value.getContrast());
            setField(imageParams, "brightness", value.getBrightness());
            setField(imageParams, "ddeEnabled", value.getDdeEnable());
            setField(imageParams, "ddeValue", value.getDdeValue());

            Object vtParams = newInstance("com.fotric.irtekfilesdk.IrFileVtParamsV2");
            setField(vtParams, "ispType", 1);
            setField(vtParams, "imageParams", imageParams);

            invoke(irFile, "setIspType", new Class<?>[]{int.class}, 1);
            Object result = invoke(irFile, "setVtParamsV2", new Class<?>[]{vtParams.getClass()}, vtParams);
            assertOk(result, "setVtParamsV2");

            return RenderResult.message(t("Synced image params to FileSDK: contrast=", "已同步图像参数到 FileSDK: contrast=") + value.getContrast()
                    + ", brightness=" + value.getBrightness()
                    + ", ddeEnable=" + value.getDdeEnable()
                    + ", ddeValue=" + value.getDdeValue());
        } catch (Throwable e) {
            return fail(t("Failed to sync image params to FileSDK: ", "同步图像参数到 FileSDK 失败: ") + e.getMessage());
        }
    }

    public synchronized RenderResult applyImageFlipMode(short flipMode) {
        imageFlipMode = flipMode;
        return RenderResult.message(t("Applied image flip to preview: flipMode=", "已同步图像翻转模式到上层预览: flipMode=") + flipMode);
    }

    public synchronized String getFileSdkVersion() {
        try {
            Class<?> cls = Class.forName("com.fotric.irtekfilesdk.IRtekFile");
            File file = new File(context.getCacheDir(), "uvc_version_context.fjpg");
            Object created = invokeStatic(
                    cls,
                    "createIrtekFile",
                    new Class<?>[]{String.class, int.class},
                    file.getAbsolutePath(),
                    IR_FILE_THERMAL_PICTURE
            );
            if (created == null) return null;
            Object version = invoke(created, "getSDKVersion", new Class<?>[]{});
            int errorCode = getIntField(version, "errorCode", -1);
            if (errorCode != OK) return null;
            byte[] buffer = (byte[]) getField(version, "buffer");
            String text = decodeCString(buffer);
            if (!text.isEmpty()) return text;
            int raw = getIntField(version, "version", 0);
            return raw == 0 ? null : String.valueOf(raw);
        } catch (Throwable ignored) {
            return null;
        }
    }

    public synchronized PaletteResult loadPresetPalettes() {
        if (irFile == null) return PaletteResult.failure(t("IRtekFileSDK is not initialized", "IRtekFileSDK 未初始化，无法读取预置调色板"));
        try {
            Object result = invoke(irFile, "getPresetPalettes", new Class<?>[]{});
            int errorCode = getIntField(result, "errorCode", -1);
            if (errorCode != OK) {
                return PaletteResult.failure("getPresetPalettes failed: errorCode=" + errorCode);
            }

            int count = getIntField(result, "count", 0);
            Object palettes = getField(result, "presetPalettes");
            List<PaletteOption> options = new ArrayList<>();
            if (palettes != null) {
                int length = Math.min(count, Array.getLength(palettes));
                for (int i = 0; i < length; i++) {
                    Object palette = Array.get(palettes, i);
                    byte refNo = getByteField(palette, "refNo", (byte) i);
                    byte[] nameBytes = (byte[]) getField(palette, "name");
                    String name = decodeCString(nameBytes);
                    options.add(new PaletteOption(refNo, name.isEmpty() ? "Palette " + refNo : name));
                }
            }
            return PaletteResult.success(options, t("Loaded preset palettes: count=", "读取预置调色板成功: count=") + options.size());
        } catch (Throwable e) {
            return PaletteResult.failure(t("Failed to load preset palettes: ", "读取预置调色板失败: ") + e.getMessage());
        }
    }

    public synchronized RenderResult applyPalette(byte refNo, boolean inverted) {
        if (irFile == null) return fail(t("IRtekFileSDK is not initialized", "IRtekFileSDK 未初始化，无法设置调色板"));
        try {
            Object palette = newInstance("com.fotric.irtekfilesdk.IrFilePalette");
            setField(palette, "refNo", refNo);
            setField(palette, "colors", null);
            setField(palette, "inverted", (byte) (inverted ? 1 : 0));
            Object result = invoke(irFile, "setPalette", new Class<?>[]{palette.getClass()}, palette);
            assertOk(result, "setPalette");
            return RenderResult.message(t("Palette applied: refNo=", "已应用调色板: refNo=") + refNo + ", inverted=" + inverted);
        } catch (Throwable e) {
            return fail(t("Failed to apply palette: ", "应用调色板失败: ") + e.getMessage());
        }
    }

    /** Feeds the frame to FileSDK only to refresh the center-spot temperature. The preview bitmap comes from UVC. */
    public synchronized RenderResult measureCenter(UVCFrameRawData frame) {
        if (setupError != null) return fail(setupError);
        if (irFile == null) return fail(t("IRtekFileSDK is not initialized", "IRtekFileSDK 未初始化"));
        if (frame == null || frame.getData() == null) return fail(t("Empty frame", "空帧"));

        int width = frame.getWidth();
        int height = frame.getHeight();
        if (width <= 0 || height <= 4) return fail(t("Invalid frame size: ", "非法帧尺寸: ") + width + "x" + height);
        if (width != thermalWidth || height != thermalHeight) {
            return fail(t("Frame size does not match IRtekFileSDK context: frame=",
                    "帧尺寸与 IRtekFileSDK 上下文不一致: frame=")
                    + width + "x" + height + ", context=" + thermalWidth + "x" + thermalHeight);
        }

        try {
            int lutCount = updateFrameLut(frame);
            Object irFrame = newInstance("com.fotric.irtekfilesdk.IrFileFrame");
            short[] adFrame = byteArrayToShortArray(frame.getData(), width * height);
            setField(irFrame, "timeStamp", System.currentTimeMillis());
            setField(irFrame, "length", adFrame.length);
            setField(irFrame, "buffer", adFrame);
            Object image = invoke(
                    irFile,
                    "getThermalImageFromFrame",
                    new Class<?>[]{irFrame.getClass(), int.class, int.class},
                    irFrame,
                    IR_FILE_RGBA,
                    0
            );
            int imageErrorCode = getIntField(image, "errorCode", -1);
            if (imageErrorCode != OK) {
                return fail("getThermalImageFromFrame failed: errorCode=" + imageErrorCode);
            }

            int imageHeight = height - 4;
            MarkerResult centerMarker = updateAndReadCenterMarker(width, imageHeight);
            String lutText = lutCount > 0
                    ? t("in-stream LUT=", "流内 LUT=") + lutCount
                    : t("this frame has no LUT", "本帧未携带 LUT");
            Float centerTemp = centerMarker.temp;
            String message = centerTemp == null
                    ? t("CENTER marker temperature failed: ", "CENTER marker 温度读取失败: ") + centerMarker.message + ", " + lutText
                    : String.format(java.util.Locale.US,
                    t("CENTER marker %.2f °C, %s", "CENTER marker %.2f °C，%s"),
                    centerTemp, lutText);
            return RenderResult.success(null, centerTemp, message);
        } catch (Throwable e) {
            return fail(t("IRtekFileSDK failed to process frame: ", "IRtekFileSDK 处理帧失败: ") + e.getMessage());
        }
    }

    public synchronized RenderResult render(UVCFrameRawData frame) {
        if (setupError != null) return fail(setupError);
        if (irFile == null) return fail(t("IRtekFileSDK is not initialized", "IRtekFileSDK 未初始化"));
        if (frame == null || frame.getData() == null) return fail(t("Empty frame", "空帧"));

        int width = frame.getWidth();
        int height = frame.getHeight();
        if (width <= 0 || height <= 0) return fail(t("Invalid frame size: ", "非法帧尺寸: ") + width + "x" + height);
        if (width != thermalWidth || height != thermalHeight) {
            return fail(t("Frame size does not match IRtekFileSDK context: frame=",
                    "帧尺寸与 IRtekFileSDK 上下文不一致: frame=")
                    + width + "x" + height + ", context=" + thermalWidth + "x" + thermalHeight);
        }

        try {
            int lutCount = updateFrameLut(frame);

            Object irFrame = newInstance("com.fotric.irtekfilesdk.IrFileFrame");
            short[] adFrame = byteArrayToShortArray(frame.getData(), width * height);
            setField(irFrame, "timeStamp", System.currentTimeMillis());
            setField(irFrame, "length", adFrame.length);
            setField(irFrame, "buffer", adFrame);

            Object image = invoke(
                    irFile,
                    "getThermalImageFromFrame",
                    new Class<?>[]{irFrame.getClass(), int.class, int.class},
                    irFrame,
                    IR_FILE_RGBA,
                    0
            );
            int imageErrorCode = getIntField(image, "errorCode", -1);
            if (imageErrorCode != OK) {
                return fail("getThermalImageFromFrame failed: errorCode=" + imageErrorCode);
            }

            byte[] rgba = (byte[]) getField(image, "data");
            MarkerResult centerMarker = updateAndReadCenterMarker(width, height);
            Bitmap rawBitmap = rgbaToBitmap(rgba, width, height);
            if (rawBitmap == null) {
                return fail(t("Failed to convert RGBA to Bitmap", "RGBA 转 Bitmap 失败"));
            }
            Bitmap bitmap = applyFlip(drawCenterMarker(
                    rawBitmap,
                    rawBitmap.getWidth(),
                    rawBitmap.getHeight()
            ));
            Float centerTemp = centerMarker.temp;
            String lutText = lutCount > 0
                    ? t("in-stream LUT=", "流内 LUT=") + lutCount
                    : t("this frame has no LUT", "本帧未携带 LUT");
            String message = centerTemp == null
                    ? t("Thermal image generated, CENTER marker temperature failed: ",
                    "热像图已生成，CENTER marker 温度读取失败: ") + centerMarker.message + ", " + lutText
                    : String.format(java.util.Locale.US,
                    t("Thermal image generated, CENTER marker %.2f °C, %s",
                            "热像图已生成，CENTER marker %.2f °C，%s"),
                    centerTemp, lutText);
            return RenderResult.success(bitmap, centerTemp, message);
        } catch (Throwable e) {
            return fail(t("IRtekFileSDK failed to process frame: ", "IRtekFileSDK 处理帧失败: ") + e.getMessage());
        }
    }

    private void setThermalSize(int width, int height) throws Exception {
        Object size = newInstance("com.fotric.irtekfilesdk.IrFileSize");
        setField(size, "width", (short) width);
        setField(size, "height", (short) height);
        Object result = invoke(irFile, "setThermalSize", new Class<?>[]{size.getClass()}, size);
        assertOk(result, "setThermalSize");
    }

    private void setThermalParams(ThermalParam value) throws Exception {
        if (value == null) return;
        Object params = newInstance("com.fotric.irtekfilesdk.IrFileThermalParams");
        setField(params, "reflTemp", toKelvin(value.getReflTemp()));
        setField(params, "atmTemp", toKelvin(value.getAtmTemp()));
        setField(params, "distance", value.getDistance());
        setField(params, "relHumidity", value.getRelHumidity());
        setField(params, "emissivity", value.getEmissivity());
        setField(params, "extOptTemp", toKelvin(value.getAtmTemp()));
        setField(params, "extOptTrans", 1.0f);
        Object result = invoke(irFile, "setThermalParams", new Class<?>[]{params.getClass()}, params);
        assertOk(result, "setThermalParams");
    }

    private void setLut(LutTable value) throws Exception {
        if (value == null) return;
        short[] ad = value.getAd();
        short[] t = value.getT();
        int count = Math.min(value.getAdLength(), value.getTLength());
        count = Math.min(count, Math.min(ad == null ? 0 : ad.length, t == null ? 0 : t.length));
        if (count <= 0) return;

        float[] temps = new float[count];
        for (int i = 0; i < count; i++) {
            temps[i] = t[i];
        }
        setLut(ad, temps, count);
    }

    private int updateFrameLut(UVCFrameRawData frame) throws Exception {
        short[] ad = frame.getAdValues();
        float[] temps = frame.getTargetTemps();
        int count = Math.min(frame.getLutLength(), Math.min(ad == null ? 0 : ad.length, temps == null ? 0 : temps.length));
        if (count > 0) {
            setLut(ad, temps, count);
        }
        return count;
    }

    private void setLut(short[] ad, float[] temps, int count) throws Exception {
        Object lut = newInstance("com.fotric.irtekfilesdk.IrFileLut");
        short[] adCopy = new short[count];
        float[] tempCopy = new float[count];
        System.arraycopy(ad, 0, adCopy, 0, count);
        System.arraycopy(temps, 0, tempCopy, 0, count);
        setField(lut, "radMethod", (byte) 1);
        setField(lut, "count", count);
        setField(lut, "adArray", adCopy);
        setField(lut, "tempArray", tempCopy);
        Object result = invoke(irFile, "setLut", new Class<?>[]{lut.getClass()}, lut);
        assertOk(result, "setLut");
    }

    private MarkerResult updateAndReadCenterMarker(int width, int height) throws Exception {
        Object marker = createCenterMarker(width / 2, height / 2);
        Object setResult = invoke(irFile, "setMarker", new Class<?>[]{marker.getClass()}, marker);
        int setErrorCode = getIntField(setResult, "errorCode", -1);
        if (setErrorCode != OK) {
            return MarkerResult.failure("setMarker errorCode=" + setErrorCode);
        }

        Object readMarker = invoke(irFile, "getMarker", new Class<?>[]{String.class}, CENTER_MARKER_NAME);
        int getErrorCode = getIntField(readMarker, "errorCode", -1);
        if (getErrorCode != OK) {
            return MarkerResult.failure("getMarker errorCode=" + getErrorCode);
        }

        float[] tempValues = (float[]) getField(readMarker, "tempValues");
        if (tempValues == null || tempValues.length == 0) {
            return MarkerResult.failure(t("marker tempValues is empty", "marker tempValues 为空"));
        }
        float tempKelvin = tempValues.length >= 3 ? tempValues[2] : tempValues[0];
        if (Float.isNaN(tempKelvin)) {
            return MarkerResult.failure(t("marker temperature is NaN", "marker 温度为 NaN"));
        }
        return MarkerResult.success(toCelsius(tempKelvin));
    }

    private Object createCenterMarker(int x, int y) throws Exception {
        Object point = newInstance("com.fotric.irtekfilesdk.IrFilePoint");
        setField(point, "x", (short) x);
        setField(point, "y", (short) y);

        Class<?> pointClass = point.getClass();
        Object points = Array.newInstance(pointClass, 1);
        Array.set(points, 0, point);

        Object marker = newInstance("com.fotric.irtekfilesdk.IrFileMarker");
        setField(marker, "type", IR_FILE_SPOT);
        setField(marker, "name", markerNameBytes(CENTER_MARKER_NAME));
        setField(marker, "localParams", (byte) 0);
        setField(marker, "reflTemp", 0.0f);
        setField(marker, "distance", 0.0f);
        setField(marker, "emissivity", 0.0f);
        setField(marker, "pointCount", 1);
        setField(marker, "points", points);
        return marker;
    }

    private static byte[] markerNameBytes(String name) {
        byte[] result = new byte[32];
        byte[] source = name.getBytes(StandardCharsets.UTF_8);
        System.arraycopy(source, 0, result, 0, Math.min(source.length, result.length - 1));
        return result;
    }

    private RenderResult fail(String message) {
        return RenderResult.failure(message);
    }

    private static short[] byteArrayToShortArray(byte[] data, int pixelCount) {
        int count = Math.min(pixelCount, data.length / 2);
        short[] result = new short[count];
        for (int i = 0; i < count; i++) {
            int lo = data[i * 2] & 0xFF;
            int hi = data[i * 2 + 1] & 0xFF;
            result[i] = (short) (lo | (hi << 8));
        }
        return result;
    }

    private static Bitmap rgbaToBitmap(byte[] rgba, int width, int height) {
        if (rgba == null || rgba.length < width * height * 4) return null;
        int[] pixels = new int[width * height];
        for (int i = 0; i < pixels.length; i++) {
            int base = i * 4;
            int r = rgba[base] & 0xFF;
            int g = rgba[base + 1] & 0xFF;
            int b = rgba[base + 2] & 0xFF;
            int a = rgba[base + 3] & 0xFF;
            pixels[i] = (a << 24) | (r << 16) | (g << 8) | b;
        }
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
        return bitmap;
    }

    private Bitmap applyFlip(Bitmap bitmap) {
        if (bitmap == null || imageFlipMode == 0) return bitmap;

        Matrix matrix = new Matrix();
        switch (imageFlipMode) {
            case 1:
                matrix.setScale(1.0f, -1.0f, bitmap.getWidth() / 2.0f, bitmap.getHeight() / 2.0f);
                break;
            case 2:
                matrix.setScale(-1.0f, 1.0f, bitmap.getWidth() / 2.0f, bitmap.getHeight() / 2.0f);
                break;
            case 3:
                matrix.setScale(-1.0f, -1.0f, bitmap.getWidth() / 2.0f, bitmap.getHeight() / 2.0f);
                break;
            default:
                return bitmap;
        }
        Bitmap flipped = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, false);
        if (flipped != bitmap) {
            bitmap.recycle();
        }
        return flipped;
    }

    private static Bitmap drawCenterMarker(Bitmap bitmap, int width, int height) {
        if (bitmap == null) return null;
        Bitmap target = bitmap.isMutable() ? bitmap : bitmap.copy(Bitmap.Config.ARGB_8888, true);
        if (target == null) return bitmap;

        float cx = target.getWidth() / 2.0f;
        float cy = target.getHeight() / 2.0f;
        float radius = Math.max(8.0f, Math.min(target.getWidth(), target.getHeight()) * 0.035f);

        Canvas canvas = new Canvas(target);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(Math.max(2.0f, Math.min(target.getWidth(), target.getHeight()) / 120.0f));
        paint.setColor(Color.YELLOW);
        canvas.drawCircle(cx, cy, radius, paint);
        canvas.drawLine(cx - radius * 1.5f, cy, cx + radius * 1.5f, cy, paint);
        canvas.drawLine(cx, cy - radius * 1.5f, cx, cy + radius * 1.5f, paint);
        return target;
    }

    private static final class MarkerResult {
        final Float temp;
        final String message;

        private MarkerResult(Float temp, String message) {
            this.temp = temp;
            this.message = message;
        }

        static MarkerResult success(float temp) {
            return new MarkerResult(temp, "ok");
        }

        static MarkerResult failure(String message) {
            return new MarkerResult(null, message);
        }
    }

    private static float toKelvin(float value) {
        return value < 200.0f ? value + 273.15f : value;
    }

    private static float toCelsius(float value) {
        return value > 200.0f ? value - 273.15f : value;
    }

    private static Object newInstance(String className) throws Exception {
        return Class.forName(className).getDeclaredConstructor().newInstance();
    }

    private static Object invokeStatic(
            Class<?> cls,
            String name,
            Class<?>[] parameterTypes,
            Object... args
    ) throws Exception {
        Method method = cls.getDeclaredMethod(name, parameterTypes);
        method.setAccessible(true);
        return method.invoke(null, args);
    }

    private static Object invoke(
            Object target,
            String name,
            Class<?>[] parameterTypes,
            Object... args
    ) throws Exception {
        Method method = target.getClass().getDeclaredMethod(name, parameterTypes);
        method.setAccessible(true);
        return method.invoke(target, args);
    }

    private static void assertOk(Object result, String apiName) throws Exception {
        int errorCode = getIntField(result, "errorCode", OK);
        if (errorCode != OK) {
            throw new IllegalStateException(apiName + " failed, errorCode=" + errorCode);
        }
    }

    private static void setField(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getField(name);
        field.set(target, value);
    }

    private static Object getField(Object target, String name) throws Exception {
        Field field = target.getClass().getField(name);
        return field.get(target);
    }

    private static int getIntField(Object target, String name, int defaultValue) throws Exception {
        if (target == null) return defaultValue;
        Object value = getField(target, name);
        return value instanceof Number ? ((Number) value).intValue() : defaultValue;
    }

    private static float getFloatField(Object target, String name, float defaultValue) throws Exception {
        if (target == null) return defaultValue;
        Object value = getField(target, name);
        return value instanceof Number ? ((Number) value).floatValue() : defaultValue;
    }

    private static byte getByteField(Object target, String name, byte defaultValue) throws Exception {
        if (target == null) return defaultValue;
        Object value = getField(target, name);
        return value instanceof Number ? ((Number) value).byteValue() : defaultValue;
    }

    private static String decodeCString(byte[] bytes) {
        if (bytes == null || bytes.length == 0) return "";
        int length = 0;
        while (length < bytes.length && bytes[length] != 0) {
            length += 1;
        }
        return new String(bytes, 0, length, StandardCharsets.UTF_8).trim();
    }

    public static final class PaletteOption {
        public final byte refNo;
        public final String name;

        PaletteOption(byte refNo, String name) {
            this.refNo = refNo;
            this.name = name;
        }
    }

    public static final class PaletteResult {
        public final boolean success;
        public final List<PaletteOption> palettes;
        public final String message;

        private PaletteResult(boolean success, List<PaletteOption> palettes, String message) {
            this.success = success;
            this.palettes = palettes;
            this.message = message;
        }

        static PaletteResult success(List<PaletteOption> palettes, String message) {
            return new PaletteResult(true, palettes, message);
        }

        static PaletteResult failure(String message) {
            return new PaletteResult(false, new ArrayList<PaletteOption>(), message);
        }
    }

    public static final class RenderResult {
        public final boolean success;
        public final Bitmap bitmap;
        public final Float centerTemp;
        public final String message;

        private RenderResult(boolean success, Bitmap bitmap, Float centerTemp, String message) {
            this.success = success;
            this.bitmap = bitmap;
            this.centerTemp = centerTemp;
            this.message = message;
        }

        static RenderResult success(Bitmap bitmap, Float centerTemp, String message) {
            return new RenderResult(true, bitmap, centerTemp, message);
        }

        static RenderResult message(String message) {
            return new RenderResult(true, null, null, message);
        }

        static RenderResult failure(String message) {
            return new RenderResult(false, null, null, message);
        }
    }
}
