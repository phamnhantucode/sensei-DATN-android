package com.phamnhantucode.aicareercoach.ui.resumebuilder.grid

import com.google.gson.*
import com.phamnhantucode.aicareercoach.ui.resumebuilder.grid.models.ResumeElement
import java.lang.reflect.Type

/**
 * Gson Type Adapter for ResumeElement sealed class
 *
 * This adapter handles serialization/deserialization of the sealed class hierarchy
 * by adding a "type" field to distinguish between different element types
 */
class ResumeElementTypeAdapter : JsonSerializer<ResumeElement>, JsonDeserializer<ResumeElement> {

    override fun serialize(
        src: ResumeElement,
        typeOfSrc: Type,
        context: JsonSerializationContext
    ): JsonElement {
        val jsonObject = JsonObject()

        // Add type discriminator
        val type = when (src) {
            is ResumeElement.TextElement -> "TextElement"
            is ResumeElement.ImageElement -> "ImageElement"
            is ResumeElement.ShapeElement -> "ShapeElement"
            is ResumeElement.ChartElement -> "ChartElement"
            is ResumeElement.ContainerElement -> "ContainerElement"
            is ResumeElement.IconElement -> "IconElement"
            is ResumeElement.ContactElement -> "ContactElement"
            is ResumeElement.WorkExperienceElement -> "WorkExperienceElement"
            is ResumeElement.EducationElement -> "EducationElement"
            is ResumeElement.SkillElement -> "SkillElement"
            is ResumeElement.ProjectElement -> "ProjectElement"
            is ResumeElement.CertificationElement -> "CertificationElement"
            is ResumeElement.LanguageElement -> "LanguageElement"
        }
        jsonObject.addProperty("elementType", type)

        // Add the actual data
        val data = context.serialize(src)
        jsonObject.add("data", data)

        return jsonObject
    }

    override fun deserialize(
        json: JsonElement,
        typeOfT: Type,
        context: JsonDeserializationContext
    ): ResumeElement {
        val jsonObject = json.asJsonObject
        
        // Check if this is new format (with elementType and data) or old format (direct object)
        val elementTypeField = jsonObject.get("elementType")
        val type: String
        val data: JsonElement
        
        if (elementTypeField != null) {
            // New format with type discriminator
            type = elementTypeField.asString
            data = jsonObject.get("data")
        } else {
            // Old format - infer type from JSON structure
            type = inferElementType(jsonObject)
            data = jsonObject
        }

        return when (type) {
            "TextElement" -> context.deserialize(data, ResumeElement.TextElement::class.java)
            "ImageElement" -> context.deserialize(data, ResumeElement.ImageElement::class.java)
            "ShapeElement" -> context.deserialize(data, ResumeElement.ShapeElement::class.java)
            "ChartElement" -> context.deserialize(data, ResumeElement.ChartElement::class.java)
            "ContainerElement" -> context.deserialize(data, ResumeElement.ContainerElement::class.java)
            "IconElement" -> context.deserialize(data, ResumeElement.IconElement::class.java)
            "ContactElement" -> context.deserialize(data, ResumeElement.ContactElement::class.java)
            "WorkExperienceElement" -> context.deserialize(data, ResumeElement.WorkExperienceElement::class.java)
            "EducationElement" -> context.deserialize(data, ResumeElement.EducationElement::class.java)
            "SkillElement" -> context.deserialize(data, ResumeElement.SkillElement::class.java)
            "ProjectElement" -> context.deserialize(data, ResumeElement.ProjectElement::class.java)
            "CertificationElement" -> context.deserialize(data, ResumeElement.CertificationElement::class.java)
            "LanguageElement" -> context.deserialize(data, ResumeElement.LanguageElement::class.java)
            else -> throw JsonParseException("Unknown element type: $type")
        }
    }
    
    /**
     * Infer element type from JSON structure for backward compatibility with old data
     */
    private fun inferElementType(jsonObject: JsonObject): String {
        return when {
            jsonObject.has("content") && jsonObject.has("textStyle") -> "TextElement"
            jsonObject.has("imageUrl") -> "ImageElement"
            jsonObject.has("shapeType") -> "ShapeElement"
            jsonObject.has("chartType") -> "ChartElement"
            jsonObject.has("children") && jsonObject.get("children").isJsonArray -> "ContainerElement"
            jsonObject.has("iconName") && jsonObject.has("iconType") -> "IconElement"
            jsonObject.has("items") -> {
                // Need to check items structure to distinguish between different collection elements
                val items = jsonObject.getAsJsonArray("items")
                if (items.size() > 0) {
                    val firstItem = items[0].asJsonObject
                    when {
                        firstItem.has("jobTitle") && firstItem.has("company") -> "WorkExperienceElement"
                        firstItem.has("degree") && firstItem.has("institution") -> "EducationElement"
                        firstItem.has("issuer") && firstItem.has("credentialId") -> "CertificationElement"
                        firstItem.has("proficiency") && firstItem.has("cefrLevel") -> "LanguageElement"
                        firstItem.has("technologies") && firstItem.has("highlights") -> "ProjectElement"
                        firstItem.has("type") && firstItem.has("value") -> "ContactElement"
                        firstItem.has("name") && firstItem.has("category") -> "SkillElement"
                        else -> "TextElement" // Default fallback
                    }
                } else {
                    // Empty items - check for other distinguishing fields
                    when {
                        jsonObject.has("iconStyle") -> "ContactElement"
                        jsonObject.has("displayStyle") && jsonObject.has("dateStyle") -> {
                            if (jsonObject.has("companyStyle")) "WorkExperienceElement"
                            else if (jsonObject.has("institutionStyle")) "EducationElement"
                            else if (jsonObject.has("issuerStyle")) "CertificationElement"
                            else if (jsonObject.has("technologyStyle")) "ProjectElement"
                            else "SkillElement"
                        }
                        jsonObject.has("proficiencyType") -> "LanguageElement"
                        else -> "SkillElement" // Default for collection-type elements
                    }
                }
            }
            else -> "TextElement" // Default fallback
        }
    }
}
