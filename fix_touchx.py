import re

with open('app/src/main/java/com/pilltrack/nativeapp/Screens.kt', 'r') as f:
    content = f.read()

# Since we applied scale and offset via graphicsLayer, the touch coordinates coming from detectTapGestures
# are relative to the ORIGINAL unscaled/untranslated bounds because graphicsLayer is applied *after* pointerInput on the modifier chain,
# or rather pointerInput receives the touch in the local coordinate space of the Box/Canvas *before* graphicsLayer affects the parent?
# Wait, Jetpack Compose pointer input is affected by graphicsLayer. If graphicsLayer scales the content, the offset received in pointerInput is scaled.
# Let's ensure touchX calculation accurately maps back to the canvas coordinate system when zoomed.
# Wait, actually `graphicsLayer` scales the rendered content.
# If we touch at pixel `x`, and the chart is offset by `chartOffsetX` and scaled by `chartScale`,
# the *actual* point on the chart is: `(x - chartOffsetX) / chartScale`.

search_touch = """                            detectTapGestures(
                                onPress = { offset ->
                                    touchX = offset.x
                                    tryAwaitRelease()
                                },
                                onTap = { offset ->
                                    touchX = offset.x
                                }
                            )"""

replace_touch = """                            detectTapGestures(
                                onPress = { offset ->
                                    touchX = (offset.x - chartOffsetX) / chartScale
                                    tryAwaitRelease()
                                },
                                onTap = { offset ->
                                    touchX = (offset.x - chartOffsetX) / chartScale
                                }
                            )"""

if search_touch in content:
    content = content.replace(search_touch, replace_touch)

with open('app/src/main/java/com/pilltrack/nativeapp/Screens.kt', 'w') as f:
    f.write(content)
