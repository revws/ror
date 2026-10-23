package com.revror.kernelmanager.utils;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.revror.kernelmanager.R;

public class BannerTextStyleRegistry {

    public static class BannerTextStyle {
        public final String name;
        public final String titleSmall;
        public final String titleLarge;
        public final String subtitle;
        public final String pillText;
        public final String fontFamily;
        public final int weight; // 100 to 900
        public final boolean bold;
        public final boolean italic;
        public final int titleSmallColor;
        public final int titleLargeColor;
        public final int subtitleColor;
        public final int pillTextColor;
        public final int pillBgColor;
        public final int pillStrokeColor;
        public final int shadowColor;
        public final float shadowRadius;
        public final float letterSpacing;

        public BannerTextStyle(String name, String fontFamily, int weight, boolean bold, boolean italic,
                               int titleSmallColor, int titleLargeColor, int subtitleColor,
                               int pillTextColor, int pillBgColor, int pillStrokeColor,
                               int shadowColor, float shadowRadius, float letterSpacing) {
            this.name = name;
            this.titleSmall = "REINCARNATION OF";
            this.titleLarge = "REVOLUTIONARY";
            this.subtitle = "Performance • Battery • Gaming • Control";
            this.pillText = "revWhiteShadow";
            this.fontFamily = fontFamily;
            this.weight = weight;
            this.bold = bold;
            this.italic = italic;
            this.titleSmallColor = titleSmallColor;
            this.titleLargeColor = titleLargeColor;
            this.subtitleColor = subtitleColor;
            this.pillTextColor = pillTextColor;
            this.pillBgColor = pillBgColor;
            this.pillStrokeColor = pillStrokeColor;
            this.shadowColor = shadowColor;
            this.shadowRadius = shadowRadius;
            this.letterSpacing = letterSpacing;
        }

        public BannerTextStyle(String name, String fontFamily, boolean bold, boolean italic,
                               int titleSmallColor, int titleLargeColor, int subtitleColor,
                               int pillTextColor, int pillBgColor, int pillStrokeColor,
                               int shadowColor, float shadowRadius, float letterSpacing) {
            this(name, fontFamily, bold ? 800 : 400, bold, italic,
                 titleSmallColor, titleLargeColor, subtitleColor,
                 pillTextColor, pillBgColor, pillStrokeColor,
                 shadowColor, shadowRadius, letterSpacing);
        }
    }

    public static final String REVROR_SIGNATURE = "revror";

    public static Typeface createStyleTypeface(BannerTextStyle s) {
        if (s == null) return Typeface.DEFAULT_BOLD;
        if (Build.VERSION.SDK_INT >= 28) {
            try {
                Typeface base = null;
                if (s.fontFamily != null && !s.fontFamily.isEmpty()) {
                    base = Typeface.create(s.fontFamily, Typeface.NORMAL);
                }
                if (base == null) {
                    base = Typeface.DEFAULT;
                }
                int targetWeight = s.weight > 0 ? s.weight : (s.bold ? 700 : 400);
                return Typeface.create(base, targetWeight, s.italic);
            } catch (Exception ignored) {}
        }
        int styleFlag = s.bold && s.italic ? Typeface.BOLD_ITALIC
                : s.bold ? Typeface.BOLD : s.italic ? Typeface.ITALIC : Typeface.NORMAL;
        if (s.fontFamily != null && !s.fontFamily.isEmpty()) {
            try {
                return Typeface.create(s.fontFamily, styleFlag);
            } catch (Exception ignored) {}
        }
        return Typeface.defaultFromStyle(styleFlag);
    }

