/*
 * Copyright 2018 Stuart Kent
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.
 *
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package com.lyldding.library;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.support.annotation.FloatRange;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;

import java.util.List;

/**
 * @author lyldding
 */
public class PolygonDrawHelper {
    private static final String TAG = "PolygonDrawHelper";
    private Path tempPath = new Path();
    private RectF tempRectF = new RectF();

    private PolygonDrawHelper() {
    }

    private static class InstanceHolder {
        private static final PolygonDrawHelper INSTANCE = new PolygonDrawHelper();
    }

    public static PolygonDrawHelper getInstance() {
        return InstanceHolder.INSTANCE;
    }

    /**
     * 绘制多边形区域
     *
     * @param canvas
     * @param sideCount    边数
     * @param radius       半径
     * @param cornerRadius 圆角半径
     * @param paint        画笔
     */
    public void drawPolygon(
            @NonNull final Canvas canvas,
            @IntRange(from = 3) final int sideCount,
            final float centerX,
            final float centerY,
            @FloatRange(from = 0, fromInclusive = false) final float radius,
            @FloatRange(from = 0) float cornerRadius,
            @NonNull final Paint paint) {

        constructPolygonPath(
                tempPath,
                sideCount,
                centerX,
                centerY,
                radius,
                cornerRadius);

        canvas.drawPath(tempPath, paint);
    }


    /**
     * 绘制路径
     *
     * @param canvas
     * @param sideCount    边数
     * @param radius       半径
     * @param cornerRadius 圆角半径
     * @param paint        画笔
     */
    public void drawPath(
            @NonNull final Canvas canvas,
            @IntRange(from = 3) final int sideCount,
            final float centerX,
            final float centerY,
            @FloatRange(from = 0, fromInclusive = false) final float radius,
            @FloatRange(from = 0) float cornerRadius,
            @NonNull final Paint paint) {

        constructPolygonPath(
                tempPath,
                sideCount,
                centerX,
                centerY,
                radius,
                cornerRadius);

        canvas.drawPath(tempPath, paint);
    }

    /**
     * Constructs a regular polygonal {@link Path}.
     *
     * @param path         the {@link Path} to be filled with polygon components. Will be reset.
     * @param sideCount    the number of sides of the polygon
     * @param centerX      the x-coordinate of the polygon center in pixels
     * @param centerY      the y-coordinate of the polygon center in pixels
     * @param outerRadius  the distance from the polygon center to any vertex (ignoring corner
     *                     rounding) in pixels
     * @param cornerRadius the radius of the rounding applied to each corner of the polygon in
     *                     pixels
     */
    private void constructPolygonPath(
            @NonNull final Path path,
            @IntRange(from = 3) final int sideCount,
            final float centerX,
            final float centerY,
            @FloatRange(from = 0, fromInclusive = false) final float outerRadius,
            @FloatRange(from = 0) final float cornerRadius) {

        path.reset();

        final float inRadius = (float) (outerRadius * Math.cos(toRadians(180.0 / sideCount)));

        if (inRadius < cornerRadius) {
            /*
             * If the supplied corner radius is too small, we default to the "incircle".
             *   - https://web.archive.org/web/20170415150442/https://en.wikipedia.org/wiki/Regular_polygon
             *   - https://web.archive.org/web/20170415150415/http://www.mathopenref.com/polygonincircle.html
             */
            path.addCircle(centerX, centerY, inRadius, Path.Direction.CW);
        } else {
            if (Math.abs(cornerRadius) < 0.01) {
                constructNonRoundedPolygonPath(
                        path,
                        sideCount,
                        outerRadius);
            } else {
                constructRoundedPolygonPath(
                        path,
                        sideCount,
                        outerRadius,
                        cornerRadius);
            }
        }
    }

