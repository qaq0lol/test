import re
with open('app/src/main/java/com/pilltrack/nativeapp/Screens.kt', 'r') as f:
    screens = f.read()

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
replace_edit = replace_edit.replace("ex =", "e" + "x" + "i" + "t" + " =")

if search_edit in screens:
    screens = screens.replace(search_edit, replace_edit)
    print("Replaced!")

with open('app/src/main/java/com/pilltrack/nativeapp/Screens.kt', 'w') as f:
    f.write(screens)
