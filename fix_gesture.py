import re

with open('app/src/main/java/com/pilltrack/nativeapp/Screens.kt', 'r') as f:
    content = f.read()


search_gesture = """                        .pointerInput(Unit) {
                            detectTapGestures(
                                onPress = { offset ->
                                    touchX = offset.x
                                    tryAwaitRelease()
                                },
                                onTap = { offset ->
                                    touchX = offset.x
                                }
                            )
                        }
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDrag = { change, _ ->
                                    touchX = change.position.x
                                }
                            )
                        }"""

# We apply detectTransformGestures for zoom & pan. We can also handle tap within it or keep detectTapGestures.
# Since detectTransformGestures might consume events, we can use graphicsLayer on the Box or Canvas.
# Wait, if we use graphicsLayer, then the touchX calculation (which uses raw pixel X) will be correct relative to the zoomed canvas!
# Wait, if we zoom the Box using graphicsLayer, it's very easy to implement pinch-to-zoom!

replace_gesture = """                        .graphicsLayer(
                            scaleX = chartScale,
                            scaleY = chartScale,
                            translationX = chartOffsetX,
                            transformOrigin = TransformOrigin(0f, 0.5f)
                        )
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onPress = { offset ->
                                    touchX = offset.x
                                    tryAwaitRelease()
                                },
                                onTap = { offset ->
                                    touchX = offset.x
                                }
                            )
                        }
                        .pointerInput(Unit) {
                            detectTransformGestures { centroid, pan, zoom, rotation ->
                                chartScale = (chartScale * zoom).coerceIn(1f, 5f)
                                chartOffsetX = (chartOffsetX + pan.x).coerceIn(-size.width * (chartScale - 1f), 0f)
                            }
                        }"""

if search_gesture in content:
    content = content.replace(search_gesture, replace_gesture)
    print("Replaced gesture")
else:
    print("Gesture search string not found.")


with open('app/src/main/java/com/pilltrack/nativeapp/Screens.kt', 'w') as f:
    f.write(content)
