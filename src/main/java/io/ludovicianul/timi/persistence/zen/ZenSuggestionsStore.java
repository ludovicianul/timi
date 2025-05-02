package io.ludovicianul.timi.persistence.zen;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.inject.Singleton;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

@Singleton
public class ZenSuggestionsStore {

  private final Map<String, Map<String, List<String>>> suggestions;

  public ZenSuggestionsStore() {
    suggestions = initializeSuggestions();
  }

  private Map<String, Map<String, List<String>>> initializeSuggestions() {
    Path suggestionsFile =
        Path.of(System.getProperty("user.home"), ".timi", "zen-suggestions.json");
    if (Files.exists(suggestionsFile)) {
      try {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(suggestionsFile.toFile(), new TypeReference<>() {});
      } catch (IOException e) {
        System.err.println("⚠️ Failed to load zen suggestions from file, using defaults.");
      }
    }
    return loadDefaultSuggestions();
  }

  public String pick(String mood, String persona, Map<String, String> context) {
    List<String> options =
        Optional.ofNullable(suggestions.getOrDefault(persona, Map.of()).get(mood))
            .orElse(
                suggestions
                    .getOrDefault("default", Map.of())
                    .getOrDefault(mood, List.of("Keep showing up. Reflection is a practice.")));

    if (options.isEmpty()) {
      return "...";
    }
    String template = options.get(new Random().nextInt(options.size()));
    for (Map.Entry<String, String> entry : context.entrySet()) {
      template = template.replace("{" + entry.getKey() + "}", entry.getValue());
    }
    return template;
  }

  private static Map<String, Map<String, List<String>>> loadDefaultSuggestions() {
    Map<String, Map<String, List<String>>> all = new HashMap<>();

    all.put(
        "coach",
        Map.of(
            "CALM",
                List.of(
                    "🌿 You stayed in flow for {duration}. Protect that focus tomorrow.",
                    "🧠 Your effort on {topType} paid off. Keep the deep work rolling.",
                    "📘 Fewer distractions = more impact. You’re on the right track.",
                    "🔒 Solid, quiet focus. That’s how momentum builds.",
                    "🎧 You protected your attention. That’s not easy — good job."),
            "INTENTIONAL",
                List.of(
                    "📦 {sessions} sessions logged — tidy and purposeful.",
                    "🛠️ Your day had structure. That’s a big win.",
                    "🎯 You led with {topType}. That’s where your energy went.",
                    "💡 Consider ending tomorrow with the same intention you started with today.",
                    "🚪 You closed loops. Maybe keep one open to start with tomorrow."),
            "SCATTERED",
                List.of(
                    "🌀 You jumped across {sessions} sessions. Can you defend one deeper block tomorrow?",
                    "📎 Lots of {topType} today. Was that strategy, or survival?",
                    "🔄 Reclaim your morning. Start without switching for 90 minutes.",
                    "📍 Schedule creates space. Can you control it better tomorrow?",
                    "🚨 Fragmentation alert. What could you skip?"),
            "LIGHT",
                List.of(
                    "🌱 You logged {duration} — light but intentional.",
                    "🕯️ Sometimes less is more. Reset days matter.",
                    "🧘 Next step: one focused hour. That’s all it takes.",
                    "🧩 Even one puzzle piece adds to the picture.",
                    "🪴 Growth is quiet sometimes. Be patient.")));

    all.put(
        "zen",
        Map.of(
            "CALM",
                List.of(
                    "🪷 Still waters. Deep thoughts. You and {topType}.",
                    "🌾 Today unfolded like a breath. {duration} well spent.",
                    "🍵 Your mind moved without noise. Keep the rhythm tomorrow.",
                    "🧘 {duration} of calm. Rest in that shape of the day.",
                    "🔔 You listened well to the silence between tasks."),
            "INTENTIONAL",
                List.of(
                    "🎋 The structure held. {sessions} efforts laid out with grace.",
                    "🌞 You gave the day shape. {topType} lit the way.",
                    "📖 Your energy had a narrative today.",
                    "🗺️ A thoughtful journey — next, choose one new path.",
                    "🏮 Carry today’s clarity into tomorrow’s uncertainty."),
            "SCATTERED",
                List.of(
                    "🌪️ Stormy skies. Can you walk into tomorrow with an umbrella of intention?",
                    "🌫️ Noise stole your stillness. Can you reclaim the first hour tomorrow?",
                    "🌀 Many winds pulled at your sails. Trim them gently.",
                    "🔄 Motion ≠ meaning. What would have felt enough today?",
                    "🎐 Tune your day like a wind chime. Don’t let the gusts define it."),
            "LIGHT",
                List.of(
                    "🌙 A soft breath of a day. Breathe it in.",
                    "📉 Light doesn’t mean lost. It just means space.",
                    "🛋️ Stillness has value. But make sure it’s chosen.",
                    "🧵 A loose thread. Tomorrow, tie a small knot.",
                    "⏳ Time passed slowly. Let tomorrow shape a bit of form.")));

    all.put(
        "snarky",
        Map.of(
            "CALM",
                List.of(
                    "🔥 Whoa. Actual focus. I’m genuinely impressed.",
                    "🧠 Did you drink the deep work Kool-Aid?",
                    "🕶️ One more day like this and you’ll transcend reality.",
                    "💯 A clean run of {duration}? Who are you even?",
                    "👻 We have no notes. Just spooky levels of productivity."),
            "INTENTIONAL",
                List.of(
                    "😏 You actually showed up. Look at you.",
                    "📚 Typed ‘intentional’ into your calendar and made it happen.",
                    "👔 Solid. Professional. Mildly boring. Good job.",
                    "📈 That’s a growth chart I wouldn’t be ashamed to show.",
                    "🧑‍💼 Welcome to the club of adults who do stuff."),
            "SCATTERED",
                List.of(
                    "🤹‍♂️ Juggled 7 things and dropped 3. Classic.",
                    "📎 You spent most of the day in ‘oh wait’ mode.",
                    "😵‍💫 That was chaos. Hopefully strategic?",
                    "🔥 Your brain needs a fire break. Good luck.",
                    "🫠 Try doing less. But better. Just once."),
            "LIGHT",
                List.of(
                    "🛋️ A productivity nap. Bold move.",
                    "🐌 Slow day, or just low bandwidth?",
                    "☕ Did you log coffee breaks as sessions again?",
                    "😬 That was barely a cameo.",
                    "👻 Ghosted your own calendar. Impressive.")));

    return all;
  }
}