    public static final BannerTextStyle[] STYLES = new BannerTextStyle[]{
        // 0. Classic Revolutionary (Default Crimson Red - Heavy Sans-Serif Black Italic 900)
        new BannerTextStyle(
            "Classic Revolutionary",
            "sans-serif-black", 900, true, true,
            0xFFE0E2EC, 0xFFFA476B, 0xFFC0C4D8, 0xFFF3DEEE, 0x33100B1A, 0x66FA476B, 0x88FA476B, 14f, 0.04f
        ),
        // 1. Cyber Neon Flux (Electric Cyan - Monospace Heavy 800)
        new BannerTextStyle(
            "Cyber Neon Flux",
            "monospace", 800, true, false,
            0xFF80DEEA, 0xFF00FFE5, 0xFF80CBC4, 0xFF00FFE5, 0x33003333, 0x8800FFE5, 0xCC00FFE5, 18f, 0.08f
        ),
        // 2. Infernal Rage (Hellfire Crimson - Sans 900 Italic)
        new BannerTextStyle(
            "Infernal Rage",
            "sans-serif", 900, true, true,
            0xFFFF8A80, 0xFFFF2A14, 0xFFFFAB91, 0xFFFF5252, 0x443E0D0D, 0xAAFF2A14, 0xDDFF1744, 20f, 0.06f
        ),
        // 3. Tokyo Synthwave 84 (Hot Magenta Pink - Casual Rounded Bold Italic 700)
        new BannerTextStyle(
            "Tokyo Synthwave 84",
            "casual", 700, true, true,
            0xFFF48FB1, 0xFFFF007F, 0xFFCE93D8, 0xFFFF4081, 0x333A0D28, 0xAAFF007F, 0xDDFF007F, 18f, 0.07f
        ),
        // 4. Digital Code Stream (Terminal Green - Monospace Regular 400)
        new BannerTextStyle(
            "Digital Code Stream",
            "monospace", 400, false, false,
            0xFFA5D6A7, 0xFF00FF66, 0xFF81C784, 0xFF00FF66, 0x330A2914, 0x8800FF66, 0xCC00FF66, 16f, 0.09f
        ),
        // 5. Violet Mecha Berserk (Purple & Green - Condensed Heavy 900 Italic)
        new BannerTextStyle(
            "Violet Mecha Berserk",
            "sans-serif-condensed", 900, true, true,
            0xFFD1C4E9, 0xFFB300FF, 0xFFB39DDB, 0xFF00FF88, 0x33260A38, 0x88B300FF, 0xCCB300FF, 18f, 0.05f
        ),
        // 6. Imperial Dynasty 24K (Imperial Gold - Bold Serif 800)
        new BannerTextStyle(
            "Imperial Dynasty 24K",
            "serif", 800, true, false,
            0xFFFFF59D, 0xFFFFD700, 0xFFFFE082, 0xFFFFD700, 0x33332800, 0xAAFFD700, 0xDDFFD700, 16f, 0.06f
        ),
        // 7. Arctic Absolute Zero (Glacier Frost Cyan - Ultra Thin 100)
        new BannerTextStyle(
            "Arctic Absolute Zero",
            "sans-serif-thin", 100, false, false,
            0xFFB2EBF2, 0xFF00E5FF, 0xFF80DEEA, 0xFFE0F7FA, 0x33002633, 0x8800E5FF, 0xCC00E5FF, 16f, 0.08f
        ),
        // 8. Blood Moon Shinobi (Blood Crimson - Cursive Calligraphy 600)
        new BannerTextStyle(
            "Blood Moon Shinobi",
            "cursive", 600, true, true,
            0xFFFFCDD2, 0xFFFF0D55, 0xFFEF9A9A, 0xFFFF1744, 0x3333000D, 0xAAFF0D55, 0xDDFF0D55, 18f, 0.07f
        ),
        // 9. Solar Flare Helios (Solar Plasma Orange - Heavy Sans 900 Upright)
        new BannerTextStyle(
            "Solar Flare Helios",
            "sans-serif-black", 900, true, false,
            0xFFFFCC80, 0xFFFF7700, 0xFFFFB74D, 0xFFFF9800, 0x33381800, 0xAAFF7700, 0xDDFF7700, 18f, 0.06f
        ),
        // 10. Arcane Mystic Runes (Mystic Violet - Serif Slanted 600)
        new BannerTextStyle(
            "Arcane Mystic Runes",
            "serif", 600, true, true,
            0xFFE1BEE7, 0xFF9D00FF, 0xFFCE93D8, 0xFFE040FB, 0x33220038, 0x889D00FF, 0xCC9D00FF, 16f, 0.06f
        ),
        // 11. REVROR Chroma Viper (Toxic Esports Green - Condensed Bold 700)
        new BannerTextStyle(
            "REVROR Chroma Viper",
            "sans-serif-condensed", 700, true, false,
            0xFFA7FFEB, 0xFF00FF44, 0xFF69F0AE, 0xFF00E676, 0x33003310, 0x8800FF44, 0xCC00FF44, 16f, 0.08f
        ),
        // 12. Arena Master (Arena Red - Medium Italic 600)
        new BannerTextStyle(
            "Arena Master",
            "sans-serif-medium", 600, true, true,
            0xFFFF80AB, 0xFFFF3366, 0xFFFF4081, 0xFFFF1744, 0x33380A18, 0xAAFF3366, 0xDDFF3366, 18f, 0.07f
        ),
        // 13. Solar Ascendant Aura (Divine Gold - Heavy Sans 800 Italic)
        new BannerTextStyle(
            "Solar Ascendant Aura",
            "sans-serif-black", 800, true, true,
            0xFFFFF9C4, 0xFFFFEE00, 0xFFFFF176, 0xFFFFD600, 0x33383000, 0xAAFFEE00, 0xDDFFEE00, 20f, 0.07f
        ),
        // 14. Ember Blade Flame (Flame Samurai - Serif Monospace 700 Italic)
        new BannerTextStyle(
            "Ember Blade Flame",
            "serif-monospace", 700, true, true,
            0xFFFFAB91, 0xFFFF4500, 0xFFFF8A65, 0xFFFF5722, 0x33381005, 0xAAFF4500, 0xDDFF4500, 18f, 0.06f
        ),
        // 15. Infinite Azure Void (Celestial Azure - Sleek Light 300 Italic)
        new BannerTextStyle(
            "Infinite Azure Void",
            "sans-serif-light", 300, false, true,
            0xFF80D8FF, 0xFF00B0FF, 0xFF40C4FF, 0xFF00E5FF, 0x33002238, 0x8800B0FF, 0xCC00B0FF, 20f, 0.08f
        ),
        // 16. Ghost Protocol Cyber (Cyber Teal - Hacker Monospace 600)
        new BannerTextStyle(
            "Ghost Protocol Cyber",
            "monospace", 600, true, false,
            0xFFB2DFDB, 0xFF00FFA2, 0xFF80CBC4, 0xFF1DE9B6, 0x33003020, 0x8800FFA2, 0xCC00FFA2, 16f, 0.08f
        ),
        // 17. Vaporwave 1999 (Pastel Magenta Pink - Casual Script 400 Italic)
        new BannerTextStyle(
            "Vaporwave 1999",
            "casual", 400, false, true,
            0xFFF8BBD0, 0xFFFF71CE, 0xFFE1BEE7, 0xFF01CDFE, 0x33330D28, 0xAAFF71CE, 0xDDFF71CE, 16f, 0.07f
        ),
        // 18. Dark Matter Singularity (Deep Cosmic Violet - Condensed Light 300)
        new BannerTextStyle(
            "Dark Matter Singularity",
            "sans-serif-condensed-light", 300, false, false,
            0xFFD1C4E9, 0xFF7928CA, 0xFFB39DDB, 0xFF9D4EDD, 0x33220038, 0x887928CA, 0xCC7928CA, 18f, 0.07f
        ),
        // 19. Lightning Tempest (Mint Thunderbolt - Medium 700 Italic)
        new BannerTextStyle(
            "Lightning Tempest",
            "sans-serif-medium", 700, true, true,
            0xFFB9F6CA, 0xFF38EF7D, 0xFF69F0AE, 0xFF00E676, 0x330A3018, 0x8838EF7D, 0xCC38EF7D, 16f, 0.07f
        ),
        // 20. Titan Mecha Wing (Celestial Blue - Armor Condensed 800)
        new BannerTextStyle(
            "Titan Mecha Wing",
            "sans-serif-condensed", 800, true, false,
            0xFFBBDEFB, 0xFF0099FF, 0xFF90CAF9, 0xFF2979FF, 0x33001F3D, 0x880099FF, 0xCC0099FF, 18f, 0.07f
        ),
        // 21. Crimson Phantom Ghost (Spec Ops Red - Sans 800 Italic)
        new BannerTextStyle(
            "Crimson Phantom Ghost",
            "sans-serif", 800, true, true,
            0xFFFF8A80, 0xFFFF003C, 0xFFFF5252, 0xFFFF1744, 0x3338000A, 0xAAFF003C, 0xDDFF003C, 18f, 0.07f
        ),
        // 22. Titanium Cyber Mech (Pure Titanium Silver - Clean Light 300)
        new BannerTextStyle(
            "Titanium Cyber Mech",
            "sans-serif-light", 300, false, false,
            0xFFECEFF1, 0xFFF8FAFC, 0xFFCFD8DC, 0xFFFFFFFF, 0x33202530, 0x88E2E8F0, 0xCCE2E8F0, 14f, 0.09f
        ),
        // 23. Retro 80s Arcade (Arcade Neon Magenta - Monospace 700 Italic)
        new BannerTextStyle(
            "Retro 80s Arcade",
            "monospace", 700, true, true,
            0xFFFF80AB, 0xFFFF0055, 0xFFFF4081, 0xFFFF1744, 0x33380018, 0xAAFF0055, 0xDDFF0055, 18f, 0.08f
        ),
        // 24. Quantum Hyperdrive (Ultra Violet Laser - Elegant Serif 500 Italic)
        new BannerTextStyle(
            "Quantum Hyperdrive",
            "serif", 500, false, true,
            0xFFE1BEE7, 0xFF8A2BE2, 0xFFD1C4E9, 0xFFBA68C8, 0x33200538, 0x888A2BE2, 0xCC8A2BE2, 18f, 0.08f
        ),
        // 25. Nordic Valkyrie (Ice Azure - Heavy Serif 900)
        new BannerTextStyle(
            "Nordic Valkyrie",
            "serif", 900, true, false,
            0xFFE0F7FA, 0xFF29B6F6, 0xFFB2EBF2, 0xFF00E5FF, 0x33002238, 0x8829B6F6, 0xCC29B6F6, 17f, 0.06f
        ),
        // 26. Neon Sunset Horizon (Amber Blaze - Casual 700 Upright)
        new BannerTextStyle(
            "Neon Sunset Horizon",
            "casual", 700, true, false,
            0xFFFFE0B2, 0xFFFF9100, 0xFFFFCC80, 0xFFFFAB40, 0x33381B00, 0xAAFF9100, 0xDDFF9100, 18f, 0.06f
        ),
        // 27. Overclocked Redline (Crimson Bloodline - Condensed Heavy 900 Italic)
        new BannerTextStyle(
            "Overclocked Redline",
            "sans-serif-condensed", 900, true, true,
            0xFFFFCDD2, 0xFFDC143C, 0xFFEF9A9A, 0xFFFF1744, 0x3338000F, 0xAADC143C, 0xDDDC143C, 19f, 0.05f
        ),
        // 28. Deep Abyssal Kraken (Abyssal Teal - Serif Monospace 400)
        new BannerTextStyle(
            "Deep Abyssal Kraken",
            "serif-monospace", 400, false, false,
            0xFFB2DFDB, 0xFF00CED1, 0xFF80CBC4, 0xFF4DD0E1, 0x33002B2B, 0x8800CED1, 0xCC00CED1, 16f, 0.07f
        ),
        // 29. Stealth Jet Blackbird (Slate Gunmetal - Ultra Thin 200)
        new BannerTextStyle(
            "Stealth Jet Blackbird",
            "sans-serif-thin", 200, false, false,
            0xFFCFD8DC, 0xFF94A3B8, 0xFFB0BEC5, 0xFFE2E8F0, 0x331E293B, 0x8894A3B8, 0xCC94A3B8, 14f, 0.09f
        ),
        // 30. Cyber Ronin Blade (Katana Hot Pink - Heavy Sans 900 Italic)
        new BannerTextStyle(
            "Cyber Ronin Blade",
            "sans-serif-black", 900, true, true,
            0xFFF8BBD0, 0xFFFF1493, 0xFFF48FB1, 0xFFFF4081, 0x3338051E, 0xAAFF1493, 0xDDFF1493, 19f, 0.06f
        ),
        // 31. Cosmic Nebula Cloud (Cosmic Lilac - Light 200 Italic)
        new BannerTextStyle(
            "Cosmic Nebula Cloud",
            "sans-serif-light", 200, false, true,
            0xFFEDE7F6, 0xFFCE93D8, 0xFFD1C4E9, 0xFFE1BEE7, 0x33281033, 0x88CE93D8, 0xCCCE93D8, 16f, 0.08f
        ),
        // 32. Radioactive Biohazard (Nuclear Lime - Heavy Monospace 900)
        new BannerTextStyle(
            "Radioactive Biohazard",
            "monospace", 900, true, false,
            0xFFDCEDC8, 0xFF76FF03, 0xFFC5E1A5, 0xFF64DD17, 0x331B3800, 0x8876FF03, 0xCC76FF03, 18f, 0.08f
        ),
        // 33. Gilded Sovereign Throne (Gilded Amber - Antique Serif 600 Italic)
        new BannerTextStyle(
            "Gilded Sovereign Throne",
            "serif", 600, true, true,
            0xFFFFF8E1, 0xFFE5C07B, 0xFFFFECB3, 0xFFFFD54F, 0x3333240A, 0xAAE5C07B, 0xDDE5C07B, 17f, 0.06f
        ),
        // 34. Hyperion Solar Laser (Laser Lemon - Medium 500)
        new BannerTextStyle(
            "Hyperion Solar Laser",
            "sans-serif-medium", 500, false, false,
            0xFFFFFFDE, 0xFFFFFF00, 0xFFFFF59D, 0xFFFFEA00, 0x33383800, 0xAAFFFF00, 0xDDFFFF00, 18f, 0.06f
        ),
        // 35. Studio Pure Minimalist (Clean Pure White - Regular Sans 400)
        new BannerTextStyle(
            "Studio Pure Minimalist",
            "sans-serif", 400, false, false,
            0xFFE2E8F0, 0xFFFFFFFF, 0xFFCBD5E1, 0xFFFFFFFF, 0x331E293B, 0x66FFFFFF, 0x88FFFFFF, 12f, 0.08f
        ),
        // 36. Cyber Gothic Monolith (Gothic Blood - Heavy Serif 900 Italic)
        new BannerTextStyle(
            "Cyber Gothic Monolith",
            "serif", 900, true, true,
            0xFFFFCDD2, 0xFFFF1744, 0xFFEF9A9A, 0xFFFF5252, 0x333B0505, 0xAAFF1744, 0xDDFF1744, 18f, 0.06f
        ),
        // 37. Hacker Terminal Bypass (Bypass Stream - Light Monospace 300)
        new BannerTextStyle(
            "Hacker Terminal Bypass",
            "monospace", 300, false, false,
            0xFFC8E6C9, 0xFF00E676, 0xFFA5D6A7, 0xFF69F0AE, 0x33002611, 0x8800E676, 0xCC00E676, 15f, 0.09f
        ),
        // 38. Zenith Starship HUD (Starship HUD - Sleek Condensed Light 200 Italic)
        new BannerTextStyle(
            "Zenith Starship HUD",
            "sans-serif-condensed-light", 200, false, true,
            0xFFB2EBF2, 0xFF00B0FF, 0xFF80DEEA, 0xFF40C4FF, 0x33002538, 0x8800B0FF, 0xCC00B0FF, 17f, 0.08f
        ),
        // 39. Galactic Overlord (Galactic Void - Heavy Sans 900)
        new BannerTextStyle(
            "Galactic Overlord",
            "sans-serif-black", 900, true, false,
            0xFFE1BEE7, 0xFF8E24AA, 0xFFCE93D8, 0xFFAB47BC, 0x332B003B, 0xAA8E24AA, 0xDD8E24AA, 19f, 0.07f
        ),
        // 40. Sakura Blossom Drift (Anime Spring Blossom - Cursive Script 400 Italic)
        new BannerTextStyle(
            "Sakura Blossom Drift",
            "cursive", 400, false, true,
            0xFFFCE4EC, 0xFFFF4081, 0xFFF8BBD0, 0xFFFF80AB, 0x333B0A1E, 0xAAFF4081, 0xDDFF4081, 16f, 0.07f
        ),
        // 41. Sub-Zero Cryo Chamber (Sub-Zero Cryo - Slanted Condensed 600 Italic)
        new BannerTextStyle(
            "Sub-Zero Cryo Chamber",
            "sans-serif-condensed", 600, true, true,
            0xFFE0F7FA, 0xFF00E5FF, 0xFFB2EBF2, 0xFF18FFFF, 0x33002B3B, 0x8800E5FF, 0xCC00E5FF, 18f, 0.06f
        ),
        // 42. Valhalla Royal Crown (Nordic Bronze Gold - Royal Bold Serif 700)
        new BannerTextStyle(
            "Valhalla Royal Crown",
            "serif", 700, true, false,
            0xFFFFF8E1, 0xFFFFC107, 0xFFFFECB3, 0xFFFFD54F, 0x333B2B00, 0xAAFFC107, 0xDDFFC107, 17f, 0.06f
        ),
        // 43. Glitch Matrix Overwrite (Overclocked Terminal - Heavy Monospace 800 Italic)
        new BannerTextStyle(
            "Glitch Matrix Overwrite",
            "monospace", 800, true, true,
            0xFFDCEDC8, 0xFF00FF66, 0xFFC5E1A5, 0xFF76FF03, 0x330F3000, 0x8800FF66, 0xCC00FF66, 18f, 0.09f
        ),
        // 44. Anime Cyber Girl Aika (Electric Cyan & Magenta - Sans Black 900 Italic)
        new BannerTextStyle(
            "Anime Cyber Girl Aika",
            "sans-serif-black", 900, true, true,
            0xFF80DEEA, 0xFF00E5FF, 0xFFF48FB1, 0xFFFF007F, 0x33003040, 0xAA00E5FF, 0xDD00E5FF, 20f, 0.07f
        ),
        // 45. Void Sovereign (Dark Shadow Violet - Heavy Serif 900 Italic)
        new BannerTextStyle(
            "Void Sovereign",
            "serif", 900, true, true,
            0xFFD1C4E9, 0xFF7928CA, 0xFFB39DDB, 0xFF00E5FF, 0x33250040, 0xAA7928CA, 0xDD7928CA, 22f, 0.06f
        ),
        // 46. Solar Blade Flame (Solar Flame Blaze - Sans Heavy 800 Italic)
        new BannerTextStyle(
            "Solar Blade Flame",
            "sans-serif-black", 800, true, true,
            0xFFFFCC80, 0xFFFF3D00, 0xFFFFAB40, 0xFFFFD600, 0x33401000, 0xAAFF3D00, 0xDDFF3D00, 20f, 0.06f
        ),
        // 47. Celestial Limitless Void (Celestial Azure - Sleek 300 Italic)
        new BannerTextStyle(
            "Celestial Limitless Void",
            "sans-serif-light", 300, false, true,
            0xFFB3E5FC, 0xFF00B0FF, 0xFF81D4FA, 0xFF00E5FF, 0x33002845, 0xAA00B0FF, 0xDD00B0FF, 22f, 0.08f
        ),
        // 48. Chibi Neko Gamer (Kawaii Catgirl Pastel Pink & Mint - Casual 700 Italic)
        new BannerTextStyle(
            "Chibi Neko Gamer",
            "casual", 700, true, true,
            0xFFF8BBD0, 0xFFFF4081, 0xFFB9F6CA, 0xFF00E676, 0x333D0D25, 0xAAFF4081, 0xDDFF4081, 18f, 0.07f
        ),
        // 49. Supernova Neon Rebel (Neon Punk Gold & Violet - Monospace 800)
        new BannerTextStyle(
            "Supernova Neon Rebel",
            "monospace", 800, true, false,
            0xFFFFF59D, 0xFFFFD600, 0xFFCE93D8, 0xFFD500F9, 0x333B3200, 0xAAFFD600, 0xDDFFD600, 19f, 0.08f
        ),
        // 50. REVROR Small Caps Command
        new BannerTextStyle(
            "REVROR Small Caps Command",
            "sans-serif-smallcaps", 700, true, false,
            0xFFE0F7FA, 0xFF18FFFF, 0xFF80DEEA, 0xFF00E5FF, 0x33002B33, 0x8818FFFF, 0xCC18FFFF, 16f, 0.12f
        ),
        // 51. Rounded Arcade Hero
        new BannerTextStyle(
            "Rounded Arcade Hero",
            "sans-serif-rounded", 800, true, false,
            0xFFF3E5F5, 0xFFFF5CFF, 0xFFE1BEE7, 0xFFEA80FC, 0x3333003D, 0x88FF5CFF, 0xCCFF5CFF, 17f, 0.06f
        ),
        // 52. Tactical Serif Mono
        new BannerTextStyle(
            "Tactical Serif Mono",
            "serif-monospace", 600, true, false,
            0xFFE8F5E9, 0xFF69F0AE, 0xFFC8E6C9, 0xFF00E676, 0x33002B19, 0x8869F0AE, 0xCC69F0AE, 15f, 0.1f
        ),
        // 53. Condensed Black Ops
        new BannerTextStyle(
            "Condensed Black Ops",
            "sans-serif-condensed-black", 900, true, true,
            0xFFFFEBEE, 0xFFFF1744, 0xFFFFCDD2, 0xFFFF5252, 0x333B000C, 0xAAFF1744, 0xDDFF1744, 20f, 0.04f
        ),
        // 54. Anime Bubble Pop
        new BannerTextStyle(
            "Anime Bubble Pop",
            "casual", 600, true, false,
            0xFFFFF0F7, 0xFFFF80AB, 0xFFB9F6CA, 0xFF64FFDA, 0x33330B20, 0x88FF80AB, 0xCCFF80AB, 16f, 0.05f
        ),
        // 55. Waifu Signature Script
        new BannerTextStyle(
            "Waifu Signature Script",
            "cursive", 500, false, true,
            0xFFFFE4F1, 0xFFFF4081, 0xFFD1C4E9, 0xFFB388FF, 0x33310A2B, 0x88FF4081, 0xCCFF4081, 18f, 0.04f
        ),
        // 56. Pixel Raid Terminal
        new BannerTextStyle(
            "Pixel Raid Terminal",
            "monospace", 900, true, false,
            0xFFFFFFCC, 0xFFFFFF00, 0xFFCCFF90, 0xFF76FF03, 0x33333300, 0x88FFFF00, 0xCCFFFF00, 14f, 0.14f
        ),
        // 57. Royal Anime Chronicle
        new BannerTextStyle(
            "Royal Anime Chronicle",
            "serif", 700, true, true,
            0xFFFFF8E1, 0xFFFFD54F, 0xFFFFE0B2, 0xFFFFAB40, 0x33332400, 0x88FFD54F, 0xCCFFD54F, 19f, 0.03f
        )
    };

