package com.phamnhantucode.aicareercoach.data.ai

import com.phamnhantucode.aicareercoach.data.coverletter.EmailType

/**
 * A centralized factory for creating AI prompts.
 * Follows the "5-Block Unified Strategy":
 * 1. IDENTITY (Persona)
 * 2. CONTEXT (User Data)
 * 3. TARGET (Job/Task)
 * 4. INSTRUCTIONS (Rules)
 * 5. OUTPUT (Format)
 */
object PromptFactory {

    // --- 1. IDENTITY (Standardized Personas) ---
    // --- 1. IDENTITY (Standardized Personas) ---
    private const val PERSONA_COPYWRITER = "Expert Professional Copywriter and Career Coach specialized in high-conversion job search communication."
    private const val PERSONA_ATS_EXPERT = "Senior ATS (Applicant Tracking System) Specialist and Resume Strategist."
    private const val PERSONA_INTERVIEWER = "Technical Interviewer specializing in practical, competency-based questions."
    private const val PERSONA_MARKET_ANALYST = "Senior Labor Market Analyst specialized in global employment trends."

    private fun createPrompt(
        context: String,
        role: String,
        instruction: String,
        specification: String = "",
        performance: String = "",
        example: String = ""
    ): String {
        val parts = listOf(
            "Context: ${context.trim()}",
            "Role: ${role.trim()}",
            "Instruction: ${instruction.trim()}",
            if (specification.isNotBlank()) "Specification: ${specification.trim()}" else "Specification:",
            if (performance.isNotBlank()) "Performance: ${performance.trim()}" else "Performance:",
            if (example.isNotBlank()) "Example: ${example.trim()}" else "Example:"
        )
        return parts.joinToString("\n\n")
    }

    // --- 3. Feature Prompts ---

    /**
     * Generates a prompt for Cover Letter / Email generation.
     */
    /**
     * Generates a prompt for Cover Letter / Email generation.
     */
    fun createCoverLetterPrompt(
        context: String,
        type: EmailType,
        inputs: Map<String, String>
    ): String {
        val companyName = inputs["companyName"] ?: "the company"
        val recipientName = inputs["recipientName"]?.takeIf { it.isNotBlank() } ?: "Hiring Manager"
        val jobTitle = inputs["jobTitle"] ?: "the position"

        return when (type) {
            EmailType.APPLICATION -> {
                val jobDesc = inputs["jobDescription"] ?: ""
                createPrompt(
                    context = "CANDIDATE PROFILE:\n$context\n\nTASK CONTEXT:\nWrite a tailored cover letter for the position $jobTitle at $companyName. Recipient: $recipientName. Job Description: $jobDesc",
                    role = PERSONA_COPYWRITER,
                    instruction = "Analyze the job description to identify the top 3 critical skills. Map the candidate's experience to these skills using specific examples. Express genuine enthusiasm for the company and role. Produce a concise, persuasive cover letter.",
                    specification = "Format: Standard business letter in Markdown. Length: max 200 words. Tone: Professional, Confident, Persuasive. Must include 2-3 specific achievements demonstrating value.",
                    performance = "Success: A cover letter ready to send that clearly maps experience to JD and includes measurable achievements. Avoid generic filler; do not exceed 200 words."
                )
            }
            EmailType.PROSPECTING -> {
                val reason = inputs["context"] ?: "General networking"
                val targetRole = inputs["targetRole"] ?: ""
                val targetRoleStr = if (targetRole.isNotBlank()) " Target Role: $targetRole" else ""
                
                createPrompt(
                    context = "CANDIDATE PROFILE:\n$context\n\nTASK CONTEXT:\nWrite a concise cold/prospecting email to $recipientName at $companyName. Context: $reason$targetRoleStr",
                    role = PERSONA_COPYWRITER,
                    instruction = "Hook the reader immediately referencing the context. Briefly introduce the candidate and value proposition${if (targetRole.isNotBlank()) " related to $targetRole" else ""}. Include a soft call-to-action.",
                    specification = "Format: Markdown. Length: under 150 words. Tone: Polite, Respectful, Direct. Include one concise CTA.",
                    performance = "Success: A short cold email that prompts a reply or meeting; avoid generic phrasing and filler."
                )
            }
            EmailType.REFERRAL -> {
                val relationship = inputs["relationship"] ?: "Professional acquaintance"
                val targetJob = inputs["targetJob"] ?: ""
                val targetJobStr = if (targetJob.isNotBlank()) " Target Job: $targetJob" else ""
                
                createPrompt(
                    context = "CANDIDATE PROFILE:\n$context\n\nTASK CONTEXT:\nWrite a referral request email to $recipientName for a role at $companyName. Relationship context: $relationship$targetJobStr",
                    role = PERSONA_COPYWRITER,
                    instruction = "Begin with a warm, personalized greeting. State intent to apply and briefly explain why the candidate is a strong fit. Provide a 2-3 sentence 'blurb' at the end suitable for copy-pasting to HR.",
                    specification = "Format: Markdown. Tone: Grateful, Low-pressure. Include the copy-paste blurb as a separate paragraph.",
                    performance = "Success: A polite referral request that makes it easy for the recipient to refer; keep it concise and personal."
                )
            }
            EmailType.THANK_YOU -> {
                val topic = inputs["topic"]
                val topicStr = if (!topic.isNullOrBlank()) " Key topic discussed: $topic." else ""
                
                createPrompt(
                    context = "CANDIDATE PROFILE:\n$context\n\nTASK CONTEXT:\nWrite a thank-you follow-up email to $recipientName at $companyName for the $jobTitle interview.$topicStr",
                    role = PERSONA_COPYWRITER,
                    instruction = "Express sincere gratitude, reference a memorable point from the interview or the provided discussion topic, reiterate excitement for the role and how the candidate can add value.",
                    specification = "Format: Markdown. Length: concise, suitable to send within 24 hours. Tone: Warm, Professional, Appreciative.",
                    performance = "Success: A timely thank-you email that reinforces fit and interest, without repeating the entire interview."
                )
            }
        }
    }

