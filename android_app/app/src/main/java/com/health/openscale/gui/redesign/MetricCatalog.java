/* Copyright (C) 2026  openScale contributors
*
*    This program is free software: you can redistribute it and/or modify
*    it under the terms of the GNU General Public License as published by
*    the Free Software Foundation, either version 3 of the License, or
*    (at your option) any later version.
*
*    This program is distributed in the hope that it will be useful,
*    but WITHOUT ANY WARRANTY; without even the implied warranty of
*    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
*    GNU General Public License for more details.
*
*    You should have received a copy of the GNU General Public License
*    along with this program.  If not, see <http://www.gnu.org/licenses/>
*/
package com.health.openscale.gui.redesign;

import android.content.Context;

import com.health.openscale.R;
import com.health.openscale.core.datatypes.ScaleMeasurement;
import com.health.openscale.core.datatypes.ScaleUser;
import com.health.openscale.core.utils.Converters;

import java.util.ArrayList;
import java.util.List;

/**
 * Catálogo das métricas exibíveis na UI nova.
 *
 * Existe para resolver um descasamento: no app antigo cada métrica é uma
 * subclasse de MeasurementView que mistura dado, formatação e desenho da
 * linha da tabela. O design novo desenha os cartões de outro jeito, mas o
 * dado e as regras de unidade continuam os mesmos.
 *
 * Então esta classe extrai só a parte de dados — chave, rótulo, unidade,
 * como ler o valor de um ScaleMeasurement — e deixa o desenho para os
 * layouts novos.
 *
 * IMPORTANTE: as chaves são exatamente as mesmas do app antigo (WeightMeasurementView
 * .KEY etc). Isso é deliberado: as preferências de "métrica habilitada" e a
 * ordem escolhida pelo usuário ficam compartilhadas entre as duas UIs, então
 * quem já usava o app não perde a configuração ao migrar.
 */
public final class MetricCatalog {

    /** De onde vem o valor — exibido como legenda no seletor de métricas. */
    public enum Source {
        SCALE(R.string.rd_settings_source_scale),
        ESTIMATED(R.string.rd_settings_source_estimated),
        CALCULATED(R.string.rd_settings_source_calculated),
        MANUAL(R.string.rd_settings_source_manual);

        public final int labelRes;

        Source(int labelRes) {
            this.labelRes = labelRes;
        }
    }

    /** Como a unidade do valor se comporta. */
    public enum UnitKind {
        /** kg / lb / st — precisa converter conforme o perfil. */
        WEIGHT,
        /** cm / in — precisa converter conforme o perfil. */
        LENGTH,
        /** Percentual, sem conversão. */
        PERCENT,
        /** kcal, sem conversão. */
        ENERGY,
        /** Adimensional (IMC, WHR, WHtR). */
        NONE
    }

    public static final class Metric {
        public final String key;
        public final int labelRes;
        public final UnitKind unitKind;
        public final Source source;
        /** Casas decimais na exibição. */
        public final int decimals;
        /** true = valor maior é melhor (músculo, água). Afeta a cor do delta. */
        public final boolean higherIsBetter;

        Metric(String key, int labelRes, UnitKind unitKind, Source source,
               int decimals, boolean higherIsBetter) {
            this.key = key;
            this.labelRes = labelRes;
            this.unitKind = unitKind;
            this.source = source;
            this.decimals = decimals;
            this.higherIsBetter = higherIsBetter;
        }

        /**
         * Lê o valor cru (em unidade base: kg, cm, %) de uma medição.
         * Devolve Float.NaN quando a métrica não se aplica ou está vazia.
         */
        public float valueOf(ScaleMeasurement m, ScaleUser user) {
            if (m == null) {
                return Float.NaN;
            }
            switch (key) {
                case KEY_WEIGHT:   return m.getWeight();
                case KEY_BMI:      return user == null ? Float.NaN : m.getBMI(user.getBodyHeight());
                case KEY_WATER:    return m.getWater();
                case KEY_MUSCLE:   return m.getMuscle();
                case KEY_LBM:      return m.getLbm();
                case KEY_FAT:      return m.getFat();
                case KEY_BONE:     return m.getBone();
                case KEY_VISCERAL: return m.getVisceralFat();
                case KEY_WAIST:    return m.getWaist();
                case KEY_HIP:      return m.getHip();
                case KEY_CHEST:    return m.getChest();
                case KEY_THIGH:    return m.getThigh();
                case KEY_BICEPS:   return m.getBiceps();
                case KEY_NECK:     return m.getNeck();
                case KEY_WHR:      return m.getWHR();
                case KEY_WHTR:     return user == null ? Float.NaN : m.getWHtR(user.getBodyHeight());
                case KEY_BMR:      return user == null ? Float.NaN : m.getBMR(user);
                case KEY_TDEE:     return user == null ? Float.NaN : m.getTDEE(user);
                case KEY_CALORIES: return m.getCalories();
                case KEY_FAT_CALIPER: return user == null ? Float.NaN : m.getFatCaliper(user);
                case KEY_CALIPER1: return m.getCaliper1();
                case KEY_CALIPER2: return m.getCaliper2();
                case KEY_CALIPER3: return m.getCaliper3();
                default:           return Float.NaN;
            }
        }

        /**
         * Converte o valor base para a unidade do perfil e devolve o sufixo.
         * A conversão usa Converters do core — as mesmas regras do app antigo.
         */
        public float toUserUnit(float baseValue, ScaleUser user) {
            if (Float.isNaN(baseValue) || user == null) {
                return baseValue;
            }
            switch (unitKind) {
                case WEIGHT:
                    return Converters.fromKilogram(baseValue, user.getScaleUnit());
                case LENGTH:
                    return Converters.fromCentimeter(baseValue, user.getMeasureUnit());
                default:
                    return baseValue;
            }
        }