    public static int getActiveStyleIndex(Context context) {
        if (context == null) return 0;
        int idx = context.getSharedPreferences("ror_theme", Context.MODE_PRIVATE).getInt("banner_text_style_idx", 0);
        if (idx < 0 || idx >= STYLES.length) idx = 0;
        return idx;
    }

    public static void setActiveStyleIndex(Context context, int index) {
        if (context == null) return;
        if (index < 0 || index >= STYLES.length) index = 0;
        context.getSharedPreferences("ror_theme", Context.MODE_PRIVATE).edit()
                .putInt("banner_text_style_idx", index)
                .apply();
    }

    public static BannerTextStyle getActiveStyle(Context context) {
        return STYLES[getActiveStyleIndex(context)];
    }

    public static void applyToDashboardHero(Context context, int styleIdx,
                                           TextView titleSmall, TextView titleLarge,
                                           TextView subtitle, View pillChip, TextView pillText) {
        if (!REVROR_SIGNATURE.equals("revror")) {
            throw new IllegalStateException("revror core signature integrity compromised");
        }
        if (styleIdx < 0 || styleIdx >= STYLES.length) styleIdx = 0;
        BannerTextStyle s = STYLES[styleIdx];

        Typeface mainTf = createStyleTypeface(s);

        if (titleSmall != null) {
            titleSmall.setText(s.titleSmall);
            titleSmall.setTextColor(s.titleSmallColor);
            titleSmall.setLetterSpacing(s.letterSpacing * 1.5f);
            try {
                titleSmall.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
            } catch (Exception ignored) {}
        }
        if (titleLarge != null) {
            titleLarge.setText(s.titleLarge);
            titleLarge.setTextColor(s.titleLargeColor);
            titleLarge.setTypeface(mainTf);
            titleLarge.setShadowLayer(s.shadowRadius, 0f, 0f, s.shadowColor);
            titleLarge.setLetterSpacing(s.letterSpacing);
        }
        if (subtitle != null) {
            subtitle.setText(s.subtitle);
            subtitle.setTextColor(s.subtitleColor);
        }
        if (pillText != null) {
            pillText.setText(s.pillText);
            pillText.setTextColor(s.pillTextColor);
            pillText.setCompoundDrawables(null, null, null, null);
            pillText.setCompoundDrawablePadding(0);
        }
        if (pillChip != null) {
            GradientDrawable pillBg = new GradientDrawable();
            pillBg.setColor(s.pillBgColor);
            pillBg.setCornerRadius(28f);
            pillBg.setStroke(2, s.pillStrokeColor);
            pillChip.setBackground(pillBg);
        }
    }

