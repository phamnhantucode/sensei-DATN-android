package com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.pagination

import android.content.Context
import android.graphics.Paint
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.compose.ui.text.font.FontWeight
import com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.models.*

/**
 * Service for calculating the actual rendered height of resume elements
 * 
 * Used by the pagination engine to determine where to split content across pages.
 * Heights are calculated in dp to match the grid coordinate system.
 */
class ElementHeightCalculator(
    private val context: Context,
    private val gridConfig: GridConfig
) {
    private val cellSizeDp = gridConfig.cellSizeDp
    private val pageWidthDp = gridConfig.columns * cellSizeDp
    private val pageHeightDp = gridConfig.rows * cellSizeDp
    
    // Text paint for measuring text dimensions
    private val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG)
    
    /**
     * Calculate the actual rendered height of any element in dp
     * 
     * @param element The element to measure
     * @param availableWidthDp Available width for the element (for text wrapping)
     * @return Height in dp
     */
    fun calculateHeight(element: ResumeElement, availableWidthDp: Float): Float {
        // If element uses FIXED height mode, use rowSpan
        if (element.position.heightMode == SizeMode.FIXED) {
            return element.position.rowSpan * cellSizeDp
        }
        
        // For WRAP_CONTENT, use cached height if available
        element.position.cachedHeightDp?.let { return it }
        
        // Otherwise calculate based on element type
        return when (element) {
            is ResumeElement.TextElement -> calculateTextHeight(element, availableWidthDp)
            is ResumeElement.WorkExperienceElement -> calculateWorkExperienceHeight(element, availableWidthDp)
            is ResumeElement.EducationElement -> calculateEducationHeight(element, availableWidthDp)
            is ResumeElement.SkillElement -> calculateSkillHeight(element, availableWidthDp)
            is ResumeElement.ProjectElement -> calculateProjectHeight(element, availableWidthDp)
            is ResumeElement.CertificationElement -> calculateCertificationHeight(element, availableWidthDp)
            is ResumeElement.LanguageElement -> calculateLanguageHeight(element, availableWidthDp)
            is ResumeElement.ContactElement -> calculateContactHeight(element, availableWidthDp)
            is ResumeElement.ContainerElement -> calculateContainerHeight(element, availableWidthDp)
            is ResumeElement.ImageElement -> element.position.rowSpan * cellSizeDp
            is ResumeElement.ShapeElement -> element.customHeightDp ?: (element.position.rowSpan * cellSizeDp)
            is ResumeElement.ChartElement -> element.position.rowSpan * cellSizeDp
            is ResumeElement.IconElement -> element.position.rowSpan * cellSizeDp
        }
    }
    
    /**
     * Calculate height for a text element
     */
    private fun calculateTextHeight(element: ResumeElement.TextElement, availableWidthDp: Float): Float {
        if (element.content.isBlank()) {
            return element.position.rowSpan * cellSizeDp
        }
        
        val padding = element.padding ?: Padding()
        val contentWidth = availableWidthDp - padding.left - padding.right
        
        setupTextPaint(element.textStyle)
        
        val layout = StaticLayout.Builder.obtain(
            element.content,
            0,
            element.content.length,
            textPaint,
            (contentWidth * context.resources.displayMetrics.density).toInt().coerceAtLeast(1)
        )
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setLineSpacing(0f, 1f)
            .setIncludePad(true)
            .build()
        
        val textHeightPx = layout.height.toFloat()
        val textHeightDp = textHeightPx / context.resources.displayMetrics.density
        
        return textHeightDp + padding.top + padding.bottom
    }
    
    /**
     * Calculate height for work experience element with itemized breakdown
     */
    fun calculateWorkExperienceHeight(
        element: ResumeElement.WorkExperienceElement,
        availableWidthDp: Float
    ): Float {
        val itemHeights = calculateWorkExperienceItemHeights(element, availableWidthDp)
        val padding = element.padding ?: Padding()
        
        val contentHeight = itemHeights.lastOrNull()?.cumulativeHeightDp ?: 0f
        return contentHeight + padding.top + padding.bottom
    }
    
    /**
     * Calculate height breakdown for each work experience item
     * Returns cumulative heights for split point calculation
     */
    fun calculateWorkExperienceItemHeights(
        element: ResumeElement.WorkExperienceElement,
        availableWidthDp: Float
    ): List<ItemHeightInfo> {
        val padding = element.padding ?: Padding()
        val contentWidth = availableWidthDp - padding.left - padding.right
        var cumulativeHeight = 0f
        
        return element.items.mapIndexed { index, item ->
            val itemHeight = calculateSingleWorkExperienceItemHeight(item, element, contentWidth)
            cumulativeHeight += itemHeight
            
            // Add spacing between items (except for last item)
            if (index < element.items.size - 1) {
                cumulativeHeight += element.spacing
            }
            
            ItemHeightInfo(
                itemId = item.id,
                itemIndex = index,
                heightDp = itemHeight,
                cumulativeHeightDp = cumulativeHeight
            )
        }
    }
    
    /**
     * Calculate height for a single work experience item
     */
    private fun calculateSingleWorkExperienceItemHeight(
        item: WorkExperienceItem,
        element: ResumeElement.WorkExperienceElement,
        contentWidthDp: Float
    ): Float {
        var height = 0f
        
        // Job title height
        setupTextPaint(element.titleStyle)
        height += measureTextHeight(item.jobTitle, contentWidthDp)
        height += element.itemSpacing
        
        // Company height
        setupTextPaint(element.companyStyle)
        height += measureTextHeight(item.company, contentWidthDp)
        height += element.itemSpacing
        
        // Date line (if shown)
        if (element.showDates && (item.startDate.isNotEmpty() || item.endDate.isNotEmpty())) {
            setupTextPaint(element.dateStyle)
            val dateText = buildDateText(item.startDate, item.endDate, item.isCurrentRole, element.dateSeparator)
            height += measureTextHeight(dateText, contentWidthDp)
            height += element.itemSpacing
        }
        
        // Location (if shown)
        if (element.showLocation && item.location.isNotEmpty()) {
            setupTextPaint(element.locationStyle)
            height += measureTextHeight(item.location, contentWidthDp)
            height += element.itemSpacing
        }
        
        // Responsibilities
        item.responsibilities.forEachIndexed { index, responsibility ->
            setupTextPaint(element.responsibilityStyle)
            val bulletWidth = 16f // Approximate bullet width in dp
            height += measureTextHeight(responsibility.text, contentWidthDp - bulletWidth)
            
            if (index < item.responsibilities.size - 1) {
                height += element.responsibilitySpacing
            }
        }
        
        return height
    }
    
    /**
     * Calculate height for education element
     */
    fun calculateEducationHeight(
        element: ResumeElement.EducationElement,
        availableWidthDp: Float
    ): Float {
        val itemHeights = calculateEducationItemHeights(element, availableWidthDp)
        val padding = element.padding ?: Padding()
        
        val contentHeight = itemHeights.lastOrNull()?.cumulativeHeightDp ?: 0f
        return contentHeight + padding.top + padding.bottom
    }
    
    /**
     * Calculate height breakdown for each education item
     */
    fun calculateEducationItemHeights(
        element: ResumeElement.EducationElement,
        availableWidthDp: Float
    ): List<ItemHeightInfo> {
        val padding = element.padding ?: Padding()
        val contentWidth = availableWidthDp - padding.left - padding.right
        var cumulativeHeight = 0f
        
        return element.items.mapIndexed { index, item ->
            val itemHeight = calculateSingleEducationItemHeight(item, element, contentWidth)
            cumulativeHeight += itemHeight
            
            if (index < element.items.size - 1) {
                cumulativeHeight += element.spacing
            }
            
            ItemHeightInfo(
                itemId = item.id,
                itemIndex = index,
                heightDp = itemHeight,
                cumulativeHeightDp = cumulativeHeight
            )
        }
    }
    
    private fun calculateSingleEducationItemHeight(
        item: EducationItem,
        element: ResumeElement.EducationElement,
        contentWidthDp: Float
    ): Float {
        var height = 0f
        
        // Degree
        setupTextPaint(element.degreeStyle)
        height += measureTextHeight(item.degree, contentWidthDp)
        height += element.itemSpacing
        
        // Institution
        setupTextPaint(element.institutionStyle)
        height += measureTextHeight(item.institution, contentWidthDp)
        height += element.itemSpacing
        
        // Date
        if (element.showDates) {
            setupTextPaint(element.dateStyle)
            val dateText = buildDateText(item.startDate, item.endDate, false, element.dateSeparator)
            height += measureTextHeight(dateText, contentWidthDp)
            height += element.itemSpacing
        }
        
        // Location
        if (element.showLocation && item.location.isNotEmpty()) {
            setupTextPaint(element.locationStyle)
            height += measureTextHeight(item.location, contentWidthDp)
            height += element.itemSpacing
        }
        
        // GPA
        if (element.showGPA && item.gpa.isNotEmpty()) {
            setupTextPaint(element.gpaStyle)
            height += measureTextHeight("GPA: ${item.gpa}", contentWidthDp)
            height += element.itemSpacing
        }
        
        // Achievements
        item.achievements.forEachIndexed { index, achievement ->
            setupTextPaint(element.achievementStyle)
            val bulletWidth = 16f
            height += measureTextHeight(achievement.text, contentWidthDp - bulletWidth)
            
            if (index < item.achievements.size - 1) {
                height += element.achievementSpacing
            }
        }
        
        return height
    }
    
    /**
     * Calculate height for skill element
     */
    fun calculateSkillHeight(
        element: ResumeElement.SkillElement,
        availableWidthDp: Float
    ): Float {
        val padding = element.padding ?: Padding()
        val contentWidth = availableWidthDp - padding.left - padding.right
        var height = 0f
        
        when (element.displayStyle) {
            SkillDisplayStyle.LIST -> {
                element.items.forEachIndexed { index, item ->
                    setupTextPaint(element.skillStyle)
                    val bulletWidth = if (element.showBullets) 16f else 0f
                    height += measureTextHeight(item.name, contentWidth - bulletWidth)
                    if (index < element.items.size - 1) {
                        height += element.spacing
                    }
                }
            }
            SkillDisplayStyle.TAGS -> {
                // Tags wrap, estimate based on average tag size
                val avgTagWidth = 80f // Approximate average tag width
                val tagsPerRow = (contentWidth / avgTagWidth).toInt().coerceAtLeast(1)
                val rows = (element.items.size + tagsPerRow - 1) / tagsPerRow
                val tagHeight = 28f // Approximate tag height
                height = rows * tagHeight + (rows - 1) * element.spacing
            }
            SkillDisplayStyle.PROGRESS_BARS -> {
                element.items.forEachIndexed { index, item ->
                    setupTextPaint(element.skillStyle)
                    height += measureTextHeight(item.name, contentWidth)
                    height += element.progressBarHeight + 4f // Bar + gap
                    if (index < element.items.size - 1) {
                        height += element.spacing
                    }
                }
            }
            SkillDisplayStyle.DOTS -> {
                element.items.forEachIndexed { index, item ->
                    setupTextPaint(element.skillStyle)
                    height += measureTextHeight(item.name, contentWidth)
                    height += element.dotSize + 4f // Dots + gap
                    if (index < element.items.size - 1) {
                        height += element.spacing
                    }
                }
            }
            SkillDisplayStyle.GROUPED -> {
                // Group by category
                val grouped = element.items.groupBy { it.category }
                grouped.entries.forEachIndexed { groupIndex, (category, items) ->
                    if (category.isNotEmpty()) {
                        setupTextPaint(element.categoryStyle)
                        height += measureTextHeight(category, contentWidth)
                        height += element.spacing
                    }
                    items.forEachIndexed { index, item ->
                        setupTextPaint(element.skillStyle)
                        height += measureTextHeight(item.name, contentWidth)
                        if (index < items.size - 1) {
                            height += element.spacing
                        }
                    }
                    if (groupIndex < grouped.size - 1) {
                        height += element.groupSpacing
                    }
                }
            }
        }
        
        return height + padding.top + padding.bottom
    }
    
    /**
     * Calculate height breakdown for skill items (for splitting at category boundaries)
     */
    fun calculateSkillItemHeights(
        element: ResumeElement.SkillElement,
        availableWidthDp: Float
    ): List<ItemHeightInfo> {
        val padding = element.padding ?: Padding()
        val contentWidth = availableWidthDp - padding.left - padding.right
        var cumulativeHeight = 0f
        
        return element.items.mapIndexed { index, item ->
            setupTextPaint(element.skillStyle)
            val itemHeight = measureTextHeight(item.name, contentWidth)
            cumulativeHeight += itemHeight
            
            if (index < element.items.size - 1) {
                cumulativeHeight += element.spacing
            }
            
            ItemHeightInfo(
                itemId = item.id,
                itemIndex = index,
                heightDp = itemHeight,
                cumulativeHeightDp = cumulativeHeight
            )
        }
    }
    
    /**
     * Calculate height for project element
     */
    fun calculateProjectHeight(
        element: ResumeElement.ProjectElement,
        availableWidthDp: Float
    ): Float {
        val itemHeights = calculateProjectItemHeights(element, availableWidthDp)
        val padding = element.padding ?: Padding()
        
        val contentHeight = itemHeights.lastOrNull()?.cumulativeHeightDp ?: 0f
        return contentHeight + padding.top + padding.bottom
    }
    
    fun calculateProjectItemHeights(
        element: ResumeElement.ProjectElement,
        availableWidthDp: Float
    ): List<ItemHeightInfo> {
        val padding = element.padding ?: Padding()
        val contentWidth = availableWidthDp - padding.left - padding.right
        var cumulativeHeight = 0f
        
        return element.items.mapIndexed { index, item ->
            val itemHeight = calculateSingleProjectItemHeight(item, element, contentWidth)
            cumulativeHeight += itemHeight
            
            if (index < element.items.size - 1) {
                cumulativeHeight += element.spacing
            }
            
            ItemHeightInfo(
                itemId = item.id,
                itemIndex = index,
                heightDp = itemHeight,
                cumulativeHeightDp = cumulativeHeight
            )
        }
    }
    
    private fun calculateSingleProjectItemHeight(
        item: ProjectItem,
        element: ResumeElement.ProjectElement,
        contentWidthDp: Float
    ): Float {
        var height = 0f
        
        // Project name
        setupTextPaint(element.nameStyle)
        height += measureTextHeight(item.name, contentWidthDp)
        height += element.itemSpacing
        
        // Description
        if (element.showDescription && item.description.isNotEmpty()) {
            setupTextPaint(element.descriptionStyle)
            height += measureTextHeight(item.description, contentWidthDp)
            height += element.itemSpacing
        }
        
        // Dates
        if (element.showDates) {
            setupTextPaint(element.dateStyle)
            val dateText = buildDateText(item.startDate, item.endDate, item.isOngoing, element.dateSeparator)
            height += measureTextHeight(dateText, contentWidthDp)
            height += element.itemSpacing
        }
        
        // Technologies (tags)
        if (element.showTechnologies && item.technologies.isNotEmpty()) {
            val tagHeight = 24f
            height += tagHeight
            height += element.itemSpacing
        }
        
        // Highlights
        item.highlights.forEachIndexed { index, highlight ->
            setupTextPaint(element.highlightStyle)
            height += measureTextHeight(highlight.text, contentWidthDp - 16f)
            if (index < item.highlights.size - 1) {
                height += element.highlightSpacing
            }
        }
        
        // Link
        if (element.showLink && item.link.isNotEmpty()) {
            setupTextPaint(element.linkStyle)
            height += measureTextHeight(item.link, contentWidthDp)
        }
        
        return height
    }
    
    /**
     * Calculate height for certification element
     */
    fun calculateCertificationHeight(
        element: ResumeElement.CertificationElement,
        availableWidthDp: Float
    ): Float {
        val itemHeights = calculateCertificationItemHeights(element, availableWidthDp)
        val padding = element.padding ?: Padding()
        
        val contentHeight = itemHeights.lastOrNull()?.cumulativeHeightDp ?: 0f
        return contentHeight + padding.top + padding.bottom
    }
    
    fun calculateCertificationItemHeights(
        element: ResumeElement.CertificationElement,
        availableWidthDp: Float
    ): List<ItemHeightInfo> {
        val padding = element.padding ?: Padding()
        val contentWidth = availableWidthDp - padding.left - padding.right
        var cumulativeHeight = 0f
        
        return element.items.mapIndexed { index, item ->
            val itemHeight = calculateSingleCertificationItemHeight(item, element, contentWidth)
            cumulativeHeight += itemHeight
            
            if (index < element.items.size - 1) {
                cumulativeHeight += element.spacing
            }
            
            ItemHeightInfo(
                itemId = item.id,
                itemIndex = index,
                heightDp = itemHeight,
                cumulativeHeightDp = cumulativeHeight
            )
        }
    }
    
    private fun calculateSingleCertificationItemHeight(
        item: CertificationItem,
        element: ResumeElement.CertificationElement,
        contentWidthDp: Float
    ): Float {
        var height = 0f
        
        // Name
        setupTextPaint(element.nameStyle)
        height += measureTextHeight(item.name, contentWidthDp)
        height += element.itemSpacing
        
        // Issuer
        setupTextPaint(element.issuerStyle)
        height += measureTextHeight(item.issuer, contentWidthDp)
        height += element.itemSpacing
        
        // Issue date
        if (element.showIssueDate && item.issueDate.isNotEmpty()) {
            setupTextPaint(element.dateStyle)
            height += measureTextHeight("Issued: ${item.issueDate}", contentWidthDp)
            height += element.itemSpacing
        }
        
        // Expiry date
        if (element.showExpiryDate && item.expiryDate.isNotEmpty()) {
            setupTextPaint(element.dateStyle)
            height += measureTextHeight("Expires: ${item.expiryDate}", contentWidthDp)
            height += element.itemSpacing
        }
        
        // Credential ID
        if (element.showCredentialId && item.credentialId.isNotEmpty()) {
            setupTextPaint(element.credentialIdStyle)
            height += measureTextHeight("ID: ${item.credentialId}", contentWidthDp)
            height += element.itemSpacing
        }
        
        // Verification link
        if (element.showVerificationLink && item.verificationLink.isNotEmpty()) {
            setupTextPaint(element.linkStyle)
            height += measureTextHeight(item.verificationLink, contentWidthDp)
        }
        
        return height
    }
    
    /**
     * Calculate height for language element
     */
    fun calculateLanguageHeight(
        element: ResumeElement.LanguageElement,
        availableWidthDp: Float
    ): Float {
        val itemHeights = calculateLanguageItemHeights(element, availableWidthDp)
        val padding = element.padding ?: Padding()
        
        val contentHeight = itemHeights.lastOrNull()?.cumulativeHeightDp ?: 0f
        return contentHeight + padding.top + padding.bottom
    }
    
    fun calculateLanguageItemHeights(
        element: ResumeElement.LanguageElement,
        availableWidthDp: Float
    ): List<ItemHeightInfo> {
        val padding = element.padding ?: Padding()
        val contentWidth = availableWidthDp - padding.left - padding.right
        var cumulativeHeight = 0f
        
        return element.items.mapIndexed { index, item ->
            var itemHeight = 0f
            
            // Language name
            setupTextPaint(element.languageStyle)
            itemHeight += measureTextHeight(item.name, contentWidth)
            
            // Proficiency indicator
            when (element.displayStyle) {
                LanguageDisplayStyle.TEXT_LABELS -> {
                    setupTextPaint(element.proficiencyLabelStyle)
                    itemHeight += measureTextHeight(item.proficiencyLabel, contentWidth)
                }
                LanguageDisplayStyle.PROGRESS_BARS -> {
                    itemHeight += element.progressBarHeight + 4f
                }
                LanguageDisplayStyle.DOTS -> {
                    itemHeight += element.dotSize + 4f
                }
                LanguageDisplayStyle.TAGS -> {
                    itemHeight += 24f // Tag height
                }
            }
            
            cumulativeHeight += itemHeight
            
            if (index < element.items.size - 1) {
                cumulativeHeight += element.spacing
            }
            
            ItemHeightInfo(
                itemId = item.id,
                itemIndex = index,
                heightDp = itemHeight,
                cumulativeHeightDp = cumulativeHeight
            )
        }
    }
    
    /**
     * Calculate height for contact element
     */
    fun calculateContactHeight(
        element: ResumeElement.ContactElement,
        availableWidthDp: Float
    ): Float {
        val padding = element.padding ?: Padding()
        val contentWidth = availableWidthDp - padding.left - padding.right
        var height = 0f
        
        when (element.orientation) {
            ContactOrientation.VERTICAL -> {
                element.items.forEachIndexed { index, item ->
                    setupTextPaint(element.textStyle)
                    val iconWidth = if (element.iconStyle != ContactIconStyle.NONE) element.iconSize + 8f else 0f
                    height += measureTextHeight(item.value, contentWidth - iconWidth)
                    if (index < element.items.size - 1) {
                        height += element.spacing
                    }
                }
            }
            ContactOrientation.HORIZONTAL -> {
                // Single row
                setupTextPaint(element.textStyle)
                height = measureTextHeight("Sample", contentWidth)
            }
        }
        
        return height + padding.top + padding.bottom
    }
    
    /**
     * Calculate height for container element (sum of children heights for VERTICAL layout)
     */
    fun calculateContainerHeight(
        element: ResumeElement.ContainerElement,
        availableWidthDp: Float
    ): Float {
        // For containers, if using FIXED mode, use rowSpan
        if (element.position.heightMode == SizeMode.FIXED) {
            return element.position.rowSpan * cellSizeDp
        }
        
        // For VERTICAL containers with WRAP_CONTENT, sum children heights
        // This requires access to child elements which we don't have here
        // Return cached height if available, otherwise fallback to rowSpan
        return element.position.cachedHeightDp ?: (element.position.rowSpan * cellSizeDp)
    }
    
    /**
     * Calculate container height with access to all elements
     * Used when we have the full element list available
     */
    fun calculateContainerHeightWithChildren(
        container: ResumeElement.ContainerElement,
        allElements: List<ResumeElement>,
        availableWidthDp: Float
    ): Float {
        if (container.effectiveLayoutMode != LayoutMode.VERTICAL) {
            return container.position.cachedHeightDp ?: (container.position.rowSpan * cellSizeDp)
        }
        
        val padding = container.padding
        val contentWidth = availableWidthDp - padding.left - padding.right
        var totalHeight = padding.top + padding.bottom
        
        container.children.forEachIndexed { index, childId ->
            val child = allElements.find { it.id == childId } ?: return@forEachIndexed
            totalHeight += calculateHeight(child, contentWidth)
            
            // Add spacing between children (not after last)
            if (index < container.children.size - 1) {
                totalHeight += 8f // Default child spacing - could be made configurable
            }
        }
        
        return totalHeight
    }
    
    // ========================================================================
    // Helper Methods
    // ========================================================================
    
    private fun setupTextPaint(textStyle: TextStyle) {
        textPaint.textSize = textStyle.fontSize * context.resources.displayMetrics.density
        textPaint.typeface = when (textStyle.fontWeight) {
            FontWeight.Bold, FontWeight.ExtraBold, FontWeight.Black -> Typeface.DEFAULT_BOLD
            FontWeight.Medium, FontWeight.SemiBold -> Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            else -> Typeface.DEFAULT
        }
    }
    
    private fun measureTextHeight(text: String, availableWidthDp: Float): Float {
        if (text.isBlank()) return 0f
        
        val widthPx = (availableWidthDp * context.resources.displayMetrics.density).toInt().coerceAtLeast(1)
        
        val layout = StaticLayout.Builder.obtain(text, 0, text.length, textPaint, widthPx)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setLineSpacing(0f, 1f)
            .setIncludePad(true)
            .build()
        
        return layout.height / context.resources.displayMetrics.density
    }
    
    private fun buildDateText(
        startDate: String,
        endDate: String,
        isCurrent: Boolean,
        separator: String
    ): String {
        val end = when {
            isCurrent -> "Present"
            endDate.isNotEmpty() -> endDate
            else -> ""
        }
        
        return when {
            startDate.isNotEmpty() && end.isNotEmpty() -> "$startDate$separator$end"
            startDate.isNotEmpty() -> startDate
            end.isNotEmpty() -> end
            else -> ""
        }
    }
    
    // ========================================================================
    // Page Metrics
    // ========================================================================
    
    /**
     * Get page height in dp
     */
    fun getPageHeightDp(): Float = pageHeightDp
    
    /**
     * Get page width in dp
     */
    fun getPageWidthDp(): Float = pageWidthDp
    
    /**
     * Convert row position to dp offset from top
     */
    fun rowToDp(row: Int): Float = row * cellSizeDp
    
    /**
     * Get available height from a given row position to page bottom
     */
    fun getAvailableHeightFromRow(row: Int): Float = pageHeightDp - rowToDp(row)
}
