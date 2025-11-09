package com.phamnhantucode.aicareercoach.ui.resumebuilder.grid

import com.google.gson.*
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
        val type = jsonObject.get("elementType").asString
        val data = jsonObject.get("data")

        return when (type) {
            "TextElement" -> context.deserialize(data, ResumeElement.TextElement::class.java)
            "ImageElement" -> context.deserialize(data, ResumeElement.ImageElement::class.java)
            "ShapeElement" -> context.deserialize(data, ResumeElement.ShapeElement::class.java)
            "ChartElement" -> context.deserialize(data, ResumeElement.ChartElement::class.java)
            "ContainerElement" -> context.deserialize(data, ResumeElement.ContainerElement::class.java)
            "IconElement" -> context.deserialize(data, ResumeElement.IconElement::class.java)
            "ContactElement" -> context.deserialize(data, ResumeElement.ContactElement::class.java)
            else -> throw JsonParseException("Unknown element type: $type")
        }
    }
}
