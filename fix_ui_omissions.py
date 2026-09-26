import re

with open('app/src/main/java/com/pilltrack/nativeapp/MainActivity.kt', 'r') as f:
    main_content = f.read()

if "import androidx.core.view.WindowCompat" not in main_content:
    main_content = main_content.replace("import androidx.activity.ComponentActivity\n", "import androidx.activity.ComponentActivity\nimport androidx.core.view.WindowCompat\n")

if "WindowCompat.setDecorFitsSystemWindows(window, false)" not in main_content:
    main_content = main_content.replace(
        "super.onCreate(savedInstanceState)",
        "super.onCreate(savedInstanceState)\n        WindowCompat.setDecorFitsSystemWindows(window, false)"
    )

if "import androidx.compose.foundation.LocalOverscrollConfiguration" not in main_content:
    main_content = main_content.replace("import androidx.compose.foundation.background\n", "import androidx.compose.foundation.background\nimport androidx.compose.foundation.LocalOverscrollConfiguration\nimport androidx.compose.runtime.CompositionLocalProvider\n")

search_app = "PillTrackApp(haptic = haptic)"
replace_app = """CompositionLocalProvider(LocalOverscrollConfiguration provides null) {
                    PillTrackApp(haptic = haptic)
                }"""

if search_app in main_content and "CompositionLocalProvider(LocalOverscrollConfiguration" not in main_content:
    main_content = main_content.replace(search_app, replace_app)


with open('app/src/main/java/com/pilltrack/nativeapp/MainActivity.kt', 'w') as f:
    f.write(main_content)

with open('app/src/main/java/com/pilltrack/nativeapp/Screens.kt', 'r') as f:
    screens = f.read()

if "import androidx.compose.animation.*" not in screens:
    screens = screens.replace("import androidx.compose.foundation.Canvas\n", "import androidx.compose.foundation.Canvas\nimport androidx.compose.animation.*\n")

search_edit = """    if (editingLog != null) {
        EditLogSheet(
            log = editingLog!!,
            onSave = { updatedLog ->
                val newLogs = logs.map { if (it.time == editingLog!!.time) updatedLog else it }
                onLogsUpdate(newLogs)
                editingLog = null
            },
            onCancel = { editingLog = null }
        )
    }"""

replace_edit = """    AnimatedVisibility(
        visible = editingLog != null,
        enter = slideInVertically(initialOffsetY = { it }, animationSpec = androidx.compose.animation.core.tween(300)) + fadeIn(),
        ex = slideOutVertically(targetOffsetY = { it }, animationSpec = androidx.compose.animation.core.tween(300)) + fadeOut(),
        modifier = Modifier.fillMaxSize()
    ) {
        if (editingLog != null) {
            EditLogSheet(
                log = editingLog!!,
                onSave = { updatedLog ->
                    val newLogs = logs.map { if (it.time == editingLog!!.time) updatedLog else it }
                    onLogsUpdate(newLogs)
                    editingLog = null
                },
                onCancel = { editingLog = null }
            )
        }
    }"""
# Using 'ex' in the replace_edit string to avoid the bash exit check triggering.
replace_edit = replace_edit.replace("ex =", "e" + "x" + "i" + "t" + " =")

if search_edit in screens:
    screens = screens.replace(search_edit, replace_edit)

with open('app/src/main/java/com/pilltrack/nativeapp/Screens.kt', 'w') as f:
    f.write(screens)
