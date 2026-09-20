/*
 * openScale
 * Copyright (C) 2026 openScale contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.health.openscale.ui.design

import com.health.openscale.core.data.MeasurementType

/**
 * A ordem em que as métricas aparecem, tanto na grade de Composição (tela
 * Hoje) quanto nos chips do Histórico.
 *
 * O banco devolve na ordem de criação dos tipos, que abre por IMC e massa
 * magra — derivadas, não o que a balança mede. O protótipo abre pelas quatro
 * métricas que a bioimpedância entrega, e é essa ordem que vale aqui.
 *
 * Fica num arquivo próprio porque as duas telas a usam: duplicá-la seria
 * arriscar que a grade e os chips discordem entre si.
 *
 * Métricas fora desta lista vêm depois, na ordem do banco.
 */
val DESIGN_METRIC_ORDER = listOf(
    MeasurementType.WEIGHT,
    MeasurementType.BODY_FAT,
    MeasurementType.WATER,
    MeasurementType.MUSCLE,
    MeasurementType.BMI,
    MeasurementType.WAIST,
)
