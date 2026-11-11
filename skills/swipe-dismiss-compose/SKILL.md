---
name: swipe-dismiss-compose
description: Integrate or customize the PhanithNY Swipe Dismiss for Compose library in Android apps. Use for SwipeDismissHost setup, rightward dismiss gestures, Predictive Back, custom transitions, and library-specific gesture conflicts.
---

# Swipe Dismiss for Compose

Read the [library README](https://github.com/PhanithNY/swipe-dismiss-compose#readme) for installation and the example. Check the consuming project's declared version before relying on API details. For a new integration, use a tagged [release](https://github.com/PhanithNY/swipe-dismiss-compose/releases) and its Maven ZIP, or the project's existing source checkout. Keep the dependency as `com.suykorng:swipe-dismiss-compose:<version>` so Gradle resolves its transitive dependencies from Maven metadata.

- Give `SwipeDismissHost` a stable, nullable presentation value. Pass `null` to close programmatically. On `onDismissRequest`, clear the presentation or pop the route that owns it. Keep the previous screen in the `background` slot if it should appear behind the gesture.
- Choose a single Back owner for the presented screen. Leave the host's system Back handling enabled when it owns dismissal. Set `handleSystemBack = false` when the surrounding navigation handles Back. Use `gesturesEnabled = false` for temporary blocks, such as a dialog above the host.
- Use `SwipeDismissMotion.enter` and `exit` for presentation and regular dismissal. Customize interactive motion with `swipeTransform` and release or cancellation timing with `settleAnimation`. Keep the foreground fully visible at progress `0` and offscreen or transparent at `1`. An accepted swipe finishes its own exit.
- The library supports API 21 and above. Device corner radii are available on API 31 and above when the system reports them. Native Back progress requires API 34 and gesture navigation with Predictive Back available. Earlier versions use regular system Back and support the in-app swipe.

After integrating, build the affected module. When you can run the app, check a tap, vertical scrolling, a short swipe that cancels, a swipe that dismisses, and Back. For a custom transform, check both endpoints and the revealed background.
