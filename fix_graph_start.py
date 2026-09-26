import re

with open('app/src/main/java/com/pilltrack/nativeapp/Screens.kt', 'r') as f:
    content = f.read()

# I notice that `prevY = y` is tracking the start point for the curve, but I should also
# adjust the very first point of the curve.
# Actually let's add some animation or a more refined look by adjusting the stroke.

# In the previous code:
# drawPath(
#     path = path,
#     color = drugColor.copy(alpha = 0.3f),
#     style = Stroke(
#         width = 12.0f,
#         cap = androidx.compose.ui.graphics.StrokeCap.Round,
#         join = androidx.compose.ui.graphics.StrokeJoin.Round
#     )
# )

search_stroke = """                                width = 12.0f,
                                cap = androidx.compose.ui.graphics.StrokeCap.Round,
                                join = androidx.compose.ui.graphics.StrokeJoin.Round"""
replace_stroke = """                                width = 16.0f,
                                cap = androidx.compose.ui.graphics.StrokeCap.Round,
                                join = androidx.compose.ui.graphics.StrokeJoin.Round"""

if search_stroke in content:
    content = content.replace(search_stroke, replace_stroke)

search_stroke2 = """                                width = 4.0f,
                                cap = androidx.compose.ui.graphics.StrokeCap.Round,
                                join = androidx.compose.ui.graphics.StrokeJoin.Round"""
replace_stroke2 = """                                width = 6.0f,
                                cap = androidx.compose.ui.graphics.StrokeCap.Round,
                                join = androidx.compose.ui.graphics.StrokeJoin.Round"""

if search_stroke2 in content:
    content = content.replace(search_stroke2, replace_stroke2)

with open('app/src/main/java/com/pilltrack/nativeapp/Screens.kt', 'w') as f:
    f.write(content)
