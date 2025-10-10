package com.phamnhantucode.aicareercoach.ui.onboarding

data class Industry(
    val id: String,
    val name: String,
    val subIndustries: List<String>
)

object IndustriesData {
    val industries = listOf(
        Industry(
            id = "tech",
            name = "Technology",
            subIndustries = listOf(
                "Software Development",
                "IT Services",
                "Cybersecurity",
                "Cloud Computing",
                "Artificial Intelligence/Machine Learning",
                "Data Science & Analytics",
                "Internet & Web Services",
                "Robotics",
                "Quantum Computing",
                "Blockchain & Cryptocurrency",
                "IoT (Internet of Things)",
                "Virtual/Augmented Reality",
                "Semiconductor & Electronics"
            )
        ),
        Industry(
            id = "finance",
            name = "Financial Services",
            subIndustries = listOf(
                "Banking",
                "Investment Banking",
                "Insurance",
                "FinTech",
                "Wealth Management",
                "Asset Management",
                "Real Estate Investment",
                "Private Equity",
                "Venture Capital",
                "Cryptocurrency & Digital Assets",
                "Risk Management",
                "Payment Processing",
                "Credit Services"
            )
        ),
        Industry(
            id = "healthcare",
            name = "Healthcare & Life Sciences",
            subIndustries = listOf(
                "Healthcare Services",
                "Biotechnology",
                "Pharmaceuticals",
                "Medical Devices",
                "Healthcare IT",
                "Telemedicine",
                "Mental Health Services",
                "Genomics",
                "Clinical Research",
                "Healthcare Analytics",
                "Elder Care Services",
                "Veterinary Services",
                "Alternative Medicine"
            )
        ),
        Industry(
            id = "manufacturing",
            name = "Manufacturing & Industrial",
            subIndustries = listOf(
                "Automotive",
                "Aerospace & Defense",
                "Electronics Manufacturing",
                "Industrial Manufacturing",
                "Chemical Manufacturing",
                "Consumer Goods",
                "Food & Beverage Processing",
                "Textile Manufacturing",
                "Metal Fabrication",
                "3D Printing/Additive Manufacturing",
                "Machinery & Equipment",
                "Packaging",
                "Plastics & Rubber"
            )
        ),
        Industry(
            id = "retail",
            name = "Retail & E-commerce",
            subIndustries = listOf(
                "E-commerce Platforms",
                "Retail Technology",
                "Fashion & Apparel",
                "Consumer Electronics",
                "Grocery & Food Retail",
                "Luxury Goods",
                "Sports & Recreation",
                "Home & Garden",
                "Beauty & Personal Care",
                "Pet Products",
                "Specialty Retail",
                "Direct-to-Consumer (D2C)",
                "Department Stores"
            )
        ),
        Industry(
            id = "media",
            name = "Media & Entertainment",
            subIndustries = listOf(
                "Digital Media",
                "Gaming & Esports",
                "Streaming Services",
                "Social Media",
                "Digital Marketing",
                "Film & Television",
                "Music & Audio",
                "Publishing",
                "Advertising",
                "Sports Entertainment",
                "News & Journalism",
                "Animation",
                "Event Management"
            )
        ),
        Industry(
            id = "education",
            name = "Education & Training",
            subIndustries = listOf(
                "EdTech",
                "Higher Education",
                "Professional Training",
                "Online Learning",
                "K-12 Education",
                "Corporate Training",
                "Language Learning",
                "Special Education",
                "Early Childhood Education",
                "Career Development",
                "Educational Publishing",
                "Educational Consulting",
                "Vocational Training"
            )
        ),
        Industry(
            id = "energy",
            name = "Energy & Utilities",
            subIndustries = listOf(
                "Renewable Energy",
                "Clean Technology",
                "Oil & Gas",
                "Nuclear Energy",
                "Energy Management",
                "Utilities",
                "Smart Grid Technology",
                "Energy Storage",
                "Carbon Management",
                "Waste Management",
                "Water & Wastewater",
                "Mining",
                "Environmental Services"
            )
        ),
        Industry(
            id = "consulting",
            name = "Professional Services",
            subIndustries = listOf(
                "Management Consulting",
                "IT Consulting",
                "Strategy Consulting",
                "Digital Transformation",
                "Business Advisory",
                "Legal Services",
                "Accounting & Tax",
                "Human Resources",
                "Marketing Services",
                "Architecture",
                "Engineering Services",
                "Research & Development",
                "Business Process Outsourcing (BPO)"
            )
        ),
        Industry(
            id = "telecom",
            name = "Telecommunications",
            subIndustries = listOf(
                "Wireless Communications",
                "Network Infrastructure",
                "Telecom Services",
                "5G Technology",
                "Internet Service Providers",
                "Satellite Communications",
                "Data Centers",
                "Fiber Optics",
                "Mobile Technology",
                "VoIP Services",
                "Network Security",
                "Telecom Equipment",
                "Cloud Communications"
            )
        ),
        Industry(
            id = "transportation",
            name = "Transportation & Logistics",
            subIndustries = listOf(
                "Electric Vehicles",
                "Autonomous Vehicles",
                "Logistics & Supply Chain",
                "Aviation",
                "Railways",
                "Maritime Transport",
                "Urban Mobility",
                "Fleet Management",
                "Last-Mile Delivery",
                "Warehousing",
                "Freight & Cargo",
                "Public Transportation",
                "Space Transportation"
            )
        ),
        Industry(
            id = "agriculture",
            name = "Agriculture & Food",
            subIndustries = listOf(
                "AgTech",
                "Farming",
                "Food Production",
                "Sustainable Agriculture",
                "Precision Agriculture",
                "Aquaculture",
                "Vertical Farming",
                "Agricultural Biotechnology",
                "Food Processing",
                "Organic Farming",
                "Plant-Based Foods",
                "Agricultural Equipment",
                "Indoor Farming"
            )
        ),
        Industry(
            id = "construction",
            name = "Construction & Real Estate",
            subIndustries = listOf(
                "Commercial Construction",
                "Residential Construction",
                "Real Estate Development",
                "Property Management",
                "Construction Technology",
                "Building Materials",
                "Infrastructure Development",
                "Smart Buildings",
                "Interior Design",
                "Facilities Management",
                "Real Estate Technology",
                "Sustainable Building",
                "Urban Planning"
            )
        ),
        Industry(
            id = "hospitality",
            name = "Hospitality & Tourism",
            subIndustries = listOf(
                "Hotels & Resorts",
                "Restaurants & Food Service",
                "Travel Technology",
                "Tourism",
                "Event Planning",
                "Vacation Rentals",
                "Cruise Lines",
                "Catering",
                "Theme Parks",
                "Travel Agencies",
                "Hospitality Management",
                "Online Travel Booking",
                "Cultural Tourism"
            )
        ),
        Industry(
            id = "nonprofit",
            name = "Non-Profit & Social Services",
            subIndustries = listOf(
                "Charitable Organizations",
                "Social Services",
                "Environmental Conservation",
                "Humanitarian Aid",
                "Education Non-Profits",
                "Healthcare Non-Profits",
                "Arts & Culture",
                "Community Development",
                "International Development",
                "Animal Welfare",
                "Youth Organizations",
                "Social Enterprise",
                "Advocacy Organizations"
            )
        )
    )
}
