#先清理缓存
#version=$1
version=3.0.3.1
echo "start clean cache"
./gradlew clean
echo "clean cache complete"
echo -e "\n"
echo -e "\n"
#开始编译apk demo文件
./gradlew assemble
echo "copy demo.apk"
cp ./app/build/outputs/apk/debug/app-debug.apk ../SDKDemo.apk
echo "clean build"
./gradlew clean
echo "zip file"
7z a -tzip ../../IRtekSDK_Android_V${version}.zip ../../demo ../../doc ../../library ../../README.md
echo "zip over"