    /**
     * Generates a prompt for Resume Match Analysis.
     * Matches Web: actions/resume.ts/analyzeMatchingResume
     */
    fun createMatchAnalysisPrompt(
        resumeJson: String,
        jobDescription: String
    ): String {
        return createPrompt(
            context = "JOB_DESCRIPTION:\n$jobDescription\n\nCANDIDATE_RESUME_JSON:\n$resumeJson",
            role = PERSONA_ATS_EXPERT,
            instruction = "Using JOB_DESCRIPTION and CANDIDATE_RESUME_JSON, produce a match analysis and suggestions. Return ONLY a single JSON object matching the template exactly. No extra text, code fences, or explanation.",
            specification = """
                # STRICT RULES FOR SKILLS (CRITICAL)
                1. ONLY MATCHING KEYWORDS: In 'fieldSuggestions.skills.suggested', you MUST include all essential skills and technologies mentioned in the JD, even if they are missing from the current resume.
                2. ATOMIZED FORMAT: Each skill must be a standalone keyword/tag. 
                - Split "React/Next.js" into "React", "Next.js".
                - Split "HTML/CSS" into "HTML", "CSS".
                3. NO EXPLANATIONS: DO NOT include any text in parentheses or extra descriptors.
                - WRONG: "Automated Testing (Eagerness to learn)", "English (Fluent)", "React (v18)".
                - RIGHT: "Automated Testing", "English", "React".
                4. CLEANING: Remove all adjectives like "Expert", "Proficient", "Junior", or "Knowledge of".

                # TASKS
                1. Overall Match Analysis: Score 0-100 based on JD requirements vs Resume.
                2. Identify missing skills: List skills required by JD that are not in the resume.
                3. Rewrite Sections: Optimize 'professional_summary', 'experiences', and 'projects' by weaving in JD keywords naturally.
                4. Skills Optimization: Generate a cleaned, atomized list of top 20 skills that are most relevant to the JD.
                Return EXACTLY this JSON structure (keys and types must match):
                {
                  "matchAnalysis": {
                    "overallScore": number; // 0–100
                    "verdict": "strong_match" | "moderate_match" | "weak_match";

                    "missingSkills": {
                        "required": string[], // Skills in JD but NOT in Resume
                        "niceToHave": string[] // Optional skills in JD but NOT in Resume
                    };

                    "missingExperience": string[];

                    "notes": string;
                  };

                  "generalSuggestions": string[];

                  // If the original content is good enough and needs no changes, then skip that item.
                  "fieldSuggestions": {
                    "professional_summary"?: { "current": "...", "suggested": "...", "reason": "..." };
                    "experiences"?: Array<{ "index": number, "suggested": { "title": "...", "organization": "...", "description": "...", "startDate": "...", "isCurrent": boolean }, "reason": "..." }>;
                    "educations"?: Array<{ "index": number, "suggested": { "institution": "...", "degree": "...", "field": "...", "graduationDate": "..." }, "reason": "..." }>;
                    "projects"?: Array<{ "index": number, "suggested": { "name": "...", "description": "...", "type": "..." }, "reason": "..." }>;
                    "skills"?: { "current": ["..."], "suggested": ["..."], "reason": "..." };
                  };
                }
                Skills must be atomized (no slashes, parentheses, or modifiers).
            """.trimIndent(),
            performance = "MUST return ONLY the JSON object above and nothing else. The response must parse with JSON.parse()."
        )
    }

