package me.gaurang.sellmod.core.detect;

import me.gaurang.sellmod.core.config.ModConfig;
import me.gaurang.sellmod.core.port.MenuSnapshot;
import me.gaurang.sellmod.core.port.MenuSlot;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Pure sell/confirm-button scorer. Automatic mode is deliberately heuristic
 * rather than plugin-specific; all Minecraft data extraction happens in the
 * adapter before reaching this class.
 */
public final class ButtonScorer {
    private static final int AUTOMATIC_THRESHOLD = 35;
    private static final int AUTOMATIC_MARGIN = 8;

    private static final Set<String> STRONG_SELL_WORDS = Set.of(
            "sell", "sellall", "sellallitems", "verkaufen", "vender", "vendre", "venderme", "vendi",
            "vendere", "verkopen", "sprzedaj", "sprzedawać", "sprzedawac", "prodat", "predat",
            "prodaj", "пpодать", "продать", "продажа", "продати", "sat", "satmak", "vinde",
            "elad", "eladás", "πωληση", "売る", "売却", "出售", "卖", "판매", "팔기",
            "بيع", "bán", "ban", "ขาย"
    );

    private static final Set<String> ACTION_WORDS = Set.of(
            "confirm", "confirmed", "confirmation", "accept", "accepted", "acceptall", "done", "submit",
            "continue", "sellitems", "verkaufen", "bestatigen", "bestätigen", "annehmen", "akzeptieren",
            "confirmar", "aceptar", "confirmer", "accepter", "conferma", "confermare", "accetta",
            "bevestigen", "accepteren", "potwierdz", "akceptuj", "potvrdit", "prijmout", "подтвердить",
            "принять", "підтвердити", "прийняти", "onayla", "kabul", "confirma", "accepta", "megerősít",
            "elfogad", "επιβεβαίωση", "αποδοχή", "確認", "確定", "确定", "确认", "수락", "확인",
            "تأكيد", "قبول", "ยืนยัน"
    );

    private ButtonScorer() {
    }

    public static MenuSlot findBest(MenuSnapshot snapshot, ModConfig config) {
        if (snapshot == null) {
            return null;
        }

        List<ScoredSlot> candidates = new ArrayList<>();
        int topSlots = topSlots(snapshot);
        for (MenuSlot slot : snapshot.slots()) {
            if (slot.playerOwned() || !slot.hasItem()) {
                continue;
            }

            int score = config.buttonDetectionMode == ModConfig.ButtonDetectionMode.MANUAL_MATERIAL
                    ? scoreManual(slot, config.sellButtonMaterial, topSlots)
                    : scoreAutomatic(slot, topSlots);
            if (score > 0) {
                candidates.add(new ScoredSlot(slot, score));
            }
        }

        if (candidates.isEmpty()) {
            return null;
        }

        candidates.sort(Comparator.comparingInt(ScoredSlot::score).reversed());
        ScoredSlot best = candidates.get(0);
        if (config.buttonDetectionMode == ModConfig.ButtonDetectionMode.HYBRID_AUTOMATIC) {
            int second = candidates.size() > 1 ? candidates.get(1).score() : 0;
            if (best.score() < AUTOMATIC_THRESHOLD || (candidates.size() > 1 && best.score() - second < AUTOMATIC_MARGIN)) {
                return null;
            }
        }
        return best.slot();
    }

    private static int topSlots(MenuSnapshot snapshot) {
        int topSlots = 0;
        for (MenuSlot other : snapshot.slots()) {
            if (!other.playerOwned()) {
                topSlots = Math.max(topSlots, other.index() + 1);
            }
        }
        return topSlots;
    }

    private static int scoreManual(MenuSlot slot, String configuredMaterial, int topSlots) {
        if (configuredMaterial == null || configuredMaterial.isBlank()) {
            return 0;
        }
        String actual = slot.itemId();
        if (!actual.equalsIgnoreCase(configuredMaterial.trim())) {
            return 0;
        }

        // Material is the hard requirement in manual mode; context only chooses the
        // most plausible matching button when the GUI happens to contain duplicates.
        return 100 + positionalScore(slot.index(), topSlots);
    }

    private static int scoreAutomatic(MenuSlot slot, int topSlots) {
        String id = slot.itemId().toLowerCase(Locale.ROOT);
        String path = slot.itemPath().toLowerCase(Locale.ROOT);
        String text = slot.displayText();

        String normalized = normalize(text);
        String normalizedId = normalize(id);
        int score = 0;

        for (String word : STRONG_SELL_WORDS) {
            if (containsToken(normalized, word) || containsToken(normalizedId, word)) {
                score += 55;
                break;
            }
        }
        for (String word : ACTION_WORDS) {
            if (containsToken(normalized, word) || containsToken(normalizedId, word)) {
                score += 32;
                break;
            }
        }

        if (path.contains("sell") || path.contains("confirm") || path.contains("accept") || path.contains("submit")) {
            score += 30;
        }
        if (path.contains("lime")) {
            score += 15;
        } else if (path.contains("green")) {
            score += 12;
        } else if (path.contains("emerald")) {
            score += 8;
        }
        if (slot.itemCount() == 1) {
            score += 5;
        }

        score += positionalScore(slot.index(), topSlots);
        return score;
    }

    private static int positionalScore(int index, int topSlots) {
        int score = 0;
        if (topSlots > 0 && index >= Math.max(0, topSlots - 9)) {
            score += 15;
        }
        if (index % 9 >= 6) {
            score += 10;
        }
        return score;
    }

    private static boolean containsToken(String text, String token) {
        if (text == null || token == null || token.isBlank()) {
            return false;
        }

        // For scripts where word-boundary detection is unreliable (for example
        // CJK), a literal substring is intentional. For alphabetic/number-based
        // tokens, require Unicode-aware boundaries so short words such as "sat"
        // cannot accidentally match unrelated text such as "status".
        if (usesUnsegmentedScript(token)) {
            return text.contains(token);
        }

        String pattern = "(?iu)(?:^|[^\\p{L}\\p{N}])"
                + Pattern.quote(token)
                + "(?:$|[^\\p{L}\\p{N}])";
        return Pattern.compile(pattern).matcher(text).find();
    }

    private static boolean usesUnsegmentedScript(String token) {
        for (int offset = 0; offset < token.length();) {
            int codePoint = token.codePointAt(offset);
            offset += Character.charCount(codePoint);
            Character.UnicodeScript script = Character.UnicodeScript.of(codePoint);
            if (script == Character.UnicodeScript.HAN
                    || script == Character.UnicodeScript.HIRAGANA
                    || script == Character.UnicodeScript.KATAKANA
                    || script == Character.UnicodeScript.HANGUL
                    || script == Character.UnicodeScript.THAI) {
                return true;
            }
        }
        return false;
    }

    private static String normalize(String input) {
        if (input == null) {
            return "";
        }
        String decomposed = Normalizer.normalize(input, Normalizer.Form.NFD);
        return decomposed.replaceAll("\\p{M}+", "").toLowerCase(Locale.ROOT).replace('_', ' ');
    }

    private record ScoredSlot(MenuSlot slot, int score) {
    }
}
