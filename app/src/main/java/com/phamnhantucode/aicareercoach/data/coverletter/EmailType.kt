package com.phamnhantucode.aicareercoach.data.coverletter

enum class EmailType(val displayName: String, val id: String) {
    APPLICATION("Application", "APPLICATION"),
    PROSPECTING("Prospecting", "PROSPECTING"),
    REFERRAL("Referral Request", "REFERRAL"),
    THANK_YOU("Thank You Note", "THANK_YOU");

    companion object {
        fun fromId(id: String): EmailType {
            return entries.find { it.id == id } ?: APPLICATION
        }
    }
}
