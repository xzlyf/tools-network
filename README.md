# tools-network 通用网络请求框架

**当前版本**
[![](https://jitpack.io/v/xzlyf/tools-network.svg)](https://jitpack.io/#xzlyf/tools-network)



``` gradle
//需要将java版本是指为java 1.8
android{
	compileOptions {
		sourceCompatibility JavaVersion.VERSION_1_8
		targetCompatibility JavaVersion.VERSION_1_8
	}
}


dependencies{
	//主要依赖
	implementation 'com.github.xzlyf:tools-network:VERSION_CODE'


	//可选依赖
	implementation 'com.squareup.okhttp3:okhttp:3.14.4'
	implementation 'com.google.code.gson:gson:2.8.7'
}

```