    /**
     * Generates a prompt for Single Content Improvement.
     * Matches Web: actions/resume.ts/improveWithAI
     */
    fun createContentImprovementPrompt(
        currentContent: String,
        contentType: String
    ): String {
        return createPrompt(
            context = "You are rewriting a piece of resume content ($contentType) to improve impact and ATS alignment. Original content: $currentContent",
            role = PERSONA_ATS_EXPERT,
            instruction = "Rewrite the provided content to be more impactful, professional, and results-driven. Use action verbs and focus on achievements using the formula: [Action Verb] + [Task] + [Impact/Result]. Do NOT invent facts or tools not present in the original text.",
            specification = "Produce exactly 3-4 professional sentences. Each output sentence should be a single line starting with \"-\". Strictly English. Return ONLY the improved paragraph with no additional commentary.",
            performance = "Avoid filler language; prefer measurable impact. Preserve truthfulness and ensure improved readability and ATS keyword relevance."
        )
    }

    /**
     * Generates a prompt for Interview Question Generation.
     * Combines logic from Web: actions/interview.ts (generateQuiz & generateInterviewQuestions)
     */
    fun createInterviewQuestionsPrompt(
        context: String,
        focusTopic: String,
        questionCount: Int = 5,
        historyContext: String = "",
        includeQuiz: Boolean = true,
        includeOpenEnded: Boolean = true
    ): String {
        val instructions = mutableListOf<String>()
        instructions.add("Context: $context")
        instructions.add("Focus Topic: $focusTopic")
        if (historyContext.isNotBlank()) instructions.add("History Context: $historyContext")

        if (includeQuiz) {
            instructions.add("Produce $questionCount multiple-choice technical interview questions. Each question must have 4 options, one correct answer, and a clear explanation for the correct answer.")
        }
        if (includeOpenEnded) {
            instructions.add("Create $questionCount clear, professional interview questions that focus on practical skills and real-world project experience. For each question, provide a correct answer/explanation.")
        }

        val outputSpec = """
            Return STRICT JSON:
            {
               ${if (includeQuiz) """"quizQuestions": [ { "id": "q1", "question": "...", "options": ["A", "B", "C", "D"], "correctAnswerIndex": 0, "explanation": "..." } ],""" else ""}
               ${if (includeOpenEnded) """"interviewQuestions": [ { "id": "i1", "question": "...", "correctAnswer": "..." } ],""" else ""}
               "practiceTips": [ { "category": "General", "icon": "lightbulb", "color": "#FFC107", "tips": ["Tip 1"] } ],
               "coachingNotes": { "summary": "...", "improvementAreas": [], "recommendedPracticeFrequency": "..." }
            }
             IMPORTANT:
             - Return ONLY the JSON.
        """.trimIndent()
        
        return createPrompt(
            context = "CANDIDATE PROFILE:\n$context",
            role = PERSONA_INTERVIEWER,
            instruction = instructions.joinToString("\n"),
            specification = outputSpec,
            performance = "Ensure questions are relevant to the profile and experience level. Return ONLY JSON."
        )
    }

    /**
     * Generates a prompt for a single follow-up interview question.
     */
    fun createSingleInterviewQuestionPrompt(
        context: String,
        focusTopic: String,
        previousQuestions: List<String>
    ): String {
        val historyBlock = if (previousQuestions.isNotEmpty()) {
            "PREVIOUS QUESTIONS (Avoid Duplicates):\n- " + previousQuestions.joinToString("\n- ")
        } else ""

        val outputSpec = """
            Return STRICT JSON:
            {
              "question": "The interview question text",
              "idealAnswer": "Key bullet points for a good answer"
            }
             IMPORTANT: Return ONLY the JSON.
        """.trimIndent()

        return createPrompt(
            context = "CANDIDATE PROFILE:\n$context\n\nINTERVIEW HISTORY:\n$historyBlock",
            role = PERSONA_INTERVIEWER,
            instruction = "Generate ONE single OPEN_ENDED interview question. Focus Topic: $focusTopic. The question should be clear, professional, and focus on practical skills. Avoid questions already asked.",
             specification = outputSpec,
            performance = "Relevant and non-repetitive."
        )
    }
    
