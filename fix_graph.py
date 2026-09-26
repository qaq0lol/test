import re

with open('app/src/main/java/com/pilltrack/nativeapp/Screens.kt', 'r') as f:
    content = f.read()

# Make curves smoother by using quadratic bezier paths in the graph instead of straight lineTo

search_block = """                    for (i in 0..pointsToDraw) {
                        val tHours = (i.toFloat() / numPoints) * evalHours
                        val absoluteTime = timelineStart + (tHours * 60 * 60 * 1000).toLong()

                        val c = concs[i]
                        val x = (i.toFloat() / numPoints) * w
                        val y = chartBottom - ((c / suggestedMax) * (chartBottom - 20.dp.toPx())).coerceIn(0f, chartBottom)

                        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)

                        if (curTouchX != null && kotlin.math.abs(x - curTouchX) < (w / numPoints * 1.2f)) {
                            touchY = y
                            touchVal = c
                        }
                    }"""

replace_block = """                    var prevX = 0f
                    var prevY = 0f

                    for (i in 0..pointsToDraw) {
                        val tHours = (i.toFloat() / numPoints) * evalHours
                        val absoluteTime = timelineStart + (tHours * 60 * 60 * 1000).toLong()

                        val c = concs[i]
                        val x = (i.toFloat() / numPoints) * w
                        val y = chartBottom - ((c / suggestedMax) * (chartBottom - 20.dp.toPx())).coerceIn(0f, chartBottom)

                        if (i == 0) {
                            path.moveTo(x, y)
                        } else {
                            // Smooth bezier curve implementation
                            val controlPointX = (prevX + x) / 2f
                            path.cubicTo(controlPointX, prevY, controlPointX, y, x, y)
                        }

                        prevX = x
                        prevY = y

                        if (curTouchX != null && kotlin.math.abs(x - curTouchX) < (w / numPoints * 1.2f)) {
                            touchY = y
                            touchVal = c
                        }
                    }"""

if search_block in content:
    content = content.replace(search_block, replace_block)
else:
    print("Block not found!")


with open('app/src/main/java/com/pilltrack/nativeapp/Screens.kt', 'w') as f:
    f.write(content)
