# Skill Chip Text Color Fix

## Problem
Skill items displayed as chips had poor text visibility on:
1. Resume Builder form tab (Skills section)
2. AI Enhancement Dialog (Enhanced Skills section)

The text color on skill chips was hard to see against the light background color.

## Root Cause
Both the `SkillChip` composable in ResumeBuilderScreen and skill chips in ResumeEnhancementDialog were using `MaterialTheme.colorScheme.primary` as text color on a light background (`primary.copy(alpha = 0.12f)` or `primary.copy(alpha = 0.15f)`). This creates poor contrast making text hard to read.

## Solution
Changed the text color from `MaterialTheme.colorScheme.primary` to `MaterialTheme.colorScheme.onSurface` in both locations. The `onSurface` color is designed to have proper contrast with surface colors, ensuring readability.

### Changes Made

1. **ResumeBuilderScreen.kt** - Updated `SkillChip` composable (line 1329):
   ```kotlin
   // Before:
   color = MaterialTheme.colorScheme.primary
   
   // After:
   color = MaterialTheme.colorScheme.onSurface
   ```
   Also updated the close icon tint to match (line 1338).

2. **ResumeEnhancementDialog.kt** - Updated skill chips in `EnhancedSkillsCard` (line 1361):
   ```kotlin
   // Before:
   color = MaterialTheme.colorScheme.primary
   
   // After:
   color = MaterialTheme.colorScheme.onSurface
   ```

## User Impact
- **Immediate Fix**: Skill chip text is now clearly readable with proper contrast
- **Material Design Compliance**: Uses correct semantic color (`onSurface`) for text on surface backgrounds
- **Consistency**: Both Resume Builder form and AI Enhancement dialog now have consistent, readable skill chips
- **Dark Mode Support**: `onSurface` color automatically adapts to dark/light themes

## Testing Recommendations
1. Open Resume Builder and navigate to Skills section
2. Add skills and verify text is clearly visible on chip backgrounds
3. Open AI Enhancement dialog with skill suggestions
4. Verify enhanced skill chips have readable text
5. Test in both light and dark modes (if supported)

## Files Modified
- `app/src/main/java/com/phamnhantucode/aicareercoach/ui/resumebuilder/ResumeBuilderScreen.kt`
- `app/src/main/java/com/phamnhantucode/aicareercoach/ui/resumebuilder/ResumeEnhancementDialog.kt`

## Note
The previous changes to SkillElementRenderer and PropertyPanel for grid-based resume templates are still valid and provide customization for PDF export and template rendering. This fix addresses the UI form components specifically.

