import re

with open('app/src/main/java/com/pilltrack/nativeapp/Screens.kt', 'r') as f:
    content = f.read()

# I forgot to declare `chartScale` and `chartOffsetX` because I used a separate script and failed to match the search string earlier!
# Let's fix that.
# Add import TransformOrigin
if "import androidx.compose.ui.graphics.TransformOrigin" not in content:
    content = content.replace("import androidx.compose.ui.graphics.graphicsLayer", "import androidx.compose.ui.graphics.graphicsLayer\nimport androidx.compose.ui.graphics.TransformOrigin")

search_touch = "var touchX by remember { mutableStateOf<Float?>(null) }"
replace_touch = """var touchX by remember { mutableStateOf<Float?>(null) }
        var chartScale by remember { mutableStateOf(1f) }
        var chartOffsetX by remember { mutableStateOf(0f) }"""

if search_touch in content:
    content = content.replace(search_touch, replace_touch)

with open('app/src/main/java/com/pilltrack/nativeapp/Screens.kt', 'w') as f:
    f.write(content)
