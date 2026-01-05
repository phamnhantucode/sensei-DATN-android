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
                            
                            // For container splits: handle children including newly created split elements
                            if (element is ResumeElement.ContainerElement) {
                                val firstPartChildIds = (splitResult.firstPart as? ResumeElement.ContainerElement)?.children ?: emptyList()
                                
                                // Add first-part children to current page
                                // IMPORTANT: Check newChildElements FIRST because split parts may have same ID as original
                                firstPartChildIds.forEach { childId ->
                                    val child = splitResult.newChildElements.find { it.id == childId }
                                        ?: page.elements.find { it.id == childId }
                                    child?.let { currentPageElements.add(it) }
                                }
                                
                                // Add overflow children to overflow page
                                splitResult.overflowChildIds.forEach { childId ->
                                    val child = splitResult.newChildElements.find { it.id == childId }
                                        ?: page.elements.find { it.id == childId }
                                    child?.let { overflowElements.add(adjustElementPositionForNextPage(it)) }
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
     * 
     * Enhanced logic: Even if no items fit in the remaining space on current page,
     * we check if individual items can fit on the next page. This prevents the entire
     * section from moving to page 2 when items could be split across pages.
     * 
     * Key insight: If available space is >= 50% of page height and first item doesn't fit,
     * we should still try to keep the element on this page - it will overflow but the 
     * pagination will handle it by creating continuation on next page.
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
        val fullPageContentHeight = pageHeightDp - padding.top - padding.bottom
        
        // Find split point - how many items fit in available space
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
        
        // If nothing fits in available space
        if (splitIndex == 0) {
            // Check if we have more than 1 item and first item would fit on a full page
            if (element.items.size > 1) {
                val firstItemHeight = itemHeights.firstOrNull()?.cumulativeHeightDp ?: 0f
                
                // If available space is less than 15% of page, move entire element
                // This avoids leaving orphaned section headers with tiny content
                val minUsefulSpaceRatio = 0.15f
                if (availableContentHeight < fullPageContentHeight * minUsefulSpaceRatio) {
                    return SplitResult(null, element, SplitStrategy.NO_SPLIT)
                }
                
                // If first item is larger than full page, we can't split anyway
                if (firstItemHeight > fullPageContentHeight) {
                    return SplitResult(null, element, SplitStrategy.NO_SPLIT)
                }
            }
            
            // Move entire element to next page
            return SplitResult(null, element, SplitStrategy.NO_SPLIT)
        }
        
        // If everything fits, no split needed
        if (splitIndex >= element.items.size) {
            return SplitResult(element, null, SplitStrategy.AT_ITEM_BOUNDARY)
        }
        
        // Create split elements - both parts get new IDs to distinguish from original
        val firstPart = element.copy(
            id = UUID.randomUUID().toString(),
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
        val fullPageContentHeight = pageHeightDp - padding.top - padding.bottom
        
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
        
        // If nothing fits in available space
        if (splitIndex == 0) {
            if (element.items.size > 1) {
                val firstItemHeight = itemHeights.firstOrNull()?.cumulativeHeightDp ?: 0f
                val minUsefulSpaceRatio = 0.15f
                if (availableContentHeight < fullPageContentHeight * minUsefulSpaceRatio) {
                    return SplitResult(null, element, SplitStrategy.NO_SPLIT)
                }
                if (firstItemHeight > fullPageContentHeight) {
                    return SplitResult(null, element, SplitStrategy.NO_SPLIT)
                }
            }
            return SplitResult(null, element, SplitStrategy.NO_SPLIT)
        }
        
        if (splitIndex >= element.items.size) {
            return SplitResult(element, null, SplitStrategy.AT_ITEM_BOUNDARY)
        }
        
        val firstPart = element.copy(
            id = UUID.randomUUID().toString(),
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
        val fullPageContentHeight = pageHeightDp - padding.top - padding.bottom
        
        var splitIndex = 0
        for (itemHeight in itemHeights) {
            if (itemHeight.cumulativeHeightDp > availableContentHeight) {
                break
            }
            splitIndex = itemHeight.itemIndex + 1
        }
        
        // If nothing fits in available space
        if (splitIndex == 0) {
            if (element.items.size > 1) {
                val firstItemHeight = itemHeights.firstOrNull()?.cumulativeHeightDp ?: 0f
                val minUsefulSpaceRatio = 0.15f
                if (availableContentHeight < fullPageContentHeight * minUsefulSpaceRatio) {
                    return SplitResult(null, element, SplitStrategy.NO_SPLIT)
                }
                if (firstItemHeight > fullPageContentHeight) {
                    return SplitResult(null, element, SplitStrategy.NO_SPLIT)
                }
            }
            return SplitResult(null, element, SplitStrategy.NO_SPLIT)
        }
        
        if (splitIndex >= element.items.size) {
            return SplitResult(element, null, SplitStrategy.AT_ITEM_BOUNDARY)
        }
        
        val firstPart = element.copy(
            id = UUID.randomUUID().toString(),
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
        val fullPageContentHeight = pageHeightDp - padding.top - padding.bottom
        
        var splitIndex = 0
        for (itemHeight in itemHeights) {
            if (itemHeight.cumulativeHeightDp > availableContentHeight) {
                break
            }
            splitIndex = itemHeight.itemIndex + 1
        }
        
        // If nothing fits in available space
        if (splitIndex == 0) {
            if (element.items.size > 1) {
                val firstItemHeight = itemHeights.firstOrNull()?.cumulativeHeightDp ?: 0f
                val minUsefulSpaceRatio = 0.15f
                if (availableContentHeight < fullPageContentHeight * minUsefulSpaceRatio) {
                    return SplitResult(null, element, SplitStrategy.NO_SPLIT)
                }
                if (firstItemHeight > fullPageContentHeight) {
                    return SplitResult(null, element, SplitStrategy.NO_SPLIT)
                }
            }
            return SplitResult(null, element, SplitStrategy.NO_SPLIT)
        }
        
        if (splitIndex >= element.items.size) {
            return SplitResult(element, null, SplitStrategy.AT_ITEM_BOUNDARY)
        }
        
        val firstPart = element.copy(
            id = UUID.randomUUID().toString(),
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
        val fullPageContentHeight = pageHeightDp - padding.top - padding.bottom
        
        var splitIndex = 0
        for (itemHeight in itemHeights) {
            if (itemHeight.cumulativeHeightDp > availableContentHeight) {
                break
            }
            splitIndex = itemHeight.itemIndex + 1
        }
        
        // If nothing fits in available space
        if (splitIndex == 0) {
            if (element.items.size > 1) {
                val firstItemHeight = itemHeights.firstOrNull()?.cumulativeHeightDp ?: 0f
                val minUsefulSpaceRatio = 0.15f
                if (availableContentHeight < fullPageContentHeight * minUsefulSpaceRatio) {
                    return SplitResult(null, element, SplitStrategy.NO_SPLIT)
                }
                if (firstItemHeight > fullPageContentHeight) {
                    return SplitResult(null, element, SplitStrategy.NO_SPLIT)
                }
            }
            return SplitResult(null, element, SplitStrategy.NO_SPLIT)
        }
        
        if (splitIndex >= element.items.size) {
            return SplitResult(element, null, SplitStrategy.AT_ITEM_BOUNDARY)
        }
        
        val firstPart = element.copy(
            id = UUID.randomUUID().toString(),
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
        val fullPageContentHeight = pageHeightDp - padding.top - padding.bottom
        
        var splitIndex = 0
        for (itemHeight in itemHeights) {
            if (itemHeight.cumulativeHeightDp > availableContentHeight) {
                break
            }
            splitIndex = itemHeight.itemIndex + 1
        }
        
        // If nothing fits in available space
        if (splitIndex == 0) {
            if (element.items.size > 1) {
                val firstItemHeight = itemHeights.firstOrNull()?.cumulativeHeightDp ?: 0f
                val minUsefulSpaceRatio = 0.15f
                if (availableContentHeight < fullPageContentHeight * minUsefulSpaceRatio) {
                    return SplitResult(null, element, SplitStrategy.NO_SPLIT)
                }
                if (firstItemHeight > fullPageContentHeight) {
                    return SplitResult(null, element, SplitStrategy.NO_SPLIT)
                }
            }
            return SplitResult(null, element, SplitStrategy.NO_SPLIT)
        }
        
        if (splitIndex >= element.items.size) {
            return SplitResult(element, null, SplitStrategy.AT_ITEM_BOUNDARY)
        }
        
        val firstPart = element.copy(
            id = UUID.randomUUID().toString(),
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
     * Split ContainerElement at child boundary for vertical layout containers
     * 
     * Enhanced: If a child element is splittable (like WorkExperienceElement),
     * we'll try to split it at item boundaries instead of moving it entirely.
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
        val fullPageContentHeight = pageHeightDp - padding.top - padding.bottom
        
        // Calculate cumulative heights of children and find split point
        var cumulativeHeight = 0f
        var splitIndex = 0
        val childSpacing = 8f // Default spacing between children
        
        // Track which child needs internal splitting and the split result
        var childToSplit: ResumeElement? = null
        var childSplitResult: SplitResult? = null
        var childToSplitIndex = -1
        
        for ((index, childId) in element.children.withIndex()) {
            val child = allElements.find { it.id == childId } ?: continue
            val childHeight = heightCalculator.calculateHeight(child, contentWidthDp)
            val spacingBefore = if (index > 0) childSpacing else 0f
            
            // Check if this child would exceed available height
            val heightWithChild = cumulativeHeight + childHeight + spacingBefore
            
            if (heightWithChild > availableContentHeight) {
                // This child doesn't fully fit - check if it's splittable
                val remainingHeight = availableContentHeight - cumulativeHeight - spacingBefore
                
                if (remainingHeight > 0 && isSplittableElement(child)) {
                    // Try to split this child element internally
                    val internalSplitResult = splitElement(child, remainingHeight, contentWidthDp, allElements)
                    
                    if (internalSplitResult.firstPart != null && internalSplitResult.secondPart != null) {
                        // Successfully split the child - include first part on this page
                        childToSplit = child
                        childSplitResult = internalSplitResult
                        childToSplitIndex = index
                        splitIndex = index + 1 // Include this (partially) in first part
                    }
                }
                break
            }
            
            cumulativeHeight = heightWithChild
            splitIndex = index + 1
        }
        
        // If nothing fits and no child was split, move entire container to next page
        if (splitIndex == 0 && childSplitResult == null) {
            // Check if available space is too small (less than 15% of page)
            val minUsefulSpaceRatio = 0.15f
            if (availableContentHeight < fullPageContentHeight * minUsefulSpaceRatio) {
                return SplitResult(
                    firstPart = null,
                    secondPart = element,
                    strategy = SplitStrategy.NO_SPLIT
                )
            }
            
            return SplitResult(
                firstPart = null,
                secondPart = element,
                strategy = SplitStrategy.NO_SPLIT
            )
        }
        
        // If everything fits, no split needed
        if (splitIndex >= element.children.size && childSplitResult == null) {
            return SplitResult(
                firstPart = element,
                secondPart = null,
                strategy = SplitStrategy.AT_ITEM_BOUNDARY
            )
        }
        
        // Build the split result
        val firstPartChildren: MutableList<String>
        val secondPartChildren: MutableList<String>
        val newElements = mutableListOf<ResumeElement>() // New elements created from splitting
        
        if (childSplitResult != null && childToSplitIndex >= 0) {
            // We have an internally split child
            firstPartChildren = element.children.take(childToSplitIndex).toMutableList()
            
            // Add the first part of the split child
            childSplitResult.firstPart?.let { 
                firstPartChildren.add(it.id)
                newElements.add(it)
            }
            
            // Second part starts with the remainder of the split child
            secondPartChildren = mutableListOf<String>()
            childSplitResult.secondPart?.let {
                secondPartChildren.add(it.id)
                newElements.add(it)
            }
            
            // Add remaining children after the split child
            secondPartChildren.addAll(element.children.drop(childToSplitIndex + 1))
        } else {
            // Simple split at child boundary
            firstPartChildren = element.children.take(splitIndex).toMutableList()
            secondPartChildren = element.children.drop(splitIndex).toMutableList()
        }
        
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
            overflowChildIds = secondPartChildren,
            newChildElements = newElements // Include newly created elements from child splitting
        )
    }
    
    /**
     * Check if an element type supports internal splitting at item boundaries
     */
    private fun isSplittableElement(element: ResumeElement): Boolean {
        return when (element) {
            is ResumeElement.WorkExperienceElement -> element.items.size > 1
            is ResumeElement.EducationElement -> element.items.size > 1
            is ResumeElement.SkillElement -> element.items.size > 1
            is ResumeElement.ProjectElement -> element.items.size > 1
            is ResumeElement.CertificationElement -> element.items.size > 1
            is ResumeElement.LanguageElement -> element.items.size > 1
            else -> false
        }
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
