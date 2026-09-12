package com.example.data.ai

import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class QuizQuestion(
    val question: String,
    val options: List<String>,
    val correctIndex: Int,
    val explanation: String
)

data class GeneratedFlashcard(
    val front: String,
    val back: String
)

class GeminiService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun explainTopic(topic: String, subject: String): String = withContext(Dispatchers.IO) {
        val prompt = """
            You are an expert university professor. Explain the topic '$topic' in '$subject' for a college exam.
            Provide the response in the following clear format:
            ### 📌 Overview
            (A simple, crystal-clear 2-sentence explanation)
            
            ### 🔑 Core Concepts
            (3-4 bullet points highlighting essential mechanisms)
            
            ### 💡 Concrete Example
            (A short, relatable practical or numerical example)
            
            ### 🎯 Exam Tips & Common Pitfalls
            (What examiners look for and common mistakes to avoid)
        """.trimIndent()

        val response = callGemini(prompt)
        if (!response.isNullOrBlank() && !response.startsWith("Error:")) {
            response
        } else {
            getCuratedExplanation(topic, subject)
        }
    }

    suspend fun generateQuiz(topic: String, subject: String): List<QuizQuestion> = withContext(Dispatchers.IO) {
        val prompt = """
            Generate 3 multiple choice questions for university students on '$topic' in '$subject'.
            Return ONLY a raw JSON array of objects with keys:
            "question" (string), "options" (array of 4 strings), "correctIndex" (integer 0-3), "explanation" (string).
            Do not include markdown ticks or explanation outside JSON.
        """.trimIndent()

        val response = callGemini(prompt)
        if (!response.isNullOrBlank()) {
            val parsed = parseQuizJson(response)
            if (parsed.isNotEmpty()) return@withContext parsed
        }
        getCuratedQuiz(topic, subject)
    }

    suspend fun generateFlashcards(topic: String, subject: String): List<GeneratedFlashcard> = withContext(Dispatchers.IO) {
        val prompt = """
            Generate 4 high-yield study flashcards for '$topic' in '$subject'.
            Return ONLY a raw JSON array of objects with keys:
            "front" (string - concise question or concept), "back" (string - concise answer or definition).
            Do not include markdown ticks or text outside JSON.
        """.trimIndent()

        val response = callGemini(prompt)
        if (!response.isNullOrBlank()) {
            val parsed = parseFlashcardsJson(response)
            if (parsed.isNotEmpty()) return@withContext parsed
        }
        getCuratedFlashcards(topic, subject)
    }

    suspend fun generateAiStudyAdvice(hours: Int, subjects: List<String>, exams: List<String>): String = withContext(Dispatchers.IO) {
        val prompt = """
            You are an expert academic advisor. The student has $hours hours to study today.
            Their active subjects are: ${subjects.joinToString(", ")}.
            Upcoming exams: ${exams.joinToString(", ")}.
            Give a high-impact, motivational 3-step study strategy for today with time-blocking advice.
        """.trimIndent()

        val response = callGemini(prompt)
        if (!response.isNullOrBlank() && !response.startsWith("Error:")) {
            response
        } else {
            "🚀 **Today's Strategic Focus** ($hours Hours Available)\n\n" +
                    "1. **Peak Cognitive Period (60m)**: Start with your closest exam (${exams.firstOrNull() ?: "Urgent Subjects"}). Focus on understanding active derivations or high-yield definitions.\n\n" +
                    "2. **Active Problem Solving (45m)**: Work through 2-3 past exam questions. Avoid passive reading.\n\n" +
                    "3. **Spaced Recall & Flashcards (30m)**: Review weak flashcards and test yourself without notes for rapid retention."
        }
    }

    private fun callGemini(prompt: String): String? {
        return try {
            val apiKey = try {
                BuildConfig::class.java.getField("GEMINI_API_KEY").get(null) as? String ?: ""
            } catch (e: Exception) {
                ""
            }

            if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
                Log.d("GeminiService", "No valid GEMINI_API_KEY found, using curated fallback")
                return null
            }

            val requestJson = JSONObject().apply {
                val contents = JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().put("text", prompt))
                        })
                    })
                }
                put("contents", contents)
            }

            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.6-flash:generateContent?key=$apiKey"
            val body = requestJson.toString().toRequestBody(jsonMediaType)
            val request = Request.Builder()
                .url(url)
                .post(body)
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val resBody = response.body?.string() ?: return null
                val root = JSONObject(resBody)
                val candidates = root.optJSONArray("candidates")
                val content = candidates?.optJSONObject(0)?.optJSONObject("content")
                val parts = content?.optJSONArray("parts")
                parts?.optJSONObject(0)?.optString("text")
            } else {
                Log.w("GeminiService", "Gemini API error code: ${response.code}")
                null
            }
        } catch (e: Exception) {
            Log.e("GeminiService", "Gemini call failed", e)
            null
        }
    }

    private fun parseQuizJson(raw: String): List<QuizQuestion> {
        val clean = extractJsonBlock(raw)
        val list = mutableListOf<QuizQuestion>()
        try {
            val array = JSONArray(clean)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val q = obj.getString("question")
                val optArr = obj.getJSONArray("options")
                val opts = mutableListOf<String>()
                for (j in 0 until optArr.length()) {
                    opts.add(optArr.getString(j))
                }
                val correct = obj.getInt("correctIndex")
                val exp = obj.optString("explanation", "Correct answer verified.")
                list.add(QuizQuestion(q, opts, correct, exp))
            }
        } catch (e: Exception) {
            Log.w("GeminiService", "Failed to parse quiz json: $raw", e)
        }
        return list
    }

    private fun parseFlashcardsJson(raw: String): List<GeneratedFlashcard> {
        val clean = extractJsonBlock(raw)
        val list = mutableListOf<GeneratedFlashcard>()
        try {
            val array = JSONArray(clean)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val front = obj.getString("front")
                val back = obj.getString("back")
                list.add(GeneratedFlashcard(front, back))
            }
        } catch (e: Exception) {
            Log.w("GeminiService", "Failed to parse flashcard json: $raw", e)
        }
        return list
    }

    private fun extractJsonBlock(text: String): String {
        val trimmed = text.trim()
        val startIndex = trimmed.indexOf('[')
        val endIndex = trimmed.lastIndexOf(']')
        return if (startIndex != -1 && endIndex != -1 && endIndex > startIndex) {
            trimmed.substring(startIndex, endIndex + 1)
        } else {
            trimmed
        }
    }

    // --- Curated Academic Fallbacks ---
    private fun getCuratedExplanation(topic: String, subject: String): String {
        return when {
            topic.contains("Normalization", ignoreCase = true) -> """
                ### 📌 Overview
                Normalization is a systematic database design technique to minimize data redundancy and prevent insert, update, and delete anomalies by decomposing relations into progressively cleaner forms.
                
                ### 🔑 Core Concepts
                • **1NF**: Atomic column values only (no repeating groups or arrays).
                • **2NF**: In 1NF + NO partial functional dependencies (every non-prime attribute fully depends on candidate keys).
                • **3NF**: In 2NF + NO transitive dependencies (non-prime attributes cannot determine other non-prime attributes).
                • **BCNF**: Strict 3NF where every determinant (X in X → Y) MUST be a superkey.
                
                ### 💡 Concrete Example
                In an `Enrollments(StudentID, CourseID, ProfessorName)` table, if `CourseID → ProfessorName`, then `ProfessorName` partially depends on `CourseID`. Decompose into `Enrollments(StudentID, CourseID)` and `Courses(CourseID, ProfessorName)`.
                
                ### 🎯 Exam Tips & Common Pitfalls
                • Always identify all candidate keys first before checking for 2NF/3NF.
                • Lossless-join decomposition and dependency preservation are the two golden criteria evaluated in university exams!
            """.trimIndent()

            topic.contains("Backpropagation", ignoreCase = true) || topic.contains("ANN", ignoreCase = true) -> """
                ### 📌 Overview
                Backpropagation is the fundamental algorithm for training artificial neural networks. It calculates the gradient of the loss function with respect to every weight using the calculus chain rule.
                
                ### 🔑 Core Concepts
                • **Forward Pass**: Input data propagates forward layer by layer to generate predictions and calculate the loss.
                • **Loss Gradient**: Measures the instantaneous rate of error with respect to the output layer activations.
                • **Chain Rule Propagation**: Errors are propagated backward through hidden layers by multiplying local partial derivatives.
                • **Weight Update**: Weights are adjusted in the opposite direction of the gradient: W = W - α * (∂L/∂W).
                
                ### 💡 Concrete Example
                For a 2-layer network, ∂Loss/∂W1 = (∂Loss/∂Output) × (∂Output/∂Hidden) × (∂Hidden/∂W1).
                
                ### 🎯 Exam Tips & Common Pitfalls
                • Watch out for the vanishing gradient problem in deep networks using Sigmoid or Tanh; modern architectures use ReLU or GELU to mitigate this.
            """.trimIndent()

            topic.contains("Hadamard", ignoreCase = true) || topic.contains("Qubit", ignoreCase = true) -> """
                ### 📌 Overview
                A Hadamard gate (H-gate) is a single-qubit quantum unitary operation that transforms computational basis states into equal superpositions.
                
                ### 🔑 Core Concepts
                • Matrix: H = 1/√2 × [[1, 1], [1, -1]].
                • Effect on |0⟩: Transforms into |+⟩ = (|0⟩ + |1⟩) / √2.
                • Effect on |1⟩: Transforms into |-⟩ = (|0⟩ - |1⟩) / √2.
                • Reversibility: H is its own inverse (H × H = I, where I is identity).
                
                ### 💡 Concrete Example
                Measuring a qubit in state H|0⟩ yields outcome 0 with 50% probability and outcome 1 with 50% probability (|1/√2|² = 1/2).
                
                ### 🎯 Exam Tips & Common Pitfalls
                • Remember that H² = I. Applying Hadamard twice restores the initial quantum state.
            """.trimIndent()

            else -> """
                ### 📌 Overview
                $topic is a pivotal concept in $subject that is frequently tested on semester exams and technical assessments.
                
                ### 🔑 Core Concepts
                • Understand theoretical foundations and the mathematical or logical formalisms.
                • Review the core algorithms, data flow, and underlying state transformations.
                • Contrast with related paradigms and identify trade-offs (time, memory, efficiency).
                
                ### 💡 Concrete Example
                Analyze standard textbook scenarios and examine edge cases where boundary conditions alter the expected behavior.
                
                ### 🎯 Exam Tips & Common Pitfalls
                • Focus on step-by-step proofs, diagrams, and time-complexity breakdowns.
            """.trimIndent()
        }
    }

    private fun getCuratedQuiz(topic: String, subject: String): List<QuizQuestion> {
        return if (topic.contains("Normalization", ignoreCase = true) || subject.contains("DBMS", ignoreCase = true)) {
            listOf(
                QuizQuestion(
                    question = "Which normal form requires removing transitive dependencies?",
                    options = listOf("First Normal Form (1NF)", "Second Normal Form (2NF)", "Third Normal Form (3NF)", "Boyce-Codd Normal Form (BCNF)"),
                    correctIndex = 2,
                    explanation = "3NF eliminates transitive dependencies (X → Y and Y → Z where Z is non-prime)."
                ),
                QuizQuestion(
                    question = "What condition defines BCNF compared to standard 3NF?",
                    options = listOf(
                        "Every determinant must be a candidate/superkey",
                        "Must allow multivalued dependencies",
                        "Must contain no foreign keys",
                        "Must have fewer than 5 columns"
                    ),
                    correctIndex = 0,
                    explanation = "For every non-trivial functional dependency X → Y in BCNF, X must be a superkey."
                ),
                QuizQuestion(
                    question = "Which anomaly occurs when deleting a student record unintentionally deletes course details?",
                    options = listOf("Insertion anomaly", "Deletion anomaly", "Update anomaly", "Read anomaly"),
                    correctIndex = 1,
                    explanation = "Deletion anomaly occurs when unrelated data is lost due to poorly normalized schemas."
                )
            )
        } else if (topic.contains("Quantum", ignoreCase = true) || subject.contains("Quantum", ignoreCase = true)) {
            listOf(
                QuizQuestion(
                    question = "What is the result of applying a Hadamard gate to the basis state |0⟩?",
                    options = listOf("|0⟩", "|1⟩", "(|0⟩ + |1⟩) / √2", "(|0⟩ - |1⟩) / √2"),
                    correctIndex = 2,
                    explanation = "The Hadamard gate maps |0⟩ to the equal superposition state |+⟩ = (|0⟩ + |1⟩) / √2."
                ),
                QuizQuestion(
                    question = "What is the mathematical property of quantum gates representing reversible operations?",
                    options = listOf("Unitary matrices", "Singular matrices", "Stochastic vectors", "Nilpotent operators"),
                    correctIndex = 0,
                    explanation = "All valid quantum logic gates must be unitary operators (U†U = I) to preserve quantum probability."
                ),
                QuizQuestion(
                    question = "Which two-qubit gate is commonly used to create quantum entanglement?",
                    options = listOf("Pauli-X gate", "CNOT (Controlled-NOT) gate", "Phase gate", "Identity gate"),
                    correctIndex = 1,
                    explanation = "A Hadamard gate followed by a CNOT gate produces maximally entangled Bell states."
                )
            )
        } else {
            listOf(
                QuizQuestion(
                    question = "What is the primary role of the activation function in Neural Networks?",
                    options = listOf("Store weights", "Introduce non-linearity", "Normalize input images", "Calculate matrix transpose"),
                    correctIndex = 1,
                    explanation = "Activation functions introduce non-linearity, allowing networks to learn complex non-linear functions."
                ),
                QuizQuestion(
                    question = "Which algorithm is used to compute gradients via the chain rule?",
                    options = listOf("Dijkstra's Algorithm", "Backpropagation", "Merge Sort", "K-Means"),
                    correctIndex = 1,
                    explanation = "Backpropagation applies the calculus chain rule backwards from loss to input layers."
                ),
                QuizQuestion(
                    question = "What problem can occur in deep networks when gradients approach zero?",
                    options = listOf("Exploding weights", "Vanishing gradient problem", "Underflow memory leak", "Overfitting penalty"),
                    correctIndex = 1,
                    explanation = "The vanishing gradient problem prevents early layers from training effectively."
                )
            )
        }
    }

    private fun getCuratedFlashcards(topic: String, subject: String): List<GeneratedFlashcard> {
        return listOf(
            GeneratedFlashcard(
                front = "What is the core definition of $topic?",
                back = "A foundational principle in $subject designed to optimize academic and practical performance."
            ),
            GeneratedFlashcard(
                front = "What is the main advantage or use-case of $topic?",
                back = "Provides structured problem solving, eliminates redundancies, and guarantees system correctness."
            ),
            GeneratedFlashcard(
                front = "What is a frequent exam question on $topic?",
                back = "Differentiating between edge cases, computing algorithmic complexity, and showing step-by-step transformations."
            ),
            GeneratedFlashcard(
                front = "Key formula or rule to remember for $topic?",
                back = "Check constraints, verify invariants, and apply the standard verification checklist before finalizing solutions."
            )
        )
    }
}
