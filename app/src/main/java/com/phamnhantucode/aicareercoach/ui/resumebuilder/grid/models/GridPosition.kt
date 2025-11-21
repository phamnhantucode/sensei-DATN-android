package com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.models

/**
 * Grid-based position
 * @param row Starting row (0-based)
 * @param col Starting column (0-based)
 * @param rowSpan Number of rows to span
 * @param colSpan Number of columns to span
 * @param widthMode How to calculate width (FIXED or WRAP_CONTENT)
 * @param heightMode How to calculate height (FIXED or WRAP_CONTENT)
 * @param cachedHeightDp Cached calculated height in dp when heightMode is WRAP_CONTENT (null means not yet calculated)
 */
data class GridPosition(
    val row: Int = 0,
    val col: Int = 0,
    val rowSpan: Int = 1,
    val colSpan: Int = 1,
    val widthMode: SizeMode = SizeMode.FIXED,
    val heightMode: SizeMode = SizeMode.FIXED,
    val cachedHeightDp: Float? = null
) {
    fun overlaps(other: GridPosition): Boolean {
        val thisEndRow = row + rowSpan
        val thisEndCol = col + colSpan
        val otherEndRow = other.row + other.rowSpan
        val otherEndCol = other.col + other.colSpan

        return !(thisEndRow <= other.row ||
                 row >= otherEndRow ||
                 thisEndCol <= other.col ||
                 col >= otherEndCol)
    }

    fun contains(gridRow: Int, gridCol: Int): Boolean {
        return gridRow >= row && gridRow < row + rowSpan &&
               gridCol >= col && gridCol < col + colSpan
    }
}

data class Padding(
    val top: Float = 0f,
    val right: Float = 0f,
    val bottom: Float = 0f,
    val left: Float = 0f
)
