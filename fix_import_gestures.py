import re

with open('app/src/main/java/com/pilltrack/nativeapp/Screens.kt', 'r') as f:
    content = f.read()

# Make sure detectTransformGestures is imported (it is from androidx.compose.foundation.gestures.*)
if "import androidx.compose.foundation.gestures.detectTransformGestures" not in content:
    content = content.replace("import androidx.compose.foundation.gestures.detectTapGestures", "import androidx.compose.foundation.gestures.detectTapGestures\nimport androidx.compose.foundation.gestures.detectTransformGestures")

with open('app/src/main/java/com/pilltrack/nativeapp/Screens.kt', 'w') as f:
    f.write(content)
