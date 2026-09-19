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
package com.health.openscale.gui.redesign.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

import androidx.core.content.ContextCompat;

import com.health.openscale.R;

import java.util.List;

/**
 * Sparkline do cartão "Últimos 30 dias" na tela Hoje.
 *
 * Desenha uma linha suavizada com área preenchida, como no protótipo. Não usa
 * MPAndroidChart de propósito: aqui não há eixo, rótulo nem interação, e uma
 * View própria de ~100 linhas custa menos que configurar um LineChart inteiro
 * para desenhar um traço.
 *
 * O gráfico de verdade, com eixos e toque, está no HistoryFragment e esse sim
 * usa MPAndroidChart.
 */
public class SparklineView extends View {

    /** Espaço reservado para o círculo do último ponto não ser cortado. */
    private static final float EDGE_PADDING_DP = 6f;
    private static final float LINE_WIDTH_DP = 2.4f;
    private static final float LAST_DOT_RADIUS_DP = 4.5f;

    /** Suavização da curva. 0.3 aproxima o horizontal-bezier do protótipo. */
    private static final float SMOOTHING = 0.3f;

    private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint dotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final Path linePath = new Path();
    private final Path fillPath = new Path();

    private float[] values = null;
    private float density;

    public SparklineView(Context context) {
        super(context);
        init(context);
    }

    public SparklineView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public SparklineView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        density = context.getResources().getDisplayMetrics().density;

        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(LINE_WIDTH_DP * density);
        linePaint.setStrokeCap(Paint.Cap.ROUND);
        linePaint.setStrokeJoin(Paint.Join.ROUND);
        linePaint.setColor(ContextCompat.getColor(context, R.color.chart_line));

        fillPaint.setStyle(Paint.Style.FILL);
        fillPaint.setColor(ContextCompat.getColor(context, R.color.chart_fill));
        // O protótipo usa opacidade 0.55 sobre o preenchimento.
        fillPaint.setAlpha(140);

        dotPaint.setStyle(Paint.Style.FILL);
        dotPaint.setColor(ContextCompat.getColor(context, R.color.chart_line));
    }

    /**
     * Define os valores a desenhar, do mais antigo para o mais recente.
     * Passar null ou menos de dois pontos deixa a view vazia.
     */
    public void setValues(List<Float> newValues) {
        if (newValues == null || newValues.size() < 2) {
            values = null;
        } else {
            values = new float[newValues.size()];
            for (int i = 0; i < newValues.size(); i++) {
                values[i] = newValues.get(i);
            }
        }
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (values == null || values.length < 2) {
            return;
        }

        final float padding = EDGE_PADDING_DP * density;
        final float width = getWidth() - padding * 2f;
        final float height = getHeight() - padding * 2f;

        if (width <= 0 || height <= 0) {
            return;
        }

        float min = values[0];
        float max = values[0];
        for (float v : values) {
            if (v < min) min = v;
            if (v > max) max = v;
        }

        // Série constante: desenha uma reta no meio em vez de dividir por zero.
        final float range = (max - min) < 0.0001f ? 1f : (max - min);
        final float stepX = width / (values.length - 1);

        final float[] xs = new float[values.length];
        final float[] ys = new float[values.length];
        for (int i = 0; i < values.length; i++) {
            xs[i] = padding + stepX * i;
            if ((max - min) < 0.0001f) {
                ys[i] = padding + height / 2f;
            } else {
                // Y invertido: valor maior fica mais acima na tela.
                ys[i] = padding + height - ((values[i] - min) / range) * height;
            }
        }

        buildSmoothPath(linePath, xs, ys);

        fillPath.set(linePath);
        fillPath.lineTo(xs[xs.length - 1], getHeight());
        fillPath.lineTo(xs[0], getHeight());
        fillPath.close();

        canvas.drawPath(fillPath, fillPaint);
        canvas.drawPath(linePath, linePaint);
        canvas.drawCircle(xs[xs.length - 1], ys[ys.length - 1],
                LAST_DOT_RADIUS_DP * density, dotPaint);
    }

    /**
     * Curva de Catmull-Rom convertida em Bézier cúbica — o mesmo efeito do
     * horizontal-bezier que o protótipo usa no SVG.
     */
    private void buildSmoothPath(Path path, float[] xs, float[] ys) {
        path.reset();
        path.moveTo(xs[0], ys[0]);

        for (int i = 0; i < xs.length - 1; i++) {
            final float x0 = i == 0 ? xs[0] : xs[i - 1];
            final float y0 = i == 0 ? ys[0] : ys[i - 1];
            final float x1 = xs[i];
            final float y1 = ys[i];
            final float x2 = xs[i + 1];
            final float y2 = ys[i + 1];
            final float x3 = (i + 2 < xs.length) ? xs[i + 2] : x2;
            final float y3 = (i + 2 < ys.length) ? ys[i + 2] : y2;

            final float c1x = x1 + (x2 - x0) * SMOOTHING;
            final float c1y = y1 + (y2 - y0) * SMOOTHING;
            final float c2x = x2 - (x3 - x1) * SMOOTHING;
            final float c2y = y2 - (y3 - y1) * SMOOTHING;

            path.cubicTo(c1x, c1y, c2x, c2y, x2, y2);
        }
    }

    /** Reaplica as cores após troca de tema claro/escuro. */
    public void refreshThemeColors() {
        linePaint.setColor(ContextCompat.getColor(getContext(), R.color.chart_line));
        fillPaint.setColor(ContextCompat.getColor(getContext(), R.color.chart_fill));
        fillPaint.setAlpha(140);
        dotPaint.setColor(ContextCompat.getColor(getContext(), R.color.chart_line));
        invalidate();
    }
}
