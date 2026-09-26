import re

with open('app/src/main/java/com/pilltrack/nativeapp/Screens.kt', 'r') as f:
    content = f.read()

# Revert touchX calculation to use `offset.x` directly because PointerInput offsets
# are already affected by graphicsLayer transformations in Compose.

search_touch = """                            detectTapGestures(
                                onPress = { offset ->
                                    touchX = (offset.x - chartOffsetX) / chartScale
                                    tryAwaitRelease()
                                },
                                onTap = { offset ->
                                    touchX = (offset.x - chartOffsetX) / chartScale
                                }
                            )"""

replace_touch = """                            detectTapGestures(
                                onPress = { offset ->
                                    touchX = offset.x
                                    tryAwaitRelease()
                                },
                                onTap = { offset ->
                                    touchX = offset.x
                                }
                            )"""

if search_touch in content:
    content = content.replace(search_touch, replace_touch)

# Fix panning bug:
# `detectTransformGestures { centroid, pan, zoom, rotation ->`
# The pan returned is in the current (scaled) coordinate space.
# translationX requires values in the parent's unscaled pixel space.
search_pan = """                                chartOffsetX = (chartOffsetX + pan.x).coerceIn(-size.width * (chartScale - 1f), 0f)"""
replace_pan = """                                chartOffsetX = (chartOffsetX + pan.x * chartScale).coerceIn(-size.width * (chartScale - 1f), 0f)"""
if search_pan in content:
    content = content.replace(search_pan, replace_pan)

with open('app/src/main/java/com/pilltrack/nativeapp/Screens.kt', 'w') as f:
    f.write(content)
