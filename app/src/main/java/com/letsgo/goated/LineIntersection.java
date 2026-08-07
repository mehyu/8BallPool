package com.letsgo.goated;

public class LineIntersection {
    public float endX;
    public float endY;
    public int wallHit;

    public LineIntersection() {
        this.endX = 0;
        this.endY = 0;
        this.wallHit = 0;
    }

    public LineIntersection(float endX, float endY, int wallHit) {
        this.endX = endX;
        this.endY = endY;
        this.wallHit = wallHit;
    }

    public void set(float endX, float endY, int wallHit) {
        this.endX = endX;
        this.endY = endY;
        this.wallHit = wallHit;
    }

    public static LineIntersection getLineIntersectionPoint(float startX, float startY, float angle, int canvasWidth, int canvasHeight, float ballRadius) {
        LineIntersection result = new LineIntersection();
        calculateLineIntersectionPoint(startX, startY, angle, canvasWidth, canvasHeight, ballRadius, result);
        return result;
    }

    public static void calculateLineIntersectionPoint(float startX, float startY, float angle, int canvasWidth, int canvasHeight, float ballRadius, LineIntersection outResult) {
        double radAngle = Math.toRadians(angle);

        float dx = (float) Math.cos(radAngle);
        float dy = (float) -Math.sin(radAngle);

        float distanceParameter = Float.MAX_VALUE;
        int wall = 0;

        float effectiveLeftWall = 0 + ballRadius;
        float effectiveRightWall = canvasWidth - ballRadius;
        float effectiveTopWall = 0 + ballRadius;
        float effectiveBottomWall = canvasHeight - ballRadius;

        if (dx < 0) {
            float distanceParameter_left = (effectiveLeftWall - startX) / dx;
            if (distanceParameter_left >= -0.001f && distanceParameter_left < distanceParameter) {
                float intersectY = startY + distanceParameter_left * dy;
                if (intersectY >= effectiveTopWall && intersectY <= effectiveBottomWall) {
                    distanceParameter = distanceParameter_left;
                    wall = 3;
                }
            }
        }

        if (dx > 0) {
            float distanceParameter_right = (effectiveRightWall - startX) / dx;
            if (distanceParameter_right >= -0.001f && distanceParameter_right < distanceParameter) {
                float intersectY = startY + distanceParameter_right * dy;
                if (intersectY >= effectiveTopWall && intersectY <= effectiveBottomWall) {
                    distanceParameter = distanceParameter_right;
                    wall = 4;
                }
            }
        }

        if (dy < 0) {
            float distanceParameter_top = (effectiveTopWall - startY) / dy;
            if (distanceParameter_top >= -0.001f && distanceParameter_top < distanceParameter) {
                float intersectX = startX + distanceParameter_top * dx;
                if (intersectX >= effectiveLeftWall && intersectX <= effectiveRightWall) {
                    distanceParameter = distanceParameter_top;
                    wall = 1;
                }
            }
        }

        if (dy > 0) {
            float distanceParameter_bottom = (effectiveBottomWall - startY) / dy;
            if (distanceParameter_bottom >= -0.001f && distanceParameter_bottom < distanceParameter) {
                float intersectX = startX + distanceParameter_bottom * dx;
                if (intersectX >= effectiveLeftWall && intersectX <= effectiveRightWall) {
                    distanceParameter = distanceParameter_bottom;
                    wall = 2;
                }
            }
        }

        if (distanceParameter == Float.MAX_VALUE) {
            distanceParameter = Math.max(canvasWidth, canvasHeight);
        }

        float endX = startX + distanceParameter * dx;
        float endY = startY + distanceParameter * dy;

        if (outResult != null) {
            outResult.set(endX, endY, wall);
        }
    }
}
