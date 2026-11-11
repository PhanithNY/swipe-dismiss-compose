# Swipe Dismiss for Compose

Add swipe-to-dismiss to a Jetpack Compose screen. The foreground follows a rightward drag to reveal the previous screen. Release a short, slow drag to snap back, or finish the swipe to dismiss. You can also customize the opening transition, the regular exit, the swipe transform, and the settling animation.

## Install from a release

1. Download and extract the [v0.1.0 Maven ZIP](https://github.com/PhanithNY/swipe-dismiss-compose/releases/download/v0.1.0/swipe-dismiss-compose-0.1.0-maven.zip).
2. Add the extracted `maven` folder to the existing repositories block in your project's `settings.gradle.kts`.

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

3. Add the dependency to your Compose module's `build.gradle.kts`.

   ```kotlin
   dependencies {
       implementation("com.suykorng:swipe-dismiss-compose:0.1.0")
   }
   ```

## Show and dismiss a screen

Call `SwipeDismissDemo()` from an existing Compose activity or screen. This example uses Compose Foundation for its text and tap targets. Add `implementation("androidx.compose.foundation:foundation")` if your module does not already have it on its compile classpath.

```kotlin
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.suykorng.swipedismiss.SwipeDismissHost

@Composable
fun SwipeDismissDemo() {
    var selectedItem by rememberSaveable { mutableStateOf<String?>(null) }

    SwipeDismissHost(
        presentation = selectedItem,
        onDismissRequest = { selectedItem = null },
        background = {
            Box(
                modifier = Modifier.fillMaxSize().background(Color.White),
                contentAlignment = Alignment.Center,
            ) {
                BasicText(
                    text = "Open details",
                    modifier = Modifier
                        .clickable(role = Role.Button) { selectedItem = "Item details" }
                        .padding(16.dp),
                )
            }
        },
    ) { item ->
        Box(
            modifier = Modifier.fillMaxSize().background(Color.White),
            contentAlignment = Alignment.Center,
        ) {
            BasicText(text = item)
            BasicText(
                text = "Back",
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .clickable(role = Role.Button) { selectedItem = null }
                    .padding(16.dp),
            )
        }
    }
}
```

`presentation` is your app's state. Set it to a stable item to open the foreground and clear it in `onDismissRequest`. The host keeps the background behind the foreground so it can reveal it during the swipe. The in-app swipe is enabled by default. Use `allowRightSwipe = false` for screens that already need horizontal gestures.

## Customize the motion

Pass a `SwipeDismissMotion` through the host's `motion` parameter. These options can be set independently.

| Option | Controls |
| --- | --- |
| `enter` | How the foreground appears. |
| `exit` | How it closes when you clear the item or system Back has no swipe progress. |
| `swipeTransform` | How the foreground moves during the swipe. Progress goes from `0` when fully visible to `1` when dismissed. Direction is `+1` for right and `-1` for left. |
| `settleAnimation` | How a swipe finishes or snaps back on release. The default is a spring. |

For example, use `enter = fadeIn() + scaleIn(initialScale = 0.96f)` for a slight scale-in. A custom `swipeTransform` can change translation, scale, alpha, or shape. Make sure its final frame takes the foreground offscreen or makes it transparent. A completed swipe supplies its own exit animation.

## Android compatibility

| Feature | Support |
| --- | --- |
| In-app rightward swipe and spring | Android 5.0+ (API 21) |
| Device-matched corners | Android 12+ (API 31) when Android reports corner radii. Otherwise the default swipe uses square corners. |
| Native interactive Back | Android 14+ (API 34) when gesture navigation and Predictive Back are available. Earlier versions use the regular exit for system Back. |

Set `handleSystemBack = false` when the surrounding navigation handles Back. Set `gesturesEnabled = false` to pause the host's gestures while a dialog covers it. For native progress, follow the [Predictive Back setup](https://developer.android.com/develop/ui/compose/system/predictive-back-setup) in your app.

## Use the source checkout

Clone the repo alongside your app. Add `includeBuild("../swipe-dismiss-compose")` to your app's `settings.gradle.kts` and use the same dependency declaration.

To build the library itself, use JDK 17 and Android SDK Platform 36. Point `sdk.dir` in `local.properties` or `ANDROID_HOME` at your Android SDK. Then run:

```sh
./gradlew lintRelease publishReleasePublicationToBuildRepository
```

The AAR, sources, and Maven metadata are written to `build/repository`.

## Agent skill

The [Swipe Dismiss Compose skill](skills/swipe-dismiss-compose/SKILL.md) guides agents through dependency setup, navigation ownership, motion customization, and verification. For Codex, copy the `skills/swipe-dismiss-compose` folder into your Codex skills directory or ask Codex to install it from this repository.

## License

The library source, documentation, and release binaries are available under the [MIT License](LICENSE). The Gradle Wrapper files retain their original Apache 2.0 license.