    public static View buildStylePreviewCard(Context context, int styleIdx, boolean isSelected, View.OnClickListener onClick) {
        if (styleIdx < 0 || styleIdx >= STYLES.length) styleIdx = 0;
        BannerTextStyle s = STYLES[styleIdx];
        float density = context.getResources().getDisplayMetrics().density;
        int p8 = (int) (8 * density);
        int p10 = (int) (10 * density);

        Typeface mainTf = createStyleTypeface(s);

        LinearLayout card = new LinearLayout(context);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(p10, p8, p10, p8);

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(isSelected ? (s.titleLargeColor & 0x24FFFFFF) : 0xFF141722);
        bg.setCornerRadius(12 * density);
        bg.setStroke(isSelected ? (int) (2 * density) : (int) (1 * density),
                isSelected ? s.titleLargeColor : 0xFF31364A);
        card.setBackground(bg);

        LinearLayout topRow = new LinearLayout(context);
        topRow.setOrientation(LinearLayout.HORIZONTAL);
        topRow.setGravity(Gravity.CENTER_VERTICAL);

        TextView tag = new TextView(context);
        tag.setText(s.name.toUpperCase());
        tag.setTextSize(9f);
        tag.setTextColor(isSelected ? s.titleLargeColor : 0xFF8A92AB);
        tag.setTypeface(null, Typeface.BOLD);
        tag.setLetterSpacing(0.06f);
        topRow.addView(tag, new LinearLayout.LayoutParams(0, -2, 1f));

        if (isSelected) {
            TextView activeBadge = new TextView(context);
            activeBadge.setText("ACTIVE");
            activeBadge.setTextSize(8f);
            activeBadge.setTextColor(s.titleLargeColor);
            activeBadge.setTypeface(null, Typeface.BOLD);
            activeBadge.setPadding((int) (6 * density), (int) (1 * density), (int) (6 * density), (int) (1 * density));
            GradientDrawable bBg = new GradientDrawable();
            bBg.setColor(s.titleLargeColor & 0x33FFFFFF);
            bBg.setCornerRadius(8 * density);
            bBg.setStroke(1, s.titleLargeColor);
            activeBadge.setBackground(bBg);
            topRow.addView(activeBadge);
        }
        card.addView(topRow);

        TextView small = new TextView(context);
        small.setText(s.titleSmall);
        small.setTextSize(8.5f);
        small.setTextColor(s.titleSmallColor);
        small.setLetterSpacing(s.letterSpacing * 1.5f);
        card.addView(small);

        TextView large = new TextView(context);
        large.setText(s.titleLarge);
        large.setTextSize(17f);
        large.setTextColor(s.titleLargeColor);
        large.setTypeface(mainTf);
        large.setShadowLayer(s.shadowRadius * 0.7f, 0, 0, s.shadowColor);
        large.setLetterSpacing(s.letterSpacing);
        card.addView(large);

        TextView sub = new TextView(context);
        sub.setText(s.subtitle);
        sub.setTextSize(8f);
        sub.setTextColor(s.subtitleColor);
        sub.setSingleLine(true);
        card.addView(sub);

        LinearLayout pillRow = new LinearLayout(context);
        pillRow.setOrientation(LinearLayout.HORIZONTAL);
        pillRow.setGravity(Gravity.CENTER_VERTICAL);
        pillRow.setPadding((int) (8 * density), (int) (3 * density), (int) (10 * density), (int) (3 * density));
        GradientDrawable pBg = new GradientDrawable();
        pBg.setColor(s.pillBgColor);
        pBg.setCornerRadius(14 * density);
        pBg.setStroke(1, s.pillStrokeColor);
        pillRow.setBackground(pBg);

        // Concentric glowing neon pulse dot inside preview card
        FrameLayout dotContainer = new FrameLayout(context);
        int dSize = (int) (12 * density);
        FrameLayout.LayoutParams dcLp = new FrameLayout.LayoutParams(dSize, dSize);
        dcLp.gravity = Gravity.CENTER_VERTICAL;
        dcLp.setMarginEnd((int) (6 * density));
        dotContainer.setLayoutParams(dcLp);

        View glow = new View(context);
        GradientDrawable gD = new GradientDrawable();
        gD.setShape(GradientDrawable.OVAL);
        gD.setColor(0x4400FF66);
        glow.setBackground(gD);
        dotContainer.addView(glow, new FrameLayout.LayoutParams(dSize, dSize));

        View core = new View(context);
        GradientDrawable cD = new GradientDrawable();
        cD.setShape(GradientDrawable.OVAL);
        cD.setColor(0xFF00FF66);
        core.setBackground(cD);
        int cSize = (int) (6 * density);
        FrameLayout.LayoutParams cLp = new FrameLayout.LayoutParams(cSize, cSize);
        cLp.gravity = Gravity.CENTER;
        dotContainer.addView(core, cLp);

        pillRow.addView(dotContainer);

        TextView pill = new TextView(context);
        pill.setText(s.pillText);
        pill.setTextSize(8.5f);
        pill.setTextColor(s.pillTextColor);
        pill.setTypeface(null, Typeface.BOLD);
        pillRow.addView(pill);

        // Clicking pill row in preview card also triggers Ko-Fi redirect
        pillRow.setOnClickListener(v -> {
            try {
                v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://ko-fi.com/revWS"));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
            } catch (Exception ignored) {}
        });

        LinearLayout.LayoutParams pillLp = new LinearLayout.LayoutParams(-2, -2);
        pillLp.topMargin = (int) (4 * density);
        card.addView(pillRow, pillLp);

        if (onClick != null) {
            card.setOnClickListener(onClick);
        }
        return card;
    }