        public String unitLabel(Context context, ScaleUser user) {
            switch (unitKind) {
                case WEIGHT:
                    return user == null ? "kg" : user.getScaleUnit().toString();
                case LENGTH:
                    return user == null ? "cm" : user.getMeasureUnit().toString();
                case PERCENT:
                    return "%";
                case ENERGY:
                    return context.getString(R.string.rd_unit_kcal);
                default:
                    return "";
            }
        }
    }

    // Chaves — iguais às do app antigo para compartilhar as preferências.
    public static final String KEY_WEIGHT = "weight";
    public static final String KEY_BMI = "bmi";
    public static final String KEY_WATER = "water";
    public static final String KEY_MUSCLE = "muscle";
    // "lbw" e nao "lbm": e a chave historica do app antigo
    public static final String KEY_LBM = "lbw";
    public static final String KEY_FAT = "fat";
    public static final String KEY_BONE = "bone";
    public static final String KEY_VISCERAL = "visceralFat";
    public static final String KEY_WAIST = "waist";
    public static final String KEY_WHTR = "whtr";
    public static final String KEY_HIP = "hip";
    public static final String KEY_WHR = "whr";
    public static final String KEY_CHEST = "chest";
    public static final String KEY_THIGH = "thigh";
    public static final String KEY_BICEPS = "biceps";
    public static final String KEY_NECK = "neck";
    public static final String KEY_FAT_CALIPER = "fat_caliper";
    public static final String KEY_CALIPER1 = "caliper1";
    public static final String KEY_CALIPER2 = "caliper2";
    public static final String KEY_CALIPER3 = "caliper3";
    public static final String KEY_BMR = "bmr";
    public static final String KEY_TDEE = "tdee";
    public static final String KEY_CALORIES = "calories";

    private static final List<Metric> ALL = new ArrayList<>();

    static {
        // A ordem aqui é a ordem padrão dos cartões, igual à do protótipo:
        // peso, gordura, água, músculo, IMC, cintura vêm primeiro.
        ALL.add(new Metric(KEY_WEIGHT, R.string.label_weight, UnitKind.WEIGHT, Source.SCALE, 1, false));
        ALL.add(new Metric(KEY_FAT, R.string.label_fat, UnitKind.PERCENT, Source.ESTIMATED, 1, false));
        ALL.add(new Metric(KEY_WATER, R.string.label_water, UnitKind.PERCENT, Source.ESTIMATED, 1, true));
        ALL.add(new Metric(KEY_MUSCLE, R.string.label_muscle, UnitKind.PERCENT, Source.SCALE, 1, true));
        ALL.add(new Metric(KEY_BMI, R.string.label_bmi, UnitKind.NONE, Source.CALCULATED, 1, false));
        ALL.add(new Metric(KEY_WAIST, R.string.label_waist, UnitKind.LENGTH, Source.MANUAL, 1, false));

        ALL.add(new Metric(KEY_LBM, R.string.label_lbm, UnitKind.WEIGHT, Source.ESTIMATED, 1, true));
        ALL.add(new Metric(KEY_BONE, R.string.label_bone, UnitKind.WEIGHT, Source.SCALE, 1, false));
        ALL.add(new Metric(KEY_VISCERAL, R.string.label_visceral_fat, UnitKind.NONE, Source.SCALE, 1, false));
        ALL.add(new Metric(KEY_HIP, R.string.label_hip, UnitKind.LENGTH, Source.MANUAL, 1, false));
        ALL.add(new Metric(KEY_WHTR, R.string.label_whtr, UnitKind.NONE, Source.CALCULATED, 2, false));
        ALL.add(new Metric(KEY_WHR, R.string.label_whr, UnitKind.NONE, Source.CALCULATED, 2, false));
        ALL.add(new Metric(KEY_CHEST, R.string.label_chest, UnitKind.LENGTH, Source.MANUAL, 1, false));
        ALL.add(new Metric(KEY_THIGH, R.string.label_thigh, UnitKind.LENGTH, Source.MANUAL, 1, false));
        ALL.add(new Metric(KEY_BICEPS, R.string.label_biceps, UnitKind.LENGTH, Source.MANUAL, 1, false));
        ALL.add(new Metric(KEY_NECK, R.string.label_neck, UnitKind.LENGTH, Source.MANUAL, 1, false));
        ALL.add(new Metric(KEY_BMR, R.string.label_bmr, UnitKind.ENERGY, Source.CALCULATED, 0, false));
        ALL.add(new Metric(KEY_TDEE, R.string.label_tdee, UnitKind.ENERGY, Source.CALCULATED, 0, false));
        ALL.add(new Metric(KEY_CALORIES, R.string.label_calories, UnitKind.ENERGY, Source.CALCULATED, 0, false));
        ALL.add(new Metric(KEY_FAT_CALIPER, R.string.label_fat_caliper, UnitKind.PERCENT, Source.MANUAL, 1, false));
        ALL.add(new Metric(KEY_CALIPER1, R.string.label_caliper1_female, UnitKind.LENGTH, Source.MANUAL, 1, false));
        ALL.add(new Metric(KEY_CALIPER2, R.string.label_caliper2_female, UnitKind.LENGTH, Source.MANUAL, 1, false));
        ALL.add(new Metric(KEY_CALIPER3, R.string.label_caliper3_female, UnitKind.LENGTH, Source.MANUAL, 1, false));
    }

    /** Todas as métricas conhecidas, na ordem padrão. */
    public static List<Metric> all() {
        return new ArrayList<>(ALL);
    }

    public static Metric byKey(String key) {
        for (Metric m : ALL) {
            if (m.key.equals(key)) {
                return m;
            }
        }
        return null;
    }

    private MetricCatalog() {
    }
}
