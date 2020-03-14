[![](https://jitpack.io/v/DiachukVlad/BottomSheet.svg)](https://jitpack.io/#DiachukVlad/BottomSheet) [![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
# BottomSheet Library 
It\`s an Android library writed in **[Kotlin](https://github.com/JetBrains/kotlin)** that allows you to add BottomSheet layout to your Android project quiqly and easy.
 
![hi](https://diachukvlad.github.io/files/BottomSheet.gif)

### Gradle / Maven dependency 
At the moment we do not have a publishing mechanism to a maven repository so the easiest way to add the library to your app is via a JitPack Dependency 
[![](https://jitpack.io/v/DiachukVlad/BottomSheet.svg)](https://jitpack.io/#DiachukVlad/BottomSheet)

```gradle
repositories {
    ...
    maven { url "https://jitpack.io" }
}
dependencies {
    implementation 'com.github.DiachukVlad:BottomSheet:[version]'
}
```

### How to use
* In your layout write
```xml
 <vladiachuk.com.bottomsheet.BottomSheet
        android:id="@+id/bottomSheet"
        android:layout_width="match_parent"
        android:layout_height="match_parent"
        app:layout="@layout/layout"
        app:peekHeight="16dp"/>
```

 
* In activity you must set controller of BottomSheet. Another thing that you must do is to set all possible states and set graph of transitions of states in way that is presented below.

```kotlin
override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)

    bottomSheet.controller = BottomSheetController(bottomSheet)
    
    bottomSheet.controller?.run {
        possibleStates = arrayListOf(COLLAPSED_STATE, HALF_EXPANDED_STATE, EXPANDED_STATE)
        statesGraph = arrayListOf(
            arrayOf(COLLAPSED_STATE, HALF_EXPANDED_STATE),
            arrayOf(HALF_EXPANDED_STATE, COLLAPSED_STATE),
            arrayOf(EXPANDED_STATE, COLLAPSED_STATE)
        )
    }
}
```

* To set state simply write
```kotlin
bottomSheet.controller?.run { state = COLLAPSED_STATE }
```

* To set state with animation
```kotlin
bottomSheet.controller?.run { setStateAnim(COLLAPSED_STATE)}
//or you can set custom duration
bottomSheet.controller?.run { setStateAnim(COLLAPSED_STATE, 1500)}
```

* You can use Kotlin Coroutines
```kotlin
//very important to do it using Main dispatcher
GlobalScope.launch(Dispatchers.Main) {
    bottomSheet.controller?.run { setStateAnimSuspend(COLLAPSED_STATE)}
}
```

* Another cool thing is BottomSheet friendly layouts. If you want E.g. drag bottom sheet when you move finger on BottomNavigationView you can put BottomNavigationView inside the BottomNavigationView. **It\`s important to set app:bottom_sheet_id attribute.**
```xml
<vladiachuk.com.bottomsheet.friendlyLayouts.BSFriendlyFrameLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        app:bottom_sheet_id="@id/bottomSheet">
        <com.google.android.material.bottomnavigation.BottomNavigationView
            android:id="@+id/bottom_navigation"
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:background="@color/colorPrimary"
            app:itemIconTint="@color/white"
            app:itemTextColor="@color/white"
            app:menu="@menu/bottom_navigation_menu"/>
    </vladiachuk.com.bottomsheet.friendlyLayouts.BSFriendlyFrameLayout>
```

* Other possibilities of BottomSheet library you can see in app module in repository

##  License
BottomSheet Android Library is available under MIT license. See [LICENSE](https://github.com/DiachukVlad/BottomSheet/blob/master/LICENSE) with the full license text. 

## Compatibility
BottomSheet Android library is valid for Android version 4.4 and up (with ```android:minSdkVersion="19"``` and ```android:targetSdkVersion="29"```).
