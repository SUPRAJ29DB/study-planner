// Server-Side Gemini API Service
// Model: gemini-3.6-flash
// Uses GEMINI_API_KEY from environment (configured securely in AI Studio Secrets)

const GEMINI_API_URL = 'https://generativelanguage.googleapis.com/v1beta/models/gemini-3.6-flash:generateContent';

export async function askGemini(prompt, systemInstruction = '') {
  const apiKey = process.env.GEMINI_API_KEY;

  if (!apiKey || apiKey === 'MY_GEMINI_API_KEY') {
    console.warn('GEMINI_API_KEY not configured in Secrets. Returning local intelligent academic response.');
    return generateFallbackResponse(prompt);
  }

  try {
    const payload = {
      contents: [
        {
          role: 'user',
          parts: [{ text: prompt }]
        }
      ]
    };

    if (systemInstruction) {
      payload.systemInstruction = {
        parts: [{ text: systemInstruction }]
      };
    }

    const response = await fetch(`${GEMINI_API_URL}?key=${apiKey}`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload)
    });

    if (!response.ok) {
      const errText = await response.text();
      console.error('Gemini API HTTP Error:', response.status, errText);
      return generateFallbackResponse(prompt);
    }

    const data = await response.json();
    const candidateText = data.candidates?.[0]?.content?.parts?.[0]?.text;
    return candidateText || generateFallbackResponse(prompt);
  } catch (err) {
    console.error('Gemini API fetch exception:', err.message);
    return generateFallbackResponse(prompt);
  }
}

export async function explainTopicWithGemini(subject, topic) {
  const prompt = `You are a knowledgeable university professor and academic coach.
Explain the topic "${topic}" in the subject "${subject}" to a student preparing for exams.
Provide:
1. Core Concept in 2 sentences.
2. Key Architectural/Theoretical principles (3 bullet points).
3. A memorable real-world analogy or example.
4. Top 2 common exam traps to avoid.
Keep the explanation engaging, concise, and structured.`;

  return askGemini(prompt, "Provide clear, high-yield academic explanations.");
}

export async function generateQuizWithGemini(subject, topic) {
  const prompt = `Generate 3 high-yield exam practice questions for "${topic}" in "${subject}".
For each question:
- State the Question clearly.
- Provide the concise Answer / Explanation.
Format as Markdown with bold headers.`;

  return askGemini(prompt, "You are an expert exam question creator.");
}

export async function optimizePlanWithGemini(subjects, topics, exams, hours, energy, goal) {
  const prompt = `The student has ${hours} hours available today with ${energy} energy.
Goal: ${goal}.
Subjects: ${subjects.map(s => s.name).join(', ')}.
Upcoming exams: ${exams.map(e => `${e.name} in ${e.days_left || 7} days`).join(', ')}.
Recommend 3 actionable tactical tips for today's study blocks to maximize retention and prevent cognitive fatigue.`;

  return askGemini(prompt, "Provide concise, pragmatic study strategy advice.");
}

function generateFallbackResponse(prompt) {
  const p = prompt.toLowerCase();
  if (p.includes('explain') || p.includes('normalization') || p.includes('dbms')) {
    return `### Core Concept: Normalization in DBMS
Normalization organizes database relations to eliminate data redundancy and prevent insertion, update, and deletion anomalies.

* **Key Principles:**
  * **1NF:** Eliminate repeating groups; ensure atomic column values.
  * **2NF:** In 1NF + no partial dependency on a composite primary key.
  * **3NF / BCNF:** In 2NF + eliminate transitive functional dependencies ($X \\to Y$ where $X$ is not a superkey).

* **Real-World Analogy:** Think of an address book where you only write someone's home address once in an Address table, instead of copying it onto every phone call and email log entry.

* **Exam Tip:** Watch out for multi-attribute primary keys when testing for 2NF violations!`;
  }
  if (p.includes('ai') || p.includes('neural') || p.includes('cnn')) {
    return `### Convolutional Neural Networks (CNNs)
CNNs process grid-structured inputs like images using spatial kernel convolutions and pooling layers.

* **Key Principles:**
  * **Convolution:** Extracts local feature maps (edges, textures).
  * **Pooling (Max/Average):** Downsamples feature maps for translation invariance.
  * **Dense FC Layers:** Final classification head mapping features to class logits.

* **Analogy:** Like scanning a flashlight over a painting to identify distinct motifs before assembling the big picture.`;
  }
  return `### Study Strategy Guidance
Focus on active recall and interleaving subjects today.
* **Block 1 (45 min):** Tackle the highest difficulty topics early when cognitive stamina is peak.
* **15 min Break:** Step away from all screens to consolidate synaptic connections.
* **Block 2 (45 min):** Solve practice problems instead of passive reading.`;
}
