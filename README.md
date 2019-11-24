| README.md |
|:---|

# click-debounce

[![](https://jitpack.io/v/SmartDengg/asm-clickdebounce.svg)](https://jitpack.io/#SmartDengg/asm-clickdebounce)

**Support incremental compilation! Support parallel compilation! Faster compilation speed, and shorter compilation time.**

It is a gradle plugin that uses bytecode weaving technology to solve the click jitter problem of Android applications.

For example, a normal `onClick` method without any debounce plan, multiple quick clicks may trigger multiple Activity starts:

```java
  @Override public void onClick(View v) {
    startActivity(new Intent(MainActivity.this, SecondActivity.class));
  }
```

modify the bytecode at compile time to:

```java
  @Debounced
  public void onClick(View var1) {
    if (DebouncedPredictor.shouldDoClick(var1)) {
      startActivity(new Intent(this, SecondActivity.class));
    }
  }
```

 The `@Debounced` annotation indicates that the method has been debounced. The `shouldDoClick(View)` method will determine which are the jitters and which are the correct clicks.

I also wrote a **[BLOG](https://www.jianshu.com/p/28751130c038)** to share my ideas to solve the click jitter.

*Note: This repository is just a gradle plugin, responsible for bytecode weaving work. Android runtime library please move [here](https://github.com/SmartDengg/asm-clickdebounce-runtime).*


## Requirements

- JDK 1.7 +
- Gradle 3.0.0 +

## To build

```bash

$ git clone git@github.com:SmartDengg/asm-clickdebounce.git
$ cd asm-clickdebounce/
$ ./gradlew build

```

## Getting Started

**Step 1**. Add the JitPack repository and the plugin to your buildscript:

```groovy

buildscript {

    repositories {
        ...
        maven { url 'https://jitpack.io' }
    }
    dependencies {
        ...
        classpath 'com.github.SmartDengg.asm-clickdebounce:click-debounce-gradle-plugin:1.2.0'
    }
}

```

**Step 2**. Apply it in your module:

Supports `com.android.application`, `com.android.library` and `com.android.feature`.

```groovy

apply plugin: 'smartdengg.clickdebounce'
// or apply plugin: 'clickdebounce'

```

**Step 3 (Optional)**. By adding the following code to your `build.gradle` to enable printe the beautiful log or add an exclusive list to indicate which methods do not need to be debounced. By default, the log is not printed, and process all the methods in the [support](#jump) list.

**It is not recommended to manually add @Debounce annotations to methods, you should use the **exclusive** feature that which methods should not be debounced, as follows:**

```groovy

debounce {
  // enable log
  loggable = true
  // java bytecode descriptor: [class: [methods]]
  exclusion = ["com/smartdengg/clickdebounce/ClickProxy": ['onClick(Landroid/view/View;)V',
                                                           'onItemClick(Landroid/widget/AdapterView;Landroid/view/View;IJ)V']]
}

```


**Step 4 (Optional)**. Set the debounce window time in your Java code(default is 300 milliseconds):

```java

DebouncedPredictor.FROZEN_WINDOW_MILLIS = 400L

```

## Artifact

We output some log files to help developers better understand the build information.
These file path is located in **buildDir/outputs/debounce/logs/<variant>/**, as follows:


```
.
+-- app (apply this AGP)
|   +-- build
|       +-- generated
|       +-- intermediates
|       +-- outputs
|           +-- debounce
|               +-- logs
|                   +-- debug
|                       +-- files.txt
|                       +-- classes.txt
+-- build.gradle
+-- gradle.properties
+-- gradlew
+-- gradlew.bat
+-- settings.gradle

```

- **files.txt** ：Record the class files consumed by this build，it can help you better understand this build.
- **classes.txt** ：Record information about the classes and methods woven in this build.


## How it works

**Will not intercept the touch event delivery, only intercepted in the callback of the click event, so that it will not be passed to the business logic.**

![](art/clickdebounce.png)

## <span id="jump">Support</span>

- [x] [View.OnClickListener](https://developer.android.com/reference/android/view/View.OnClickListener)
- [x] [AdapterView.OnItemClickListener](https://developer.android.com/reference/android/widget/AdapterView.OnItemClickListener)

## R8 / ProGuard (Not Required)

Pure bytecode weaving without any reflections. No Proguard rules are required.

## Bugs and Feedback

For bugs, feature requests, and discussion please use [GitHub Issues](https://github.com/SmartDengg/asm-clickdebounce/issues). Or send email to hi4joker@gmail.com.


## Found this project useful

<p align="center">:heart: Hope this article can help you. Support by clicking the :star:, or share it with people around you. :heart:  </p>


## About me

email : hi4joker@gmail.com

blog  : [小鄧子](https://www.jianshu.com/u/df40282480b4)

weibo : [-小鄧子-](https://weibo.com/5367097592/profile?topnav=1&wvr=6)


## License

See the [LICENSE](LICENSE) file for license rights and limitations (MIT).
