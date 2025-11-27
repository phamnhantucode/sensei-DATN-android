package com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.pagination

import com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.models.*
import java.util.UUID

/**
 * Core pagination engine for multi-page resume support
 * 
 * Handles:
 * - Overflow detection
 * - Element splitting at item boundaries
 * - Page creation and management
 * - Linked element group tracking for repagination
 */
class PaginationEngine(
    private val gridConfig: GridConfig,
    private val heightCalculator: ElementHeightCalculator,
    private val config: PaginationConfig = PaginationConfig()
) {
    private val cellSizeDp = gridConfig.cellSizeDp
    private val pageHeightDp = gridConfig.rows * cellSizeDp
    private val pageWidthDp = gridConfig.columns * cellSizeDp
    
    /**
     * Main entry point: paginate an entire resume
     * Processes all pages and creates overflow pages as needed
     * 
     * @param resume The resume to paginate
     * @return PaginationResult with updated pages and linked element groups
     */
    fun paginate(resume: GridResume): PaginationResult {
        val allPages = mutableListOf<ResumePage>()
        val linkedGroups = mutableListOf<LinkedElementGroup>()
        var totalContentHeight = 0f
        
        // Process each existing page
        resume.pages.forEachIndexed { pageIndex, page ->
            val pageResult = paginateSinglePage(
                page = page,
                startingPageIndex = allPages.size,
                existingGroups = resume.linkedElementGroups,
                allElements = getAllElementsFromResume(resume)
            )
            
            allPages.addAll(pageResult.pages)
            linkedGroups.addAll(pageResult.linkedGroups)
            totalContentHeight += pageResult.contentHeightDp
        }
        
        // Update page indices in pagination info
        val finalPages = allPages.mapIndexed { index, page ->
            page.copy(
                paginationInfo = page.paginationInfo?.copy(pageIndex = index)
                    ?: PagePaginationInfo(pageIndex = index)
            )
        }
        
        return PaginationResult(
            pages = finalPages,
            linkedElementGroups = mergeLinkedGroups(resume.linkedElementGroups, linkedGroups),
            totalContentHeightDp = totalContentHeight
        )
    }
    
    /**
     * Detect overflow on a single page without creating new pages
     * Used for visual indicators in the editor
     * 
     * @param page The page to check
     * @param allElements All elements in the resume (for container children)
     * @return OverflowInfo if overflow detected, null otherwise
     */
    fun detectOverflow(
        page: ResumePage,
        allElements: List<ResumeElement>
    ): OverflowInfo? {
        val overflowingElements = mutableListOf<String>()
        val suggestedSplits = mutableListOf<SuggestedSplitPoint>()
        var maxOverflow = 0f
        
        page.elements.forEach { element ->
            val topDp = element.position.row * cellSizeDp
            val widthDp = element.position.colSpan * cellSizeDp
            val heightDp = calculateElementHeight(element, widthDp, allElements)
            val bottomDp = topDp + heightDp
            
            if (bottomDp > pageHeightDp) {
                overflowingElements.add(element.id)
                maxOverflow = maxOf(maxOverflow, bottomDp - pageHeightDp)
                
                // Calculate split points for splittable elements
                val splitPoints = calculateSplitPoints(element, pageHeightDp - topDp, widthDp)
                suggestedSplits.addAll(splitPoints)
            }
        }
        
        return if (maxOverflow > config.overflowThresholdDp) {
            OverflowInfo(
                pageIndex = page.paginationInfo?.pageIndex ?: 0,
                overflowAmountDp = maxOverflow,
                overflowingElementIds = overflowingElements,
                suggestedSplitPoints = suggestedSplits
            )
        } else {
            null
        }
    }
    
    /**
     * Paginate a single page, potentially creating multiple pages
     */
    private fun paginateSinglePage(
        page: ResumePage,
        startingPageIndex: Int,
        existingGroups: List<LinkedElementGroup>,
        allElements: List<ResumeElement>,
        recursionDepth: Int = 0
    ): SinglePageResult {
        // Guard against infinite recursion
        if (recursionDepth >= config.maxPages) {
            return SinglePageResult(
                pages = listOf(page),
                linkedGroups = emptyList(),
                contentHeightDp = page.elements.maxOfOrNull { 
                    it.position.row * cellSizeDp + calculateElementHeight(it, it.position.colSpan * cellSizeDp, allElements)
                } ?: 0f
            )
        }
        
        val currentPageElements = mutableListOf<ResumeElement>()
        val overflowElements = mutableListOf<ResumeElement>()
        val newLinkedGroups = mutableListOf<LinkedElementGroup>()
        var contentHeight = 0f
        
        // Identify all child elements (they're handled by their parent containers, not directly)
        val allContainerChildIds = page.elements
            .filterIsInstance<ResumeElement.ContainerElement>()
            .flatMap { it.children }
            .toSet()
        
        // Sort elements by vertical position (top to bottom)
        val sortedElements = page.elements.sortedBy { it.position.row }
        
        sortedElements.forEach { element ->
            // Skip child elements - they are handled through their parent container
            if (element.id in allContainerChildIds) {
                // Don't process children directly, they stay/move with their container
                return@forEach
            }
            
            val elementTopDp = element.position.row * cellSizeDp
            val elementWidthDp = element.position.colSpan * cellSizeDp
            val elementHeightDp = calculateElementHeight(element, elementWidthDp, allElements)
            val elementBottomDp = elementTopDp + elementHeightDp
            
            contentHeight = maxOf(contentHeight, elementBottomDp)
            
            when {
                // Element fully fits on current page
                elementBottomDp <= pageHeightDp -> {
                    currentPageElements.add(element)
                    
                    // If this is a container, add all its children to current page too
                    if (element is ResumeElement.ContainerElement) {
                        element.children.forEach { childId ->
                            page.elements.find { it.id == childId }?.let { child ->
                                currentPageElements.add(child)
                            }
                        }
                    }
                }
                
                // Element starts on page but overflows - try to split
                elementTopDp < pageHeightDp && elementBottomDp > pageHeightDp -> {
                    val availableHeight = pageHeightDp - elementTopDp
                    val splitResult = splitElement(element, availableHeight, elementWidthDp, allElements)
                    
                    when {
                        // Successfully split
                        splitResult.firstPart != null && splitResult.secondPart != null -> {
                            currentPageElements.add(splitResult.firstPart)
                            overflowElements.add(
                                adjustElementPositionForNextPage(splitResult.secondPart)
                            )
                            
                            // For container splits: add first-part children to current page, second-part children to overflow
                            if (element is ResumeElement.ContainerElement && splitResult.overflowChildIds.isNotEmpty()) {
                                val firstPartChildIds = (splitResult.firstPart as? ResumeElement.ContainerElement)?.children ?: emptyList()
                                
                                // Add first-part children to current page
                                firstPartChildIds.forEach { childId ->
                                    page.elements.find { it.id == childId }?.let { child ->
                                        currentPageElements.add(child)
                                    }
                                }
                                
                                // Add overflow children to overflow page
                                splitResult.overflowChildIds.forEach { childId ->
                                    page.elements.find { it.id == childId }?.let { child ->
                                        overflowElements.add(adjustElementPositionForNextPage(child))
                                    }
                                }
                            }
                            
                            // Create linked group for this split
                            newLinkedGroups.add(LinkedElementGroup(
                                sourceElementId = element.id,
                                linkedElementIds = listOf(splitResult.secondPart.id),
                                elementType = element::class.simpleName ?: "",
                                splitStrategy = splitResult.strategy
                            ))
                        }
                        
                        // Couldn't split - keep on current page if it starts here
                        splitResult.firstPart != null -> {
                            currentPageElements.add(splitResult.firstPart)
                            
                            // If this is a container, add all its children to current page
                            if (element is ResumeElement.ContainerElement) {
                                element.children.forEach { childId ->
                                    page.elements.find { it.id == childId }?.let { child ->
                                        currentPageElements.add(child)
                                    }
                                }
                            }
                        }
                        
                        // Move entire element to next page
                        else -> {
                            overflowElements.add(
                                adjustElementPositionForNextPage(element)
                            )
                            
                            // If this is a container, move all its children to overflow too
                            if (element is ResumeElement.ContainerElement) {
                                element.children.forEach { childId ->
                                    page.elements.find { it.id == childId }?.let { child ->
                                        overflowElements.add(adjustElementPositionForNextPage(child))
                                    }
                                }
                            }
                        }
                    }
                }
                
                // Element entirely below page boundary - move to next page
                else -> {
                    overflowElements.add(
                        adjustElementPositionForNextPage(element, pageHeightDp)
                    )
                    
                    // If this is a container, move all its children to overflow too
                    if (element is ResumeElement.ContainerElement) {
                        element.children.forEach { childId ->
                            page.elements.find { it.id == childId }?.let { child ->
                                overflowElements.add(adjustElementPositionForNextPage(child, pageHeightDp))
                            }
                        }
                    }
                }
            }
        }
        
        // Build current page (no need for post-processing since children are handled with their containers)
        val currentPage = page.copy(
            elements = currentPageElements,
            paginationInfo = PagePaginationInfo(
                pageIndex = startingPageIndex,
                isOverflowPage = page.isOverflowPage,
                continuedFromPreviousPage = page.paginationInfo?.continuedFromPreviousPage ?: emptyList(),
                continuesOnNextPage = overflowElements.map { it.id }
            )
        )
        
        val resultPages = mutableListOf(currentPage)
        
        // Recursively paginate overflow content
        if (overflowElements.isNotEmpty()) {
            val overflowPage = ResumePage(
                elements = overflowElements,
                backgroundColor = page.backgroundColor,
                layoutMode = page.layoutMode,
                paginationInfo = PagePaginationInfo(
                    pageIndex = startingPageIndex + 1,
                    isOverflowPage = true,
                    continuedFromPreviousPage = overflowElements
                        .filter { el -> newLinkedGroups.any { it.linkedElementIds.contains(el.id) } }
                        .map { it.id }
                )
            )
            
            val recursiveResult = paginateSinglePage(
                page = overflowPage,
                startingPageIndex = startingPageIndex + 1,
                existingGroups = existingGroups,
                allElements = allElements,
                recursionDepth = recursionDepth + 1
            )
            
            resultPages.addAll(recursiveResult.pages)
            newLinkedGroups.addAll(recursiveResult.linkedGroups)
        }
        
        return SinglePageResult(
            pages = resultPages,
            linkedGroups = newLinkedGroups,
            contentHeightDp = contentHeight
        )
    }
    
    /**
     * Split an element at item boundaries
     * 
     * @param element Element to split
     * @param availableHeightDp Height available on current page
     * @param availableWidthDp Width available for element
     * @param allElements All elements for container child lookup
     * @return SplitResult with first and second parts
     */
    private fun splitElement(
        element: ResumeElement,
        availableHeightDp: Float,
        availableWidthDp: Float,
        allElements: List<ResumeElement> = emptyList()
    ): SplitResult {
        return when (element) {
            is ResumeElement.WorkExperienceElement -> 
                splitWorkExperience(element, availableHeightDp, availableWidthDp)
            is ResumeElement.EducationElement -> 
                splitEducation(element, availableHeightDp, availableWidthDp)
            is ResumeElement.SkillElement -> 
                splitSkill(element, availableHeightDp, availableWidthDp)
            is ResumeElement.ProjectElement -> 
                splitProject(element, availableHeightDp, availableWidthDp)
            is ResumeElement.CertificationElement -> 
                splitCertification(element, availableHeightDp, availableWidthDp)
            is ResumeElement.LanguageElement -> 
                splitLanguage(element, availableHeightDp, availableWidthDp)
            is ResumeElement.ContainerElement -> 
                splitContainer(element, availableHeightDp, availableWidthDp, allElements)
            // Non-splittable elements - keep on current page, no overflow
            else -> SplitResult(
                firstPart = element,
                secondPart = null,
                strategy = SplitStrategy.NO_SPLIT
            )
        }
    }
    
    /**
     * Split WorkExperienceElement at item boundary
     */
    private fun splitWorkExperience(
        element: ResumeElement.WorkExperienceElement,
        availableHeightDp: Float,
        availableWidthDp: Float
    ): SplitResult {
        if (element.items.isEmpty()) {
            return SplitResult(element, null, SplitStrategy.AT_ITEM_BOUNDARY)
        }
        
        val itemHeights = heightCalculator.calculateWorkExperienceItemHeights(element, availableWidthDp)
        val padding = element.padding ?: Padding()
        val availableContentHeight = availableHeightDp - padding.top - padding.bottom
        
        // Find split point
        var splitIndex = 0
        for (itemHeight in itemHeights) {
            if (itemHeight.cumulativeHeightDp > availableContentHeight) {
                break
            }
            splitIndex = itemHeight.itemIndex + 1
        }
        
        // Respect minimum items per page
        if (splitIndex < config.minItemsPerPage && element.items.size > config.minItemsPerPage) {
            splitIndex = config.minItemsPerPage
        }
        
        // If nothing fits, move entire element
        if (splitIndex == 0) {
            return SplitResult(null, element, SplitStrategy.NO_SPLIT)
        }
        
        // If everything fits, no split needed
        if (splitIndex >= element.items.size) {
            return SplitResult(element, null, SplitStrategy.AT_ITEM_BOUNDARY)
        }
        
        // Create split elements
        val firstPart = element.copy(
            items = element.items.take(splitIndex)
        )
        
        val secondPart = element.copy(
            id = UUID.randomUUID().toString(),
            position = element.position.copy(row = 0),
            items = element.items.drop(splitIndex)
        )
        
        return SplitResult(firstPart, secondPart, SplitStrategy.AT_ITEM_BOUNDARY, splitIndex)
    }
    
    /**
     * Split EducationElement at item boundary
     */
    private fun splitEducation(
        element: ResumeElement.EducationElement,
        availableHeightDp: Float,
        availableWidthDp: Float
    ): SplitResult {
        if (element.items.isEmpty()) {
            return SplitResult(element, null, SplitStrategy.AT_ITEM_BOUNDARY)
        }
        
        val itemHeights = heightCalculator.calculateEducationItemHeights(element, availableWidthDp)
        val padding = element.padding ?: Padding()
        val availableContentHeight = availableHeightDp - padding.top - padding.bottom
        
        var splitIndex = 0
        for (itemHeight in itemHeights) {
            if (itemHeight.cumulativeHeightDp > availableContentHeight) {
                break
            }
            splitIndex = itemHeight.itemIndex + 1
        }
        
        if (splitIndex < config.minItemsPerPage && element.items.size > config.minItemsPerPage) {
            splitIndex = config.minItemsPerPage
        }
        
        if (splitIndex == 0) {
            return SplitResult(null, element, SplitStrategy.NO_SPLIT)
        }
        
        if (splitIndex >= element.items.size) {
            return SplitResult(element, null, SplitStrategy.AT_ITEM_BOUNDARY)
        }
        
        val firstPart = element.copy(items = element.items.take(splitIndex))
        val secondPart = element.copy(
            id = UUID.randomUUID().toString(),
            position = element.position.copy(row = 0),
            items = element.items.drop(splitIndex)
        )
        
        return SplitResult(firstPart, secondPart, SplitStrategy.AT_ITEM_BOUNDARY, splitIndex)
    }
    
    /**
     * Split SkillElement at item boundary
     */
    private fun splitSkill(
        element: ResumeElement.SkillElement,
        availableHeightDp: Float,
        availableWidthDp: Float
    ): SplitResult {
        if (element.items.isEmpty()) {
            return SplitResult(element, null, SplitStrategy.AT_ITEM_BOUNDARY)
        }
        
        val itemHeights = heightCalculator.calculateSkillItemHeights(element, availableWidthDp)
        val padding = element.padding ?: Padding()
        val availableContentHeight = availableHeightDp - padding.top - padding.bottom
        
        var splitIndex = 0
        for (itemHeight in itemHeights) {
            if (itemHeight.cumulativeHeightDp > availableContentHeight) {
                break
            }
            splitIndex = itemHeight.itemIndex + 1
        }
        
        if (splitIndex == 0) {
            return SplitResult(null, element, SplitStrategy.NO_SPLIT)
        }
        
        if (splitIndex >= element.items.size) {
            return SplitResult(element, null, SplitStrategy.AT_ITEM_BOUNDARY)
        }
        
        val firstPart = element.copy(items = element.items.take(splitIndex))
        val secondPart = element.copy(
            id = UUID.randomUUID().toString(),
            position = element.position.copy(row = 0),
            items = element.items.drop(splitIndex)
        )
        
        return SplitResult(firstPart, secondPart, SplitStrategy.AT_ITEM_BOUNDARY, splitIndex)
    }
    
    /**
     * Split ProjectElement at item boundary
     */
    private fun splitProject(
        element: ResumeElement.ProjectElement,
        availableHeightDp: Float,
        availableWidthDp: Float
    ): SplitResult {
        if (element.items.isEmpty()) {
            return SplitResult(element, null, SplitStrategy.AT_ITEM_BOUNDARY)
        }
        
        val itemHeights = heightCalculator.calculateProjectItemHeights(element, availableWidthDp)
        val padding = element.padding ?: Padding()
        val availableContentHeight = availableHeightDp - padding.top - padding.bottom
        
        var splitIndex = 0
        for (itemHeight in itemHeights) {
            if (itemHeight.cumulativeHeightDp > availableContentHeight) {
                break
            }
            splitIndex = itemHeight.itemIndex + 1
        }
        
        if (splitIndex == 0) {
            return SplitResult(null, element, SplitStrategy.NO_SPLIT)
        }
        
        if (splitIndex >= element.items.size) {
            return SplitResult(element, null, SplitStrategy.AT_ITEM_BOUNDARY)
        }
        
        val firstPart = element.copy(items = element.items.take(splitIndex))
        val secondPart = element.copy(
            id = UUID.randomUUID().toString(),
            position = element.position.copy(row = 0),
            items = element.items.drop(splitIndex)
        )
        
        return SplitResult(firstPart, secondPart, SplitStrategy.AT_ITEM_BOUNDARY, splitIndex)
    }
    
    /**
     * Split CertificationElement at item boundary
     */
    private fun splitCertification(
        element: ResumeElement.CertificationElement,
        availableHeightDp: Float,
        availableWidthDp: Float
    ): SplitResult {
        if (element.items.isEmpty()) {
            return SplitResult(element, null, SplitStrategy.AT_ITEM_BOUNDARY)
        }
        
        val itemHeights = heightCalculator.calculateCertificationItemHeights(element, availableWidthDp)
        val padding = element.padding ?: Padding()
        val availableContentHeight = availableHeightDp - padding.top - padding.bottom
        
        var splitIndex = 0
        for (itemHeight in itemHeights) {
            if (itemHeight.cumulativeHeightDp > availableContentHeight) {
                break
            }
            splitIndex = itemHeight.itemIndex + 1
        }
        
        if (splitIndex == 0) {
            return SplitResult(null, element, SplitStrategy.NO_SPLIT)
        }
        
        if (splitIndex >= element.items.size) {
            return SplitResult(element, null, SplitStrategy.AT_ITEM_BOUNDARY)
        }
        
        val firstPart = element.copy(items = element.items.take(splitIndex))
        val secondPart = element.copy(
            id = UUID.randomUUID().toString(),
            position = element.position.copy(row = 0),
            items = element.items.drop(splitIndex)
        )
        
        return SplitResult(firstPart, secondPart, SplitStrategy.AT_ITEM_BOUNDARY, splitIndex)
    }
    
    /**
     * Split LanguageElement at item boundary
     */
    private fun splitLanguage(
        element: ResumeElement.LanguageElement,
        availableHeightDp: Float,
        availableWidthDp: Float
    ): SplitResult {
        if (element.items.isEmpty()) {
            return SplitResult(element, null, SplitStrategy.AT_ITEM_BOUNDARY)
        }
        
        val itemHeights = heightCalculator.calculateLanguageItemHeights(element, availableWidthDp)
        val padding = element.padding ?: Padding()
        val availableContentHeight = availableHeightDp - padding.top - padding.bottom
        
        var splitIndex = 0
        for (itemHeight in itemHeights) {
            if (itemHeight.cumulativeHeightDp > availableContentHeight) {
                break
            }
            splitIndex = itemHeight.itemIndex + 1
        }
        
        if (splitIndex == 0) {
            return SplitResult(null, element, SplitStrategy.NO_SPLIT)
        }
        
        if (splitIndex >= element.items.size) {
            return SplitResult(element, null, SplitStrategy.AT_ITEM_BOUNDARY)
        }
        
        val firstPart = element.copy(items = element.items.take(splitIndex))
        val secondPart = element.copy(
            id = UUID.randomUUID().toString(),
            position = element.position.copy(row = 0),
            items = element.items.drop(splitIndex)
        )
        
        return SplitResult(firstPart, secondPart, SplitStrategy.AT_ITEM_BOUNDARY, splitIndex)
    }
    
    /**
     * Split ContainerElement at child boundary for vertical layout containers
     */
    private fun splitContainer(
        element: ResumeElement.ContainerElement,
        availableHeightDp: Float,
        availableWidthDp: Float,
        allElements: List<ResumeElement> = emptyList()
    ): SplitResult {
        // Only split vertical layout containers
        if (element.effectiveLayoutMode != LayoutMode.VERTICAL) {
            // Non-vertical containers: keep on current page, don't overflow
            return SplitResult(
                firstPart = element,
                secondPart = null,
                strategy = SplitStrategy.NO_SPLIT
            )
        }
        
        // If no children, keep container as-is
        if (element.children.isEmpty()) {
            return SplitResult(
                firstPart = element,
                secondPart = null,
                strategy = SplitStrategy.NO_SPLIT
            )
        }
        
        val padding = element.padding
        val contentWidthDp = availableWidthDp - padding.left - padding.right
        val availableContentHeight = availableHeightDp - padding.top - padding.bottom
        
        // Calculate cumulative heights of children
        var cumulativeHeight = 0f
        var splitIndex = 0
        val childSpacing = 8f // Default spacing between children
        
        for ((index, childId) in element.children.withIndex()) {
            val child = allElements.find { it.id == childId } ?: continue
            val childHeight = heightCalculator.calculateHeight(child, contentWidthDp)
            
            // Check if this child would exceed available height
            val heightWithChild = cumulativeHeight + childHeight + (if (index > 0) childSpacing else 0f)
            
            if (heightWithChild > availableContentHeight) {
                break
            }
            
            cumulativeHeight = heightWithChild
            splitIndex = index + 1
        }
        
        // If nothing fits, move entire container to next page
        if (splitIndex == 0) {
            return SplitResult(
                firstPart = null,
                secondPart = element,
                strategy = SplitStrategy.NO_SPLIT
            )
        }
        
        // If everything fits, no split needed
        if (splitIndex >= element.children.size) {
            return SplitResult(
                firstPart = element,
                secondPart = null,
                strategy = SplitStrategy.AT_ITEM_BOUNDARY
            )
        }
        
        // Split the children list
        val firstPartChildren = element.children.take(splitIndex)
        val secondPartChildren = element.children.drop(splitIndex)
        
        // Create first container with children that fit
        val firstPart = element.copy(
            children = firstPartChildren
        )
        
        // Create second container with remaining children (new ID for tracking)
        val secondPart = element.copy(
            id = UUID.randomUUID().toString(),
            position = element.position.copy(row = 0, cachedHeightDp = null),
            children = secondPartChildren
        )
        
        return SplitResult(
            firstPart = firstPart,
            secondPart = secondPart,
            strategy = SplitStrategy.AT_ITEM_BOUNDARY,
            splitAtIndex = splitIndex,
            overflowChildIds = secondPartChildren // Include child IDs that need to move to next page
        )
    }
    
    // ========================================================================
    // Helper Methods
    // ========================================================================
    
    /**
     * Calculate element height, using cached values when available
     */
    private fun calculateElementHeight(
        element: ResumeElement,
        widthDp: Float,
        allElements: List<ResumeElement>
    ): Float {
        // Use cached height if available
        element.position.cachedHeightDp?.let { return it }
        
        // For containers, calculate with children
        if (element is ResumeElement.ContainerElement) {
            return heightCalculator.calculateContainerHeightWithChildren(element, allElements, widthDp)
        }
        
        return heightCalculator.calculateHeight(element, widthDp)
    }
    
    /**
     * Adjust element position for next page (move to top)
     */
    private fun adjustElementPositionForNextPage(
        element: ResumeElement,
        offsetFromCurrentPage: Float = 0f
    ): ResumeElement {
        val newRow = if (offsetFromCurrentPage > 0) {
            // Calculate relative position from page boundary
            val currentTopDp = element.position.row * cellSizeDp
            val offsetDp = currentTopDp - offsetFromCurrentPage
            (offsetDp / cellSizeDp).toInt().coerceAtLeast(0)
        } else {
            0 // Start at top of new page
        }
        
        val newPosition = element.position.copy(row = newRow)
        
        return when (element) {
            is ResumeElement.TextElement -> element.copy(position = newPosition)
            is ResumeElement.ImageElement -> element.copy(position = newPosition)
            is ResumeElement.ShapeElement -> element.copy(position = newPosition)
            is ResumeElement.ChartElement -> element.copy(position = newPosition)
            is ResumeElement.ContainerElement -> element.copy(position = newPosition)
            is ResumeElement.IconElement -> element.copy(position = newPosition)
            is ResumeElement.ContactElement -> element.copy(position = newPosition)
            is ResumeElement.WorkExperienceElement -> element.copy(position = newPosition)
            is ResumeElement.EducationElement -> element.copy(position = newPosition)
            is ResumeElement.SkillElement -> element.copy(position = newPosition)
            is ResumeElement.ProjectElement -> element.copy(position = newPosition)
            is ResumeElement.CertificationElement -> element.copy(position = newPosition)
            is ResumeElement.LanguageElement -> element.copy(position = newPosition)
        }
    }
    
    /**
     * Calculate suggested split points for an element
     */
    private fun calculateSplitPoints(
        element: ResumeElement,
        availableHeightDp: Float,
        availableWidthDp: Float
    ): List<SuggestedSplitPoint> {
        val itemHeights: List<ItemHeightInfo> = when (element) {
            is ResumeElement.WorkExperienceElement -> 
                heightCalculator.calculateWorkExperienceItemHeights(element, availableWidthDp)
            is ResumeElement.EducationElement -> 
                heightCalculator.calculateEducationItemHeights(element, availableWidthDp)
            is ResumeElement.ProjectElement -> 
                heightCalculator.calculateProjectItemHeights(element, availableWidthDp)
            is ResumeElement.CertificationElement -> 
                heightCalculator.calculateCertificationItemHeights(element, availableWidthDp)
            is ResumeElement.LanguageElement -> 
                heightCalculator.calculateLanguageItemHeights(element, availableWidthDp)
            is ResumeElement.SkillElement -> 
                heightCalculator.calculateSkillItemHeights(element, availableWidthDp)
            else -> return emptyList()
        }
        
        val totalHeight = itemHeights.lastOrNull()?.cumulativeHeightDp ?: return emptyList()
        
        return itemHeights
            .filter { it.cumulativeHeightDp <= availableHeightDp }
            .map { itemHeight ->
                SuggestedSplitPoint(
                    elementId = element.id,
                    itemIndex = itemHeight.itemIndex + 1,
                    heightSavedDp = totalHeight - itemHeight.cumulativeHeightDp
                )
            }
    }
    
    /**
     * Get all elements from a resume (flattened across all pages)
     */
    private fun getAllElementsFromResume(resume: GridResume): List<ResumeElement> {
        return resume.pages.flatMap { it.elements }
    }
    
    /**
     * Merge existing and new linked element groups
     */
    private fun mergeLinkedGroups(
        existing: List<LinkedElementGroup>,
        new: List<LinkedElementGroup>
    ): List<LinkedElementGroup> {
        val merged = existing.toMutableList()
        
        new.forEach { newGroup ->
            // Check if source element already has a group
            val existingIndex = merged.indexOfFirst { it.sourceElementId == newGroup.sourceElementId }
            if (existingIndex >= 0) {
                // Merge linked element IDs
                val existingGroup = merged[existingIndex]
                merged[existingIndex] = existingGroup.copy(
                    linkedElementIds = (existingGroup.linkedElementIds + newGroup.linkedElementIds).distinct()
                )
            } else {
                merged.add(newGroup)
            }
        }
        
        return merged
    }
}

/**
 * Result of paginating a single page
 */
private data class SinglePageResult(
    val pages: List<ResumePage>,
    val linkedGroups: List<LinkedElementGroup>,
    val contentHeightDp: Float
)
