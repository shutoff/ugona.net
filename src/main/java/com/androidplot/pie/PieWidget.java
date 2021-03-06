/*
 * Copyright 2013 AndroidPlot.com
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.androidplot.pie;

import android.graphics.Canvas;
import android.graphics.RectF;

import com.androidplot.exception.PlotRenderException;
import com.androidplot.ui.LayoutManager;
import com.androidplot.ui.SizeMetrics;
import com.androidplot.ui.widget.Widget;

/**
 * Visualizes data as a pie chart.
 */
public class PieWidget extends Widget {

    private PieChart pieChart;

    public PieWidget(LayoutManager layoutManager, PieChart pieChart, SizeMetrics metrics) {
        super(layoutManager, metrics);
        this.pieChart = pieChart;
    }

    @Override
    protected void doOnDraw(Canvas canvas, RectF widgetRect) throws PlotRenderException {

        for (PieRenderer renderer : pieChart.getRendererList()) {
            renderer.render(canvas, widgetRect);
        }
    }
}
