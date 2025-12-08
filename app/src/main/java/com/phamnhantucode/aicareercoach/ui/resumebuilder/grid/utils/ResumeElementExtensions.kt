package com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.utils

import com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.models.ElementStyle
import com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.models.GridPosition
import com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.models.ResumeElement
import com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.models.ResumePage

/**
 * Generic update extension for ResumeElement.
 * This allows updating common properties without a when expression.
 */
fun ResumeElement.update(
    position: GridPosition = this.position,
    style: ElementStyle = this.style,
    zIndex: Int = this.zIndex,
    locked: Boolean = this.locked,
    isVisible: Boolean = this.isVisible
): ResumeElement {
    return when (this) {
        is ResumeElement.TextElement -> copy(position = position, style = style, zIndex = zIndex, locked = locked, isVisible = isVisible)
        is ResumeElement.ImageElement -> copy(position = position, style = style, zIndex = zIndex, locked = locked, isVisible = isVisible)
        is ResumeElement.ShapeElement -> copy(position = position, style = style, zIndex = zIndex, locked = locked, isVisible = isVisible)
        is ResumeElement.ChartElement -> copy(position = position, style = style, zIndex = zIndex, locked = locked, isVisible = isVisible)
        is ResumeElement.ContainerElement -> copy(position = position, style = style, zIndex = zIndex, locked = locked, isVisible = isVisible)
        is ResumeElement.IconElement -> copy(position = position, style = style, zIndex = zIndex, locked = locked, isVisible = isVisible)
        is ResumeElement.ContactElement -> copy(position = position, style = style, zIndex = zIndex, locked = locked, isVisible = isVisible)
        is ResumeElement.WorkExperienceElement -> copy(position = position, style = style, zIndex = zIndex, locked = locked, isVisible = isVisible)
        is ResumeElement.EducationElement -> copy(position = position, style = style, zIndex = zIndex, locked = locked, isVisible = isVisible)
        is ResumeElement.SkillElement -> copy(position = position, style = style, zIndex = zIndex, locked = locked, isVisible = isVisible)
        is ResumeElement.ProjectElement -> copy(position = position, style = style, zIndex = zIndex, locked = locked, isVisible = isVisible, technologiesPlacement = this.technologiesPlacement)
        is ResumeElement.CertificationElement -> copy(position = position, style = style, zIndex = zIndex, locked = locked, isVisible = isVisible)
        is ResumeElement.LanguageElement -> copy(position = position, style = style, zIndex = zIndex, locked = locked, isVisible = isVisible)
    }
}

// ============================================================================
// ResumePage Extensions
// ============================================================================

/**
 * Get all elements sorted by z-index (bottom to top)
 */
fun ResumePage.elementsByZIndex(): List<ResumeElement> {
    return elements.sortedBy { it.zIndex }
}

/**
 * Get element at specific grid position
 */
fun ResumePage.elementAt(row: Int, col: Int): ResumeElement? {
    return elements.firstOrNull { it.position.contains(row, col) }
}

/**
 * Check if position is occupied
 */
fun ResumePage.isOccupied(position: GridPosition, excludeId: String? = null): Boolean {
    return elements.any {
        it.id != excludeId && it.position.overlaps(position)
    }
}

/**
 * Add element to page
 */
fun ResumePage.addElement(element: ResumeElement): ResumePage {
    return copy(elements = elements + element)
}

/**
 * Update element
 */
fun ResumePage.updateElement(elementId: String, update: (ResumeElement) -> ResumeElement): ResumePage {
    return copy(
        elements = elements.map {
            if (it.id == elementId) update(it) else it
        }
    )
}

/**
 * Remove element from page
 * - Removes element from the elements list
 * - Removes element from any parent container's children list
 * - If element is a ContainerElement, moves its children to top-level (preserves them)
 */
fun ResumePage.removeElement(elementId: String): ResumePage {
    // 1. Find the element to be deleted
    val elementToDelete = elements.find { it.id == elementId } ?: return this

    // 2. If deleting a container, convert children to absolute positions
    val preparedElements = if (elementToDelete is ResumeElement.ContainerElement) {
        elements.map { element ->
            if (elementToDelete.children.contains(element.id)) {
                // Convert from container-relative to page-absolute coordinates
                val absoluteRow = elementToDelete.position.row + element.position.row
                val absoluteCol = elementToDelete.position.col + element.position.col

                element.update(
                    position = element.position.copy(row = absoluteRow, col = absoluteCol)
                )
            } else {
                element
            }
        }
    } else {
        elements
    }

    // 3. Remove the element from the elements list
    val withoutElement = preparedElements.filter { it.id != elementId }

    // 4. Clean up container references
    val cleanedElements = withoutElement.map { element ->
        if (element is ResumeElement.ContainerElement && element.children.contains(elementId)) {
            element.copy(children = element.children - elementId)
        } else {
            element
        }
    }

    return copy(elements = cleanedElements)
}