    private void constructRoundedPolygonPath(
            @NonNull final Path path,
            @IntRange(from = 3) final int sideCount,
            @FloatRange(from = 0, fromInclusive = false) final float outerRadius,
            @FloatRange(from = 0) final float cornerRadius) {
        path.reset();
        final double halfInteriorCornerAngle = 90 - (180.0 / sideCount);
        final float halfCornerArcSweepAngle = (float) (90 - halfInteriorCornerAngle);
        final double distanceToCornerArcCenter = outerRadius - cornerRadius / Math.sin(toRadians(halfInteriorCornerAngle));

        for (int cornerNumber = 0; cornerNumber < sideCount; cornerNumber++) {
            final double angleToCorner = cornerNumber * (360.0 / sideCount);
            final float cornerCenterX = (float) (distanceToCornerArcCenter * Math.cos(toRadians(angleToCorner)));
            final float cornerCenterY = (float) (distanceToCornerArcCenter * Math.sin(toRadians(angleToCorner)));

            tempRectF.set(
                    cornerCenterX - cornerRadius,
                    cornerCenterY - cornerRadius,
                    cornerCenterX + cornerRadius,
                    cornerCenterY + cornerRadius);

            /*
             * Quoted from the arcTo documentation:
             *
             *   "Append the specified arc to the path as a new contour. If the start of the path is different from the
             *    path's current last point, then an automatic lineTo() is added to connect the current contour to the
             *    start of the arc. However, if the path is empty, then we call moveTo() with the first point of the
             *    arc."
             *
             * We construct our polygon by sequentially drawing rounded corners using arcTo, and leverage the
             * automatically-added moveTo/lineTo instructions to connect these corners with straight edges.
             */
            path.arcTo(
                    tempRectF,
                    (float) (angleToCorner - halfCornerArcSweepAngle),
                    2 * halfCornerArcSweepAngle);
        }

        // Draw the final straight edge.
        path.close();
    }

    /**
     * 构建路径
     *
     * @param path      路径
     * @param sideCount 边数
     * @param radius    半径
     */
    private void constructNonRoundedPolygonPath(
            @NonNull final Path path,
            @IntRange(from = 3) final int sideCount,
            @FloatRange(from = 0, fromInclusive = false) final float radius) {
        path.reset();
        for (int index = 0; index < sideCount; index++) {
            final double angleToCorner = index * (360.0 / sideCount);
            final float cornerX = (float) (radius * Math.cos(toRadians(angleToCorner)));
            final float cornerY = (float) (radius * Math.sin(toRadians(angleToCorner)));

            if (index == 0) {
                path.moveTo(cornerX, cornerY);
            } else {
                path.lineTo(cornerX, cornerY);
            }
        }
        path.close();
    }


    /**
     * 计算定点坐标
     *
     * @param pointListX X轴坐标
     * @param pointListY Y轴坐标
     * @param radius     半径
     * @param sideCount  边数
     */
    public void computeVertexPoint(List<Float> pointListX, List<Float> pointListY,
                                   @FloatRange(from = 0) float radius,
                                   @IntRange(from = 3) int sideCount,
                                   @FloatRange(from = 0) float cornerRadius) {
        int realRadius = computeRealRadius(radius, cornerRadius, sideCount);
        for (int cornerNumber = 0; cornerNumber < sideCount; cornerNumber++) {
            final double angleToCorner = cornerNumber * (360.0 / sideCount);
            pointListX.add((float) (realRadius * Math.cos(toRadians(angleToCorner))));
            pointListY.add((float) (realRadius * Math.sin(toRadians(angleToCorner))));
        }
    }

    /**
     * 计算顶点的真实半径
     *
     * @param radius       半径
     * @param cornerRadius 弧度半径
     * @param sideCount    边数
     * @return 真实半径
     */
    private int computeRealRadius(float radius, float cornerRadius, int sideCount) {
        double angleToCorner = 90 - (360.0f / sideCount) / 2;
        return (int) (radius - (cornerRadius / Math.sin(toRadians(angleToCorner)) - cornerRadius));
    }

    /**
     * 计算维度坐标
     *
     * @param pointListX     X轴坐标
     * @param pointListY     Y轴坐标
     * @param dimPercentages 各个维度值
     * @param radiusMax      半径最大值
     * @param sideCount      边数
     */
    public void computeDimPoint(List<Float> pointListX, List<Float> pointListY, List<Float> dimPercentages,
                                @FloatRange(from = 0) float radiusMax,
                                @IntRange(from = 3) int sideCount) {
        if (dimPercentages.size() != sideCount) {
            throw new IllegalArgumentException(TAG + " : sides != mDimPercentages.size()");
        }
        for (int index = 0; index < sideCount; index++) {
            final double angleToCorner = index * (360.0 / sideCount);
            float radius = dimPercentages.get(index) * radiusMax;
            pointListX.add((float) (radius * Math.cos(toRadians(angleToCorner))));
            pointListY.add((float) (radius * Math.sin(toRadians(angleToCorner))));
        }
    }


    private static double toRadians(final double degrees) {
        return 2 * Math.PI * degrees / 360;
    }

}
