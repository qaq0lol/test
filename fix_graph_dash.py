import re

with open('app/src/main/java/com/pilltrack/nativeapp/Screens.kt', 'r') as f:
    content = f.read()

# Make baseline dotted line subtle but slightly more prominent
search_dash = """                    drawPath(
                        path = baselinePath,
                        color = Color.White.copy(alpha = 0.25f),
                        style = Stroke(
                            width = 2f,
                            pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                        )
                    )"""
replace_dash = """                    drawPath(
                        path = baselinePath,
                        color = Color.White.copy(alpha = 0.35f),
                        style = Stroke(
                            width = 3f,
                            pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(15f, 15f), 0f)
                        )
                    )"""

if search_dash in content:
    content = content.replace(search_dash, replace_dash)

with open('app/src/main/java/com/pilltrack/nativeapp/Screens.kt', 'w') as f:
    f.write(content)
