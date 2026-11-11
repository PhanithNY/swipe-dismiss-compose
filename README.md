# Swipe Dismiss for Compose

Present Compose content over an existing screen. The host handles rightward drags, Android's Back gesture, and the animation that completes or cancels a swipe. The caller keeps the presented item in its own state and clears it when dismissal completes.

## Install from a release

Download `swipe-dismiss-compose-0.1.0-maven.zip` from the [v0.1.0 release](https://github.com/PhanithNY/swipe-dismiss-compose/releases/tag/v0.1.0) and extract it. Add the extracted `maven` folder as a repository in your app's `settings.gradle.kts`:

```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven {
            url = uri("path/to/maven")
            content {
                includeModule("com.suykorng", "swipe-dismiss-compose")
            }
        }
    }
}
```

Add the dependency to your Compose module's `build.gradle.kts`:

```kotlin
implementation("com.suykorng:swipe-dismiss-compose:0.1.0")
```

## Use the source checkout

Clone this repository alongside your app. Add the following to the app's `settings.gradle.kts` and use the same dependency declaration:

```kotlin
includeBuild("../swipe-dismiss-compose")
```

## Present a screen

```kotlin
SwipeDismissHost(
    presentation = selectedItem,
    onDismissRequest = { selectedItem = null },
    motion = SwipeDismissMotion(
        enter = fadeIn() + scaleIn(initialScale = 0.96f),
        exit = fadeOut() + scaleOut(targetScale = 0.96f),
        swipeTransform = { progress, direction ->
            translationX = direction * size.width * progress
            alpha = 1f - progress * 0.3f
        },
    ),
    background = { ItemList(onSelect = { selectedItem = it }) },
) { item ->
    ItemDetails(item, onBack = { selectedItem = null })
}
```

The example uses Compose's animation functions and the library's `SwipeDismissHost` and `SwipeDismissMotion` from `com.suykorng.swipedismiss`. Use `settleAnimation` to replace the default spring. The swipe transform starts at progress `0` with the content fully visible and ends at `1` with the content offscreen or transparent. A completed swipe already supplies its exit motion. The `exit` transition is used when the caller clears the item directly or when system Back has no gesture progress.

Use `handleSystemBack = false` when the surrounding navigation handles Back. The host's Back handler is active only while an item is presented and gestures are enabled. Follow the [Android Predictive Back setup](https://developer.android.com/develop/ui/compose/system/predictive-back-setup) in the consuming app.

The library's minimum SDK is API 21. Rightward drags and spring animations are in-app features. Android 12+ can supply display corner radii, with square corners as the fallback. Android 14+ can supply native Back gesture progress. System Back on earlier versions uses the regular exit transition.

## Build locally

Use JDK 17 and Android SDK Platform 36. Point `sdk.dir` in `local.properties` or `ANDROID_HOME` at your Android SDK. Run `./gradlew lintRelease publishReleasePublicationToBuildRepository` to generate the AAR, sources, and Maven metadata in `build/repository`.
