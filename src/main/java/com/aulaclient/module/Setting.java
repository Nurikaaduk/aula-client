package com.aulaclient.module;

import java.util.Locale;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

/** Beschreibt eine einstellbare Option eines Moduls (fuer das Einstellungs-Panel). */
public abstract class Setting {
    public final String label;

    protected Setting(String label) {
        this.label = label;
    }

    public static final class Toggle extends Setting {
        public final BooleanSupplier get;
        public final Consumer<Boolean> set;

        public Toggle(String label, BooleanSupplier get, Consumer<Boolean> set) {
            super(label);
            this.get = get;
            this.set = set;
        }
    }

    public static final class Slider extends Setting {
        public final double min, max, step;
        public final int decimals;
        public final DoubleSupplier get;
        public final DoubleConsumer set;

        public Slider(String label, double min, double max, double step, int decimals,
                      DoubleSupplier get, DoubleConsumer set) {
            super(label);
            this.min = min;
            this.max = max;
            this.step = step;
            this.decimals = decimals;
            this.get = get;
            this.set = set;
        }

        public double fraction() {
            return Math.max(0, Math.min(1, (get.getAsDouble() - min) / (max - min)));
        }

        public void setFromFraction(double f) {
            f = Math.max(0, Math.min(1, f));
            double v = min + f * (max - min);
            v = Math.round(v / step) * step;
            v = Math.max(min, Math.min(max, v));
            set.accept(v);
        }

        public String display() {
            return String.format(Locale.ROOT, "%." + decimals + "f", get.getAsDouble());
        }
    }

    public static final class Choice extends Setting {
        public final String[] options;
        public final IntSupplier get;
        public final IntConsumer set;

        public Choice(String label, String[] options, IntSupplier get, IntConsumer set) {
            super(label);
            this.options = options;
            this.get = get;
            this.set = set;
        }
    }
}
