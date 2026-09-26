import re

with open('app/src/main/java/com/pilltrack/nativeapp/Screens.kt', 'r') as f:
    content = f.read()

# Fix graphicsLayer inline imports
content = content.replace(".androidx.compose.ui.graphics.graphicsLayer {", ".graphicsLayer {")

# Add imports
if "import androidx.compose.ui.graphics.graphicsLayer" not in content:
    content = content.replace('import androidx.compose.ui.Modifier\n', 'import androidx.compose.ui.Modifier\nimport androidx.compose.ui.graphics.graphicsLayer\n')
if "import androidx.compose.material.icons.filled.Search" not in content:
    content = content.replace('import androidx.compose.material.icons.filled.Add\n', 'import androidx.compose.material.icons.filled.Add\nimport androidx.compose.material.icons.filled.Search\nimport androidx.compose.material.icons.filled.Clear\n')

# Wait, `grouped` is `val grouped by remember { derivedStateOf { ... } }`
# but the derivedStateOf is missing an import maybe? No, `by` requires `import androidx.compose.runtime.getValue`
if "import androidx.compose.runtime.getValue" not in content:
    content = content.replace('import androidx.compose.runtime.*\n', 'import androidx.compose.runtime.*\nimport androidx.compose.runtime.getValue\nimport androidx.compose.runtime.setValue\n')


# Text ambiguous issue:
# e: file:///app/app/src/main/java/com/pilltrack/nativeapp/Screens.kt:1560:37 Overload resolution ambiguity:
# Text(dateStr, style = ...) -> maybe dateStr is not a String?
# In `grouped.forEach { (dateStr, dayLogs) ->`, dateStr is the key from `toSortedMap()`.
# SimpleDateFormat formats to String, so dateStr should be a String.
# But because derivedStateOf was not imported or something, it might be typed as `Any`. Let's ensure types.

# Change `val grouped by remember(...) { derivedStateOf { ... } }`
# to `val grouped = remember(...) { ... }` since we don't strictly need derivedStateOf if we are using `remember` with keys.
content = content.replace("val grouped by remember(logs, searchQuery) {\n            derivedStateOf {", "val grouped = remember(logs, searchQuery) {")
content = content.replace("}.toSortedMap(reverseOrder())\n            }\n        }", "}.toSortedMap(reverseOrder())\n        }")

# Remove `androidx.compose.foundation.BorderStroke` to just `BorderStroke`
content = content.replace("androidx.compose.foundation.BorderStroke", "BorderStroke")
if "import androidx.compose.foundation.BorderStroke" not in content:
    content = content.replace("import androidx.compose.foundation.layout.*", "import androidx.compose.foundation.layout.*\nimport androidx.compose.foundation.BorderStroke")

with open('app/src/main/java/com/pilltrack/nativeapp/Screens.kt', 'w') as f:
    f.write(content)
