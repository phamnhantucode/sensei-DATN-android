package com.phamnhantucode.aicareercoach.utils

/**
 * Utility object for formatting industry data between display format and API format.
 *
 * The app uses user-friendly display names (e.g., "Software Development") internally,
 * but the backend API requires a specific format: industryId---sub-industry-kebab-case
 * (e.g., "tech---software-development").
 */
object IndustryFormatUtils {

    /**
     * Converts a display name to kebab-case format.
     *
     * Examples:
     * - "Software Development" → "software-development"
     * - "Blockchain & Cryptocurrency" → "blockchain-cryptocurrency"
     * - "IoT (Internet of Things)" → "iot-internet-of-things"
     * - "3D Printing/Additive Manufacturing" → "3d-printingadditive-manufacturing"
     *
     * @param text The display text to convert
     * @return The kebab-cased version of the text
     */
    fun toKebabCase(text: String): String {
        return text.trim()
            .lowercase()
            .replace(Regex("\\s+"), "-")           // Replace spaces with hyphens
            .replace(Regex("[^a-z0-9-]"), "")      // Remove special characters
            .replace(Regex("-+"), "-")             // Replace multiple hyphens with single
            .trim('-')                             // Remove leading/trailing hyphens
    }

    /**
     * Formats industry data for backend API.
     *
     * Combines industry ID and sub-industry into the format required by the backend:
     * industryId---sub-industry-in-kebab-case
     *
     * Example:
     * - formatIndustryForApi("tech", "Software Development") → "tech---software-development"
     *
     * @param industryId The industry ID (e.g., "tech", "finance")
     * @param subIndustry The sub-industry display name (e.g., "Software Development")
     * @return Formatted string in the format: industryId---sub-industry-kebab-case
     */
    fun formatIndustryForApi(industryId: String, subIndustry: String): String {
        val kebabSubIndustry = toKebabCase(subIndustry)
        return "$industryId---$kebabSubIndustry"
    }

    /**
     * Parses backend industry format back to its components.
     *
     * Example:
     * - "tech---software-development" → Pair("tech", "software-development")
     *
     * @param formattedIndustry The formatted industry string from the backend
     * @return Pair of (industryId, kebabCasedSubIndustry) or null if invalid format
     */
    fun parseIndustryFormat(formattedIndustry: String): Pair<String, String>? {
        val parts = formattedIndustry.split("---")
        return if (parts.size == 2) {
            Pair(parts[0], parts[1])
        } else {
            null
        }
    }

    /**
     * Converts kebab-case text back to title case for display.
     *
     * Note: This is a best-effort reconstruction. The original casing may differ
     * from what this function produces.
     *
     * Example:
     * - "software-development" → "Software Development"
     *
     * @param kebabText The kebab-cased text to convert
     * @return Title-cased version of the text
     */
    fun fromKebabCase(kebabText: String): String {
        return kebabText.split("-")
            .joinToString(" ") { word ->
                word.replaceFirstChar { it.uppercase() }
            }
    }

    /**
     * Checks if an industry string is in the old legacy format (no "---" separator).
     *
     * Old format: "Technology"
     * New format: "tech---software-development"
     *
     * @param industry The industry string to check
     * @return true if the string is in legacy format, false otherwise
     */
    fun isLegacyFormat(industry: String): Boolean {
        return !industry.contains("---")
    }

    /**
     * Formats industry data for display to users.
     *
     * Converts the backend format to a user-friendly display string by looking up
     * the industry name and converting the kebab-cased sub-industry back to title case.
     *
     * Example:
     * - "tech---software-development" → "Technology - Software Development"
     *
     * @param formattedIndustry The backend format (e.g., "tech---software-development")
     * @param industries The list of industries to look up the display name
     * @return User-friendly display string
     */
    fun <T> formatIndustryForDisplay(
        formattedIndustry: String,
        industries: List<T>,
        getId: (T) -> String,
        getName: (T) -> String
    ): String {
        val parsed = parseIndustryFormat(formattedIndustry)
        if (parsed == null) {
            // Fallback for legacy format or invalid data
            return formattedIndustry
        }

        val (industryId, kebabSubIndustry) = parsed

        // Find the industry by ID to get the display name
        val industry = industries.find { getId(it) == industryId }

        // Convert kebab-case sub-industry back to title case
        val subIndustryDisplay = fromKebabCase(kebabSubIndustry)

        return if (industry != null) {
            "${getName(industry)} - $subIndustryDisplay"
        } else {
            // Fallback if industry not found
            "$industryId - $subIndustryDisplay"
        }
    }
}
