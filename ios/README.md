# RN水印相机组件使用指南

### iOS 配置说明

1. 为了达到拍照禁止横屏效果，需加入react-native-orientation路径到组件配置项，步骤如下：
imageCropPicker.xcodeproj -> Build Settings -> Header Search Paths添加`"$(SRCROOT)/../../react-native-orientation/iOS/RCTOrientation"`, `non-recursive`改成`recursive`


