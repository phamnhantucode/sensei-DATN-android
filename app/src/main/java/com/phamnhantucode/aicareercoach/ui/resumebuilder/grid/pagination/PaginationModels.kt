package com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.pagination

import java.util.UUID

/**
 * Split strategy for pagination - determines how elements are split across pages
 */
enum class SplitStrategy {
    AT_ITEM_BOUNDARY,      // Split between list items (WorkExperience, Education, etc.)
    AT_CHILD_BOUNDARY,     // Split container at child element boundary
    NO_SPLIT               // Element cannot be split, move entirely to next page
}

/**
 * Tracks linked elements across pages for auto-repagination
 * When content changes, linked elements can automatically reflow
 * 
 * @param groupId Unique identifier for this linked group
 * @param sourceElementId Original element ID on the first page
 * @param linkedElementIds Continuation element IDs on subsequent pages (ordered by page)
 * @param elementType Class simple name for type safety during serialization
 * @param splitStrategy How this element was/should be split
 */
data class LinkedElementGroup(
    val groupId: String = UUID.randomUUID().toString(),
    val sourceElementId: String,
    val linkedElementIds: List<String> = emptyList(),
    val elementType: String,
    val splitStrategy: SplitStrategy
) {
    /**
     * Get all element IDs in this group (source + linked)
     */
    fun getAllElementIds(): List<String> = listOf(sourceElementId) + linkedElementIds
    
    /**
     * Check if an element belongs to this group
     */
    fun containsElement(elementId: String): Boolean = 
        sourceElementId == elementId || linkedElementIds.contains(elementId)
    
    /**
     * Get the page index for a specific element in this group
     * Returns 0 for source, 1+ for linked elements
     */
    fun getPageIndexForElement(elementId: String): Int? {
        if (elementId == sourceElementId) return 0
        val linkedIndex = linkedElementIds.indexOf(elementId)
        return if (linkedIndex >= 0) linkedIndex + 1 else null
    }
}

/**
 * Pagination metadata stored per page
 * 
 * @param pageIndex Zero-based index of this page
 * @param isOverflowPage True if this page was automatically created due to content overflow
 * @param continuedFromPreviousPage Element IDs that are continuations from the previous page
 * @param continuesOnNextPage Element IDs that overflow and continue on the next page
 */
data class PagePaginationInfo(
    val pageIndex: Int,
    val isOverflowPage: Boolean = false,
    val continuedFromPreviousPage: List<String> = emptyList(),
    val continuesOnNextPage: List<String> = emptyList()
) {
    /**
     * Check if this page has any continued elements
     */
    val hasContinuedElements: Boolean
        get() = continuedFromPreviousPage.isNotEmpty()
    
    /**
     * Check if this page has overflowing elements
     */
    val hasOverflowingElements: Boolean
        get() = continuesOnNextPage.isNotEmpty()
}

/**
 * Result of pagination operation
 * 
 * @param pages List of paginated pages
 * @param linkedElementGroups Cross-page element linking information
 * @param totalContentHeightDp Total height of all content across all pages
 */
data class PaginationResult(
    val pages: List<com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.models.ResumePage>,
    val linkedElementGroups: List<LinkedElementGroup>,
    val totalContentHeightDp: Float = 0f
)

/**
 * Result of splitting a single element
 * 
 * @param firstPart Element portion that fits on the current page (null if nothing fits)
 * @param secondPart Element portion that overflows to the next page (null if everything fits)
 * @param strategy The split strategy that was used
 * @param splitAtIndex For list-based elements, the index where the split occurred
 * @param overflowChildIds For container splits, the IDs of child elements that should move to the next page
 */
data class SplitResult(
    val firstPart: com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.models.ResumeElement?,
    val secondPart: com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.models.ResumeElement?,
    val strategy: SplitStrategy,
    val splitAtIndex: Int? = null,
    val overflowChildIds: List<String> = emptyList()
)

/**
 * Information about content overflow on a page
 * Used for visual indicators and triggering pagination
 * 
 * @param pageIndex Which page has the overflow
 * @param overflowAmountDp How much content exceeds the page boundary (in dp)
 * @param overflowingElementIds IDs of elements that are overflowing
 * @param suggestedSplitPoints Suggested locations to split content
 */
data class OverflowInfo(
    val pageIndex: Int,
    val overflowAmountDp: Float,
    val overflowingElementIds: List<String>,
    val suggestedSplitPoints: List<SuggestedSplitPoint> = emptyList()
)

/**
 * A suggested point where an element could be split
 * 
 * @param elementId The element that could be split
 * @param itemIndex For list-based elements, the item index to split at
 * @param heightSavedDp How much height would be saved by splitting here
 */
data class SuggestedSplitPoint(
    val elementId: String,
    val itemIndex: Int,
    val heightSavedDp: Float
)

/**
 * Height information for a single item within a list-based element
 * Used for calculating split points
 * 
 * @param itemId Unique identifier of the item
 * @param itemIndex Index of the item in the list
 * @param heightDp Height of this item in dp
 * @param cumulativeHeightDp Cumulative height including this item and all previous items
 */
data class ItemHeightInfo(
    val itemId: String,
    val itemIndex: Int,
    val heightDp: Float,
    val cumulativeHeightDp: Float
)

/**
 * Configuration for pagination behavior
 * 
 * @param minItemsPerPage Minimum items to keep on a page before splitting (prevents single-item pages)
 * @param preferKeepTogether List of element types that should avoid splitting if possible
 * @param enableAutoPagination Whether to automatically paginate when content changes
 * @param overflowThresholdDp Minimum overflow amount before triggering auto-pagination
 */
data class PaginationConfig(
    val minItemsPerPage: Int = 1,
    val preferKeepTogether: Set<String> = emptySet(),
    val enableAutoPagination: Boolean = true,
    val overflowThresholdDp: Float = 10f,
    val maxPages: Int = 50 // Maximum pages to prevent infinite recursion
)
