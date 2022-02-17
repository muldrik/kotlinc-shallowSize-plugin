# ShallowSize - Kotlin compiler plugin allowing to calculate data class total property size in compile time

The plugin is powered by [Arrow Meta](https://github.com/arrow-kt/arrow-meta) and works for Kotlin version 1.5.0 and 1.5.10 and can

## Quick start
1. Download a compiled plugin from the release tab

2.1 If you use gradle, add the following code to your `build.gradle`
```gradle
compileKotlin {
    kotlinOptions {
        jvmTarget = "$JVM_TARGET_VERSION"
        freeCompilerArgs += ["-Xplugin=<path to shallowSizePlugin.jar>"]
    }
}
```
If you use `build.gradle.kts` instead add
```gradle
tasks.compileKotlin {
    kotlinOptions {
        jvmTarget = jvmTargetVersion
        freeCompilerArgs = freeCompilerArgs + "-Xplugin=<path to shallowSizePlugin.jar>"
    }
}
```
3. Now your data classes have a member function `shallowSize()`

For example
```kotlin
data class A(val i: Int, var j: Long)
fun main() {
  val a = A(1, 100)
  println(a.shallowSize()) // 12
}
```

### Bulding from source and testing
All files needed to build the plugin from source are located in [shallowSizePlugin](https://github.com/muldrik/kotlinc-shallowSize-plugin/tree/master/shallowSizePlugin) directory (except for some environment variables that can be found in [gradle.properties](https://github.com/muldrik/kotlinc-shallowSize-plugin/blob/master/gradle.properties)

To execute tests, run

    ./gradlew test-plugin:test

### Usage notes

[Properties](https://kotlinlang.org/docs/properties.html) in Kotlin are what is declared by `val` and `var` keywords. Properties take space in bytes only if they contain a [backing field](https://kotlinlang.org/docs/properties.html#backing-fields). For example
```kotlin
data class A(val i: Int) {
  var j: Long = 20
}
```
data class `A` contains 2 properties, each with a backing field, resulting in shallowSize of 12.

##### Properties without a backing field
However, not all properties contain a backing field. Consider

```kotlin
data class Rect(val height: Int, val width: Int) {
  val area
    get() = height*width
}
```
Here, property `area` can be accessed without storing additional data. Therefore, the plugin considers its size to be zero

##### Inheriting from an open class

Inheritance can also introduce properties without backing fields
```kotlin
open class SimpleParent {
    val superClassProp = 100
}

data class SimpleInherited(val a: Long): SimpleParent()
```
Here, `SimpleInherited` class contains `superClassProp` property, however the decision was made to not account for it when calculating `SimpleInherited` shallow size. This can easily changed for future release.

Super class properties can be overriden. Then they are checked for a backing field

```kotlin
open class ParentWithOpenValProp {
  open val overridable: Int = 10
}

data class A(val i: Int) {
  override val overridable
        get() = 30
}

data class B(val i: Int) {
  override val overridable = 40
}
```
Class `A` has a shallow size of 4, while class `B` has a shallow size of 8





