import re

with open('app/src/main/java/com/pilltrack/nativeapp/MainActivity.kt', 'r') as f:
    main_content = f.read()

# Add clip import to MainActivity
if 'import androidx.compose.ui.draw.clip' not in main_content:
    main_content = main_content.replace('import androidx.compose.ui.Modifier\n', 'import androidx.compose.ui.Modifier\nimport androidx.compose.ui.draw.clip\n')

with open('app/src/main/java/com/pilltrack/nativeapp/MainActivity.kt', 'w') as f:
    f.write(main_content)


with open('app/src/main/java/com/pilltrack/nativeapp/Screens.kt', 'r') as f:
    screens_content = f.read()

# Add clip import to Screens
if 'import androidx.compose.ui.draw.clip' not in screens_content:
    screens_content = screens_content.replace('import androidx.compose.ui.Modifier\n', 'import androidx.compose.ui.Modifier\nimport androidx.compose.ui.draw.clip\n')

# Fix Unresolved reference: androidx (lines 138, 283, 512). This typically happens if it's nested or malformed inside a Modifier or parameter.
# The error says "Unresolved reference: clip" and "Unresolved reference: androidx" around line 138.
# Maybe I should check what is on line 138.
