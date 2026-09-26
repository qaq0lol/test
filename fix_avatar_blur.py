import re

with open('app/src/main/java/com/pilltrack/nativeapp/DataModels.kt', 'r') as f:
    content = f.read()

# Current issue: decodeSampledFile limits reqSize and uses RGB_565
# Avatar is requested with reqSize=512 but the image might be small and RGB_565 makes it look bad or blurry, especially on high-density screens.
# Also `inPreferredConfig = Bitmap.Config.RGB_565` heavily affects image quality (removes alpha, limits colors). For avatars, ARGB_8888 is much better.

search_decode = """            BitmapFactory.decodeFile(
                file.absolutePath,
                BitmapFactory.Options().apply {
                    inSampleSize = sample
                    inPreferredConfig = Bitmap.Config.RGB_565
                }
            )"""

replace_decode = """            BitmapFactory.decodeFile(
                file.absolutePath,
                BitmapFactory.Options().apply {
                    inSampleSize = sample
                    // Use ARGB_8888 for better quality (especially for avatars) instead of RGB_565
                    inPreferredConfig = Bitmap.Config.ARGB_8888
                }
            )"""

if search_decode in content:
    content = content.replace(search_decode, replace_decode)


# Also update the reqSize in loadAvatarBitmap to 1080 to ensure crispness on high DPI screens
search_avatar = "fun loadAvatarBitmap(path: String?): Bitmap? {\n        return decodeSampledFile(path, 512)\n    }"
replace_avatar = "fun loadAvatarBitmap(path: String?): Bitmap? {\n        return decodeSampledFile(path, 1080)\n    }"

if search_avatar in content:
    content = content.replace(search_avatar, replace_avatar)


with open('app/src/main/java/com/pilltrack/nativeapp/DataModels.kt', 'w') as f:
    f.write(content)
