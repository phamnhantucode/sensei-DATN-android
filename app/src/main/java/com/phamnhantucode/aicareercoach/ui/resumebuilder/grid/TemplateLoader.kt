package com.phamnhantucode.aicareercoach.ui.resumebuilder.grid

import android.content.Context
import com.google.gson.GsonBuilder
import com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.models.GridResume
import com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.models.ResumeElement
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Data class representing a template with its metadata and thumbnail
 */
data class ResumeTemplate(
    val id: String,
    val name: String,
    val description: String,
    val category: String, // e.g., "modern", "classic", "creative"
    val assetPath: String, // Path in assets folder
    val thumbnail: String = "", // Base64 encoded thumbnail
    val gridResume: GridResume? = null // Loaded template data
)

/**
 * Loads resume templates from assets folder
 * 
 * Templates are stored in assets/template/[category]/[template_name].json
 * Each category folder contains template JSON files that can be loaded
 */
class TemplateLoader(private val context: Context) {

    private val gson = GsonBuilder()
        .registerTypeAdapter(ResumeElement::class.java, ResumeElementTypeAdapter())
        .create()

    private val thumbnailGenerator = ThumbnailGenerator(context)

    // Cache for loaded templates
    private val templateCache = mutableMapOf<String, ResumeTemplate>()

    /**
     * Get all available template categories from assets
     */
    suspend fun getTemplateCategories(): List<String> = withContext(Dispatchers.IO) {
        try {
            context.assets.list("template")?.toList() ?: emptyList()
        } catch (e: Exception) {
            android.util.Log.e("TemplateLoader", "Failed to list template categories", e)
            emptyList()
        }
    }

    /**
     * Get all templates in a specific category
     */
    suspend fun getTemplatesInCategory(category: String): List<ResumeTemplate> = withContext(Dispatchers.IO) {
        try {
            val templateFiles = context.assets.list("template/$category") ?: return@withContext emptyList()
            
            templateFiles.filter { it.endsWith(".json") }.mapNotNull { fileName ->
                val assetPath = "template/$category/$fileName"
                val templateId = "${category}_${fileName.removeSuffix(".json")}"
                
                // Check cache first
                templateCache[templateId]?.let { return@mapNotNull it }
                
                // Load template
                loadTemplateFromAsset(assetPath, category)
            }
        } catch (e: Exception) {
            android.util.Log.e("TemplateLoader", "Failed to list templates in category: $category", e)
            emptyList()
        }
    }

    /**
     * Get all available templates from all categories
     */
    suspend fun getAllTemplates(): List<ResumeTemplate> = withContext(Dispatchers.IO) {
        val categories = getTemplateCategories()
        categories.flatMap { category ->
            getTemplatesInCategory(category)
        }
    }

    /**
     * Load a specific template from asset path
     */
    suspend fun loadTemplateFromAsset(assetPath: String, category: String): ResumeTemplate? = withContext(Dispatchers.IO) {
        try {
            val json = context.assets.open(assetPath).bufferedReader().use { it.readText() }
            val gridResume = gson.fromJson(json, GridResume::class.java)
            
            val templateId = assetPath.replace("/", "_").removeSuffix(".json")
            
            // Generate thumbnail
            val thumbnail = thumbnailGenerator.generateThumbnail(gridResume)
            
            val template = ResumeTemplate(
                id = templateId,
                name = gridResume.name.ifEmpty { extractTemplateName(assetPath) },
                description = getTemplateDescription(category),
                category = category,
                assetPath = assetPath,
                thumbnail = thumbnail,
                gridResume = gridResume
            )
            
            // Cache the template
            templateCache[templateId] = template
            
            template
        } catch (e: Exception) {
            android.util.Log.e("TemplateLoader", "Failed to load template from: $assetPath", e)
            null
        }
    }

    /**
     * Load a template by its ID
     */
    suspend fun loadTemplateById(templateId: String): ResumeTemplate? {
        // Check cache first
        templateCache[templateId]?.let { return it }
        
        // Parse template ID to get asset path
        val parts = templateId.split("_")
        if (parts.size < 3) return null
        
        val category = parts[1]
        val fileName = parts.drop(2).joinToString("_") + ".json"
        val assetPath = "template/$category/$fileName"
        
        return loadTemplateFromAsset(assetPath, category)
    }

    /**
     * Get a copy of the GridResume from a template (for editing)
     */
    suspend fun getTemplateGridResume(template: ResumeTemplate): GridResume? {
        return template.gridResume?.copy(
            id = java.util.UUID.randomUUID().toString(),
            name = "Resume - ${template.name}",
            metadata = template.gridResume.metadata.copy(
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    /**
     * Clear the template cache
     */
    fun clearCache() {
        templateCache.clear()
    }

    private fun extractTemplateName(assetPath: String): String {
        return assetPath
            .substringAfterLast("/")
            .removeSuffix(".json")
            .replace("_", " ")
            .replaceFirstChar { it.uppercase() }
    }

    private fun getTemplateDescription(category: String): String {
        return when (category.lowercase()) {
            "modern" -> "Contemporary design with clean lines and accent colors"
            "classic" -> "Traditional professional layout for corporate roles"
            "creative" -> "Bold and unique design for creative industries"
            "minimal" -> "Simple, elegant layout focused on content"
            "academic" -> "Traditional format suitable for academic positions"
            "technical" -> "Grid-based layout optimized for technical roles"
            else -> "Professional resume template"
        }
    }

    companion object {
        @Volatile
        private var instance: TemplateLoader? = null

        fun getInstance(context: Context): TemplateLoader {
            return instance ?: synchronized(this) {
                instance ?: TemplateLoader(context.applicationContext).also { instance = it }
            }
        }
    }
}