    /**
     * Generates a prompt for Audio Feedback.
     * Matches Web: actions/interview.ts/saveLiveInterviewResult (adapted for single)
     */
    fun createAudioFeedbackPrompt(
        context: String,
        question: String,
        userAnswer: String,
        category: String
    ): String {
        
        val outputSpec = """
            Return STRICT JSON:
            {
              "rating": 8,
              "feedback": "Detailed feedback text..."
            }
            IMPORTANT: Return ONLY the JSON.
        """.trimIndent()
        
        return createPrompt(
             context = "CANDIDATE PROFILE:\n$context\n\nQUESTION CONTEXT:\n- Category: $category\n- Question: $question\n- Candidate Answer: $userAnswer",
             role = PERSONA_INTERVIEWER,
             instruction = "Evaluate the answer. Focus on the knowledge gaps revealed by this answer. Keep the response under 2 sentences and make it encouraging. Do not explicitly call out user mistakes; instead, suggest what to learn or practice.",
             specification = outputSpec,
             performance = "Constructive, actionable feedback."
        )
    }

    /**
     * Generates a prompt for Batch Audio Feedback.
     * Matches Web: actions/interview.ts/saveLiveInterviewResult
     */
    fun createBatchFeedbackPrompt(
        context: String,
        feedbackInput: List<Triple<String, String, String>>
    ): String {
        
        val questionsText = feedbackInput.mapIndexed { index, (question, answer, category) ->
            """
            QUESTION_${index + 1} ($category):
            Q: $question
            A: $answer
            """.trimIndent()
        }.joinToString("\n\n")

        val outputSpec = """
            Return STRICT JSON:
            {
               "feedbackList": [
                  {
                    "questionIndex": 1,
                    "rating": 8,
                    "feedback": "Detailed feedback..."
                  }
               ],
               "overallSummary": "Optional summary of performance"
            }
            IMPORTANT: Ensure one entry in 'feedbackList' for each question. Return ONLY JSON.
        """.trimIndent()

        return createPrompt(
             context = "CANDIDATE PROFILE:\n$context\n\nINTERVIEW SESSION:\n$questionsText",
             role = PERSONA_INTERVIEWER,
             instruction = "Evaluate the candidate's answers. Focus on the knowledge gaps revealed by these answers. Keep the response under 2 sentences and make it encouraging. Do not explicitly call out user mistakes; instead, focus on what to learn/practice.",
             specification = outputSpec,
             performance = "Constructive, actionable feedback. Accurate rating (1-10)."
        )
    }

    /**
     * Generates a prompt for Industry Insights.
     * Matches Web: actions/dashboard.ts/genarateAIIsignts
     */
    fun createIndustryInsightsPrompt(industry: String): String {
        val output = """
            OUTPUT FORMAT:
            Return STRICT JSON in this shape:
            {
              "salaryRanges": [
                { "role": "string", "min": number, "max": number, "median": number, "location": "string" }
              ],
              "growthRate": number,
              "demandLevel": "High" | "Medium" | "Low",
              "topSkills": ["skill1", "skill2"],
              "marketOutlook": "Positive" | "Neutral" | "Negative",
              "keyTrends": ["trend1", "trend2"],
              "recommendedSkills": ["skill1", "skill2"]
            }
            Include at least 5 roles and at least 5 skills/trends. Growth rate must be a percentage number.
            IMPORTANT: Return ONLY the JSON. No notes.
        """.trimIndent()
        
        return """
            $PERSONA_MARKET_ANALYST
            
            TOPIC: 
            Industry: $industry
            
            INSTRUCTIONS:
            Analyze the current state of the $industry industry and return structured market insights. 
            Provide market-level insights including salary ranges, growth rate, demand level, top skills, market outlook, key trends, and recommended skills.
            
            $output
        """.trimIndent()
    }

    /**
     * Generates a prompt for Practice Tips and Coaching Notes only.
     */
    fun createTipsPrompt(
        context: String,
        historyContext: String = ""
    ): String {

        val instructions = """
            INSTRUCTIONS:
            Create practice tips and coaching notes for the candidate.
            - Consider their experience level and recent performance.
            - Provide actionable advice to improve interview readiness.
            - History Context: ${"$"}{historyContext}
        """.trimIndent()

        val output = """
            OUTPUT FORMAT:
            Return STRICT JSON:
            {
               "practiceTips": [
                  { "category": "General", "icon": "lightbulb", "color": "#FFC107", "tips": ["Tip 1"] }
               ],
               "coachingNotes": { "summary": "...", "improvementAreas": [], "recommendedPracticeFrequency": "..." }
            }
             IMPORTANT:
             - Return ONLY the JSON.
        """.trimIndent()

        return """
            ${"$"}{PERSONA_INTERVIEWER}
            
            ${"$"}{context}
            
            ${"$"}{instructions}
            
            ${"$"}{output}
        """.trimIndent()
    }
}

