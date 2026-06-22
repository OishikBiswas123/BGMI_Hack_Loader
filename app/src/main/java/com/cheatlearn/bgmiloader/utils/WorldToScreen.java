package com.cheatlearn.bgmiloader.utils;

import android.util.Log;

public final class WorldToScreen {

    private static final String TAG = "W2S";

    public static boolean project(
            float[] viewMatrix,
            Vector3 worldPos,
            Vector2 screenOut,
            int screenWidth,
            int screenHeight
    ) {
        float screenW = viewMatrix[3]  * worldPos.x
                      + viewMatrix[7]  * worldPos.y
                      + viewMatrix[11] * worldPos.z
                      + viewMatrix[15];

        if (screenW < 0.001f) return false;

        float invW = 1.0f / screenW;
        float sightX = screenWidth  / 2.0f;
        float sightY = screenHeight / 2.0f;

        screenOut.x = sightX + (viewMatrix[0] * worldPos.x
                               + viewMatrix[4] * worldPos.y
                               + viewMatrix[8] * worldPos.z
                               + viewMatrix[12]) * invW * sightX;
        screenOut.y = sightY - (viewMatrix[1] * worldPos.x
                               + viewMatrix[5] * worldPos.y
                               + viewMatrix[9] * worldPos.z
                               + viewMatrix[13]) * invW * sightY;

        return true;
    }

    public static boolean projectPlayer(
            float[] viewMatrix,
            Vector3 worldPos,
            Vector3 screenOut,
            int screenWidth,
            int screenHeight
    ) {
        float screenW = viewMatrix[3]  * worldPos.x
                      + viewMatrix[7]  * worldPos.y
                      + viewMatrix[11] * worldPos.z
                      + viewMatrix[15];

        if (screenW < 0.001f) return false;

        float screenY = viewMatrix[1]  * worldPos.x
                      + viewMatrix[5]  * worldPos.y
                      + viewMatrix[9]  * worldPos.z
                      + viewMatrix[13];
        float screenX = viewMatrix[0]  * worldPos.x
                      + viewMatrix[4]  * worldPos.y
                      + viewMatrix[8]  * worldPos.z
                      + viewMatrix[12];

        float invW = 1.0f / screenW;
        screenOut.x = (screenWidth  / 2.0f) + (screenWidth  / 2.0f) * screenX * invW;
        screenOut.y = (screenHeight / 2.0f) - (screenHeight / 2.0f) * screenY * invW;

        float yTop = (screenHeight / 2.0f)
                   - (viewMatrix[1] * worldPos.x + viewMatrix[5] * worldPos.y + viewMatrix[9]  * (worldPos.z + 85) + viewMatrix[13])
                   * (screenHeight / 2.0f) * invW;
        screenOut.z = yTop - screenOut.y;

        return true;
    }
}