    public static void showBannerTextStyleDialog(Activity activity, Runnable onApplied) {
        if (activity == null || activity.isFinishing() || activity.isDestroyed()) return;
        Dialog dialog = new Dialog(activity, android.R.style.Theme_Black_NoTitleBar_Fullscreen);

        float density = activity.getResources().getDisplayMetrics().density;
        int dp14 = (int) (14 * density);
        int dp16 = (int) (16 * density);

        LinearLayout root = new LinearLayout(activity);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp16, dp14, dp16, dp14);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xF412141D);
        bg.setCornerRadius(18 * density);
        bg.setStroke(1, 0xFF35394A);
        root.setBackground(bg);

        // Header
        LinearLayout header = new LinearLayout(activity);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(0, 0, 0, (int) (12 * density));

        LinearLayout titleCol = new LinearLayout(activity);
        titleCol.setOrientation(LinearLayout.VERTICAL);

        TextView title = new TextView(activity);
        title.setText("BANNER TEXT STYLES (" + STYLES.length + " STYLES)");
        title.setTextSize(14.5f);
        title.setTextColor(Color.WHITE);
        title.setTypeface(null, Typeface.BOLD);
        title.setLetterSpacing(0.06f);
        titleCol.addView(title);

        TextView subtitle = new TextView(activity);
        subtitle.setText("Preserves REVOLUTIONARY branding • Distinct custom fonts & glowing FX");
        subtitle.setTextSize(10f);
        subtitle.setTextColor(0xFF8E95B2);
        titleCol.addView(subtitle);

        header.addView(titleCol, new LinearLayout.LayoutParams(0, -2, 1f));

        ImageView closeBtn = new ImageView(activity);
        int closeSize = (int) (24 * density);
        LinearLayout.LayoutParams closeLp = new LinearLayout.LayoutParams(closeSize, closeSize);
        closeBtn.setLayoutParams(closeLp);
        closeBtn.setImageResource(R.drawable.ic_cancel);
        closeBtn.setColorFilter(0xFFB0B5C6);
        closeBtn.setPadding((int) (3 * density), (int) (3 * density), (int) (3 * density), (int) (3 * density));
        closeBtn.setOnClickListener(v -> {
            try {
                if (dialog.isShowing()) dialog.dismiss();
            } catch (Exception ignored) {}
        });
        header.addView(closeBtn);
        root.addView(header);

        // Action Bar beside/under text styles: [ Animation Styles ] [ Custom Gallery ]
        LinearLayout actionBar = new LinearLayout(activity);
        actionBar.setOrientation(LinearLayout.HORIZONTAL);
        actionBar.setPadding(0, 0, 0, (int) (10 * density));

        TextView btnAnimStyles = new TextView(activity);
        btnAnimStyles.setText("ANIMATION STYLES");
        btnAnimStyles.setTextSize(11f);
        btnAnimStyles.setTextColor(Color.WHITE);
        btnAnimStyles.setTypeface(null, Typeface.BOLD);
        btnAnimStyles.setGravity(Gravity.CENTER);
        btnAnimStyles.setPadding((int) (14 * density), (int) (8 * density), (int) (14 * density), (int) (8 * density));
        GradientDrawable animBg = new GradientDrawable();
        animBg.setColor(0xDD2A1C3E);
        animBg.setCornerRadius(10 * density);
        animBg.setStroke(1, 0xFFFFB300);
        btnAnimStyles.setBackground(animBg);
        btnAnimStyles.setOnClickListener(v -> {
            try { if (dialog.isShowing()) dialog.dismiss(); } catch (Exception ignored) {}
            if (activity instanceof com.revror.kernelmanager.activities.DashboardActivity) {
                ((com.revror.kernelmanager.activities.DashboardActivity) activity).showBannerAnimationStyleDialog();
            } else if (activity instanceof com.revror.kernelmanager.activities.GameLauncherActivity) {
                ((com.revror.kernelmanager.activities.GameLauncherActivity) activity).showThemeAndBannerDialog();
            }
        });

        TextView btnCustomGallery = new TextView(activity);
        btnCustomGallery.setText("CUSTOM GALLERY");
        btnCustomGallery.setTextSize(11f);
        btnCustomGallery.setTextColor(Color.WHITE);
        btnCustomGallery.setTypeface(null, Typeface.BOLD);
        btnCustomGallery.setGravity(Gravity.CENTER);
        btnCustomGallery.setPadding((int) (14 * density), (int) (8 * density), (int) (14 * density), (int) (8 * density));
        GradientDrawable galBg = new GradientDrawable();
        galBg.setColor(0xDD3A162B);
        galBg.setCornerRadius(10 * density);
        galBg.setStroke(1, 0xFFFF007F);
        btnCustomGallery.setBackground(galBg);
        btnCustomGallery.setOnClickListener(v -> {
            try { if (dialog.isShowing()) dialog.dismiss(); } catch (Exception ignored) {}
            if (activity instanceof com.revror.kernelmanager.activities.DashboardActivity) {
                ((com.revror.kernelmanager.activities.DashboardActivity) activity).openCustomGalleryPicker();
            } else if (activity instanceof com.revror.kernelmanager.activities.GameLauncherActivity) {
                ((com.revror.kernelmanager.activities.GameLauncherActivity) activity).pickCustomBanner();
            }
        });

        LinearLayout.LayoutParams aLp = new LinearLayout.LayoutParams(0, -2, 1f);
        aLp.setMarginEnd((int) (6 * density));
        actionBar.addView(btnAnimStyles, aLp);

        LinearLayout.LayoutParams gLp = new LinearLayout.LayoutParams(0, -2, 1f);
        gLp.setMarginStart((int) (6 * density));
        actionBar.addView(btnCustomGallery, gLp);

        root.addView(actionBar);

        ScrollView scroll = new ScrollView(activity);
        scroll.setVerticalScrollBarEnabled(false);
        LinearLayout list = new LinearLayout(activity);
        list.setOrientation(LinearLayout.VERTICAL);

        int currentIdx = getActiveStyleIndex(activity);
        final int[] selected = new int[]{currentIdx};

        Runnable[] rebuild = new Runnable[1];
        rebuild[0] = () -> {
            list.removeAllViews();
            for (int i = 0; i < STYLES.length; i++) {
                final int idx = i;
                boolean isSel = (selected[0] == idx);
                View card = buildStylePreviewCard(activity, idx, isSel, v -> {
                    v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                    selected[0] = idx;
                    setActiveStyleIndex(activity, idx);
                    rebuild[0].run();
                    Toast.makeText(activity, STYLES[idx].name + " applied", Toast.LENGTH_SHORT).show();
                    if (onApplied != null) onApplied.run();
                });
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
                lp.bottomMargin = (int) (8 * density);
                list.addView(card, lp);
            }
        };
        rebuild[0].run();
        scroll.addView(list, new ScrollView.LayoutParams(-1, -2));
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1f));

        try {
            dialog.setContentView(root);
            if (!activity.isFinishing() && !activity.isDestroyed()) {
                dialog.show();
            }
        } catch (Exception ignored) {}
    }
}
