# Auto-Height Feature for Work Experience and Project Elements (Grid Editor)

## Overview
The **Work Experience** and **Project** elements now support automatic height calculation to fit content in the **grid editor**. This ensures that these elements dynamically adjust their height based on the amount of content when using `heightMode = WRAP_CONTENT`.

## Implementation

### DraggableElement.kt

#### Work Experience Element
Added `calculateWorkExperienceContentHeight()` function to calculate content height in the grid editor:

```kotlin
private fun calculateWorkExperienceContentHeight(
    element: ResumeElement.WorkExperienceElement,
    width: Float,
    density: Float,
    zoomLevel: Float,
    context: android.content.Context
): Float
```

**Key Features:**
- Calculates height for all work experience items
- Uses StaticLayout for accurate text measurement with wrapping
- Accounts for all display styles (Standard, Compact, Detailed)
- Considers spacing, padding, and zoom level
- Formats dates and measures them alongside other fields
- Handles bullet points and responsibilities

**Helper Functions Added:**
- `createWorkExperienceTextPaint()` - Creates TextPaint for different text fields
- `createSimpleLayout()` - Creates StaticLayout for text measurements
- `getBulletCharacter()` - Gets the appropriate bullet character (for responsibilities)
- `formatDateRange()` - Formats date ranges for display
- `formatDate()` - Formats individual dates

#### Project Element
Added `calculateProjectContentHeight()` function to calculate content height in the grid editor:

```kotlin
private fun calculateProjectContentHeight(
    element: ResumeElement.ProjectElement,
    width: Float,
    density: Float,
    zoomLevel: Float,
    context: android.content.Context
): Float
```

**Key Features:**
- Calculates height for all project items
- Uses StaticLayout for accurate text measurement with wrapping
- Accounts for all display styles (Standard, Compact, Detailed)
- Considers spacing, padding, and zoom level
- Formats dates and measures them alongside other fields
- Handles bullet points and highlights
- Measures technologies and links

**Helper Functions Added:**
- `getBulletCharacter()` - Gets the appropriate bullet character (for highlights)
- `formatProjectDateRange()` - Formats project date ranges with "Ongoing" support

## Usage

### Work Experience Element
To use auto-height for a work experience element, set the `heightMode` property:

```kotlin
WorkExperienceElement(
    position = GridPosition(
        row = 10,
        col = 5,
        rowSpan = 1,  // Will be ignored when heightMode = WRAP_CONTENT
        colSpan = 20,
        heightMode = SizeMode.WRAP_CONTENT  // Enable auto-height
    ),
    items = listOf(
        WorkExperienceItem(
            jobTitle = "Senior Software Engineer",
            company = "Tech Company",
            startDate = "2020-01-01",
            endDate = "2023-12-31",
            location = "San Francisco, CA",
            responsibilities = listOf(
                ResponsibilityItem("Led team of 5 developers"),
                ResponsibilityItem("Implemented CI/CD pipeline"),
                ResponsibilityItem("Reduced deployment time by 50%")
            )
        ),
        // More items...
    )
)
```

### Project Element
To use auto-height for a project element:

```kotlin
ProjectElement(
    position = GridPosition(
        row = 15,
        col = 5,
        rowSpan = 1,  // Will be ignored when heightMode = WRAP_CONTENT
        colSpan = 20,
        heightMode = SizeMode.WRAP_CONTENT  // Enable auto-height
    ),
    items = listOf(
        ProjectItem(
            name = "E-Commerce Platform",
            description = "Built a full-stack e-commerce platform with React and Node.js",
            startDate = "2022-01-01",
            endDate = "2023-06-30",
            technologies = "React, TypeScript, Node.js, MongoDB, Docker",
            link = "https://github.com/user/project",
            highlights = listOf(
                ProjectHighlight("Implemented payment gateway integration"),
                ProjectHighlight("Optimized database queries reducing load time by 40%"),
                ProjectHighlight("Deployed using CI/CD pipeline with Docker")
            )
        ),
        // More items...
    )
)
```

## How It Works

1. **Height Calculation**: The renderer calculates the total height needed by:
   - Creating text layouts for each field (title, company, date, location)
   - Measuring responsibility bullet points with text wrapping
   - Adding spacing between sections
   - Including content padding

2. **Dynamic Adjustment**: When rendering:
   - If `heightMode = FIXED`, uses the original grid-based bounds (rowSpan)
   - If `heightMode = WRAP_CONTENT`, calculates required height and adjusts bounds

3. **Content Padding**: Consistent 8dp padding is applied top and bottom, matching the screen renderer behavior

## Benefits

- **No Content Clipping**: Content will never be cut off, regardless of how many work experiences are added
- **Consistent Spacing**: Maintains proper spacing between all elements
- **Flexible Layouts**: Works with all display styles (Standard, Compact, Detailed)
- **Easy to Use**: Simply set `heightMode = WRAP_CONTENT` in the position

## Technical Details

### Grid Editor Calculation
The grid editor uses pixel-based calculations with density and zoom:
```kotlin
// Base dimensions (unscaled)
val basePadding = 8f * density
val baseWidth = (width / zoomLevel) - (basePadding * 2)

// Calculate content height
val contentHeight = calculateWorkExperienceContentHeight(...)

// Apply zoom to final height
val finalHeight = (contentHeight + basePadding * 2) * zoomLevel
```

### Display Style Support

#### Work Experience
All three display styles are fully supported:
- **Standard**: Title, company+date, location on separate lines
- **Compact**: Title+company+date on one line, location below
- **Detailed**: All fields on separate lines with maximum prominence

#### Project
All three display styles are fully supported:
- **Standard**: Name, description, technologies, date, link on separate lines
- **Compact**: Name+date on one line, other fields below
- **Detailed**: All fields prominently displayed with maximum spacing

## Benefits

- **No Content Clipping**: Content will never be cut off in the grid editor
- **Consistent Spacing**: Maintains proper spacing between all elements
- **Flexible Layouts**: Works with all display styles (Standard, Compact, Detailed)
- **Zoom Independent**: Correctly handles zoom levels in editor
- **Easy to Use**: Simply set `heightMode = WRAP_CONTENT` in the element's position
- **Multiple Elements**: Works for both Work Experience and Project elements

## Limitations

- **Grid Editor Only**: This feature only works in the grid editor, not in PDF export
- Width is still determined by `colSpan` (only height is auto-calculated)
- Vertical alignment settings are ignored when using WRAP_CONTENT (always starts from top)
- For Work Experience: Horizontal layout orientation uses fixed column widths (not affected by wrap content)

## Related Files

- `DraggableElement.kt` - Grid editor with height calculation implementation
- `GridResumeModels.kt` - SizeMode enum definition (FIXED, WRAP_CONTENT)
