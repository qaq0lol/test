import re

with open('app/src/main/java/com/pilltrack/nativeapp/Screens.kt', 'r') as f:
    content = f.read()

# Make the area fill alpha slightly higher for better visibility
search_alpha = """                        drawPath(
                            path = fillPath,
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    drugColor.copy(alpha = 0.45f * fillAlphaFactor),
                                    drugColor.copy(alpha = 0.02f * fillAlphaFactor)
                                ),
                                startY = 0f,
                                endY = chartBottom
                            )
                        )"""
replace_alpha = """                        drawPath(
                            path = fillPath,
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    drugColor.copy(alpha = 0.55f * fillAlphaFactor),
                                    drugColor.copy(alpha = 0.05f * fillAlphaFactor)
                                ),
                                startY = 0f,
                                endY = chartBottom
                            )
                        )"""

if search_alpha in content:
    content = content.replace(search_alpha, replace_alpha)

with open('app/src/main/java/com/pilltrack/nativeapp/Screens.kt', 'w') as f:
    f.write(content)
