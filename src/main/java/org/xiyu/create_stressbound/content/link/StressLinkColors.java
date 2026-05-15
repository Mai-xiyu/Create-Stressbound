package org.xiyu.create_stressbound.content.link;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public final class StressLinkColors {
    public static final int UNASSIGNED = -1;
    public static final int DEFAULT = 0x4FC3F7;

    public static final int[] PALETTE = {
        0x4FC3F7, 0xFFB74D, 0x81C784, 0xE57373,
        0xBA68C8, 0xFFF176, 0x4DB6AC, 0xF06292,
        0x7986CB, 0xAED581, 0xFF8A65, 0x90CAF9,
        0xDCE775, 0xA1887F, 0xB0BEC5, 0xFFD54F,
        0x26C6DA, 0xEC407A, 0x66BB6A, 0xAB47BC,
        0xFFA726, 0x5C6BC0, 0x9CCC65, 0xEF5350
    };

    private StressLinkColors() {
    }

    public static boolean isAssigned(int color) {
        return color >= 0;
    }

    public static int normalize(int color) {
        if (!isAssigned(color)) {
            return DEFAULT;
        }
        return color & 0xFFFFFF;
    }

    public static int nextAvailable(Collection<StressLinkRecord> records) {
        Set<Integer> used = new HashSet<>();
        for (StressLinkRecord record : records) {
            if (isAssigned(record.color())) {
                used.add(normalize(record.color()));
            }
        }
        return nextAvailable(used);
    }

    public static int transmitterColor(Collection<StressLinkRecord> records) {
        for (StressLinkRecord record : records) {
            if (isAssigned(record.color())) {
                return normalize(record.color());
            }
        }
        return UNASSIGNED;
    }

    public static int nextTransmitterColor(Collection<StressLinkRecord> records) {
        Set<Integer> used = new HashSet<>();
        Set<String> seenTransmitters = new HashSet<>();
        for (StressLinkRecord record : records) {
            if (seenTransmitters.add(record.transmitter().key()) && isAssigned(record.color())) {
                used.add(normalize(record.color()));
            }
        }
        return nextAvailable(used);
    }

    public static boolean isUsedByOtherTransmitter(Collection<StressLinkRecord> records, LinkAnchor transmitter, int color) {
        int normalized = normalize(color);
        Set<String> seenTransmitters = new HashSet<>();
        for (StressLinkRecord record : records) {
            String key = record.transmitter().key();
            if (key.equals(transmitter.key()) || !seenTransmitters.add(key)) {
                continue;
            }
            if (normalize(record.color()) == normalized) {
                return true;
            }
        }
        return false;
    }

    public static int nextAvailable(Set<Integer> used) {
        for (int color : PALETTE) {
            int normalized = normalize(color);
            if (!used.contains(normalized)) {
                return normalized;
            }
        }

        for (int i = 0; i < 1024; i++) {
            int generated = hsvToRgb((float) ((i * 0.618033988749895D) % 1.0D), 0.72F, 0.95F);
            if (!used.contains(generated)) {
                return generated;
            }
        }

        for (int color = 1; color <= 0xFFFFFF; color++) {
            if (!used.contains(color)) {
                return color;
            }
        }
        return DEFAULT;
    }

    public static int nextAvailableAfter(Set<Integer> used, int currentColor) {
        int current = normalize(currentColor);
        int currentIndex = -1;
        for (int i = 0; i < PALETTE.length; i++) {
            if (normalize(PALETTE[i]) == current) {
                currentIndex = i;
                break;
            }
        }

        if (currentIndex >= 0) {
            for (int offset = 1; offset <= PALETTE.length; offset++) {
                int candidate = normalize(PALETTE[(currentIndex + offset) % PALETTE.length]);
                if (!used.contains(candidate)) {
                    return candidate;
                }
            }
        }

        return nextAvailable(used);
    }

    public static boolean isUsedByAnother(Collection<StressLinkRecord> records, StressLinkRecord target, int color) {
        int normalized = normalize(color);
        for (StressLinkRecord record : records) {
            if (!record.id().equals(target.id()) && normalize(record.color()) == normalized) {
                return true;
            }
        }
        return false;
    }

    public static String hex(int color) {
        return String.format(java.util.Locale.ROOT, "#%06X", normalize(color));
    }

    private static int hsvToRgb(float hue, float saturation, float value) {
        float h = (hue - (float) Math.floor(hue)) * 6.0F;
        int sector = (int) Math.floor(h);
        float fraction = h - sector;
        float p = value * (1.0F - saturation);
        float q = value * (1.0F - saturation * fraction);
        float t = value * (1.0F - saturation * (1.0F - fraction));

        return switch (sector) {
            case 0 -> rgb(value, t, p);
            case 1 -> rgb(q, value, p);
            case 2 -> rgb(p, value, t);
            case 3 -> rgb(p, q, value);
            case 4 -> rgb(t, p, value);
            default -> rgb(value, p, q);
        };
    }

    private static int rgb(float red, float green, float blue) {
        int r = Math.max(0, Math.min(255, Math.round(red * 255.0F)));
        int g = Math.max(0, Math.min(255, Math.round(green * 255.0F)));
        int b = Math.max(0, Math.min(255, Math.round(blue * 255.0F)));
        return (r << 16) | (g << 8) | b;
    }
}
