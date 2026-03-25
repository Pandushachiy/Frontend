package com.health.companion.presentation.screens.splash

import android.graphics.RuntimeShader
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ShaderBrush

private const val SHADER_SRC = """
uniform float2 iResolution;
uniform float  iTime;
uniform float  progress;

float hash21(float2 p) {
    p = fract(p * float2(123.34, 456.21));
    p += dot(p, p + 34.45);
    return fract(p.x * p.y);
}

float noise(float2 p) {
    float2 i = floor(p);
    float2 f = fract(p);
    float2 u = f * f * (3.0 - 2.0 * f);

    float a = hash21(i);
    float b = hash21(i + float2(1.0, 0.0));
    float c = hash21(i + float2(0.0, 1.0));
    float d = hash21(i + float2(1.0, 1.0));

    return mix(mix(a, b, u.x), mix(c, d, u.x), u.y);
}

float fbm(float2 p) {
    float v = 0.0;
    float a = 0.5;
    for (int i = 0; i < 5; ++i) {
        v += noise(p) * a;
        p = float2(
            p.x * 1.7 - p.y * 1.2,
            p.x * 1.2 + p.y * 1.7
        ) + 11.7;
        a *= 0.5;
    }
    return v;
}

half3 auroraLayer(float2 uv, float t, float p) {
    float appear = smoothstep(0.06, 0.30, p);
    float fade = 1.0 - smoothstep(0.76, 0.96, p);

    float2 q = uv;
    q.x *= 1.05;
    q.y += 0.18;

    float warp1 = fbm(q * 2.2 + float2(t * 0.08, -t * 0.05));
    float warp2 = fbm(q * 3.1 - float2(t * 0.05, t * 0.07));

    float bandA = sin(q.x * 2.8 + warp1 * 3.2 - t * 0.9);
    float bandB = sin(q.x * 1.6 - q.y * 1.4 + warp2 * 2.5 + t * 0.7);

    float ribbon = smoothstep(0.18, 0.95, bandA * 0.55 + bandB * 0.45);
    ribbon *= smoothstep(0.78, -0.08, uv.y);
    ribbon *= appear * fade * 0.22;

    half3 c1 = half3(0.08, 0.88, 0.76);
    half3 c2 = half3(0.42, 0.36, 1.00);
    half3 c3 = half3(0.10, 0.56, 1.00);

    return mix(c1, c2, half(warp1)) * half(ribbon)
         + c3 * half(ribbon * 0.30);
}

half3 particleLayer(float2 uv, float t, float p) {
    float appear = smoothstep(0.12, 0.34, p);
    float fade = 1.0 - smoothstep(0.72, 0.94, p);

    half3 col = half3(0.0);
    for (int i = 0; i < 14; ++i) {
        float fi = float(i);
        float seed = fi * 19.31;

        float x = hash21(float2(seed, 1.7)) * 2.0 - 1.0;
        float y0 = hash21(float2(seed, 2.9));
        float speed = 0.05 + hash21(float2(seed, 5.2)) * 0.10;
        float phase = hash21(float2(seed, 8.3)) * 6.2831;

        float y = fract(y0 - t * speed + phase * 0.03);
        y = y * 1.9 - 0.95;

        float drift = sin(t * 0.45 + phase) * 0.03;
        float2 pos = float2(x * 0.56 + drift, y);

        float d = length(uv - pos);
        float glow = exp(-d * d * 650.0);

        col += half3(0.90, 0.96, 1.00) * half(glow * 0.055 * appear * fade);
    }
    return col;
}

half4 main(float2 fc) {
    float2 uv = (fc - iResolution * 0.5) / min(iResolution.x, iResolution.y);
    float t = iTime;
    float p = clamp(progress, 0.0, 1.0);

    float2 center = float2(0.0, 0.06);
    float2 cuv = uv - center;
    float dist = length(cuv);
    float ang = atan(cuv.y, cuv.x);

    float intro = smoothstep(0.0, 0.26, p);
    float settle = smoothstep(0.18, 0.55, p);
    float outro = 1.0 - smoothstep(0.84, 1.0, p);

    half3 col = half3(0.025, 0.035, 0.060);

    float bgGrad = smoothstep(1.15, -0.15, uv.y);
    col += half3(0.00, 0.10, 0.16) * half(bgGrad * 0.32);
    col += half3(0.12, 0.06, 0.22) * half((1.0 - bgGrad) * 0.18);

    float bgNoise = fbm(uv * 1.5 + float2(t * 0.02, -t * 0.015));
    col += half3(0.03, 0.06, 0.09) * half(bgNoise * 0.10);

    col += auroraLayer(uv, t, p);
    col += particleLayer(uv, t, p);

    float breath = 1.0 + 0.06 * sin(t * 1.9);
    float coreR = mix(0.025, 0.11, smoothstep(0.0, 0.42, p)) * breath;

    float core = exp(-(dist * dist) / (coreR * coreR));
    float innerGlow = exp(-(dist * dist) / (coreR * coreR * 3.8));
    float outerGlow = exp(-(dist * dist) / (coreR * coreR * 12.0));

    half3 coreColor = half3(0.90, 0.98, 1.00);
    half3 edgeColor = half3(0.18, 0.92, 0.82);
    half3 haloColor = half3(0.34, 0.42, 1.00);

    col += coreColor * half(core * 0.95 * intro);
    col += edgeColor * half(innerGlow * 0.40 * intro);
    col += haloColor * half(outerGlow * 0.22 * intro);

    float ringT = smoothstep(0.18, 0.70, p);
    float ringFade = 1.0 - smoothstep(0.70, 0.92, p);
    float ringR = mix(0.09, 0.42, ringT);
    float ringW = mix(0.040, 0.010, ringT);

    float ringDelta = (dist - ringR) / ringW;
    float ring = exp(-ringDelta * ringDelta);
    ring *= 0.86 + 0.14 * sin(ang * 6.0 - t * 2.2);
    ring *= ringFade;

    col += half3(0.40, 0.95, 0.88) * half(ring * 0.22);
    col += half3(0.58, 0.50, 1.00) * half(ring * 0.14);

    float sheathNoise = fbm(
        float2(
            ang * 1.4 + t * 0.08,
            dist * 5.5 - t * 0.12
        )
    );

    float sheath = smoothstep(0.0, 0.7, settle) * exp(-dist * 7.5);
    col += half3(0.10, 0.90, 0.82) * half(sheathNoise * sheath * 0.20);
    col += half3(0.34, 0.42, 1.00) * half((1.0 - sheathNoise) * sheath * 0.10);

    float vignette = 1.0 - smoothstep(0.45, 1.18, length(uv));
    col *= half(0.72 + vignette * 0.28);

    float finalLift = smoothstep(0.62, 0.82, p) * (1.0 - smoothstep(0.84, 0.96, p));
    col = mix(col, half3(0.92, 0.98, 1.00), half(finalLift * 0.12));

    col *= half(outro);

    return half4(col, half(outro));
}
"""

@Composable
fun SplashOverlay(
    progress: Float,
    modifier: Modifier = Modifier,
) {
    val shader = remember { RuntimeShader(SHADER_SRC) }

    val smoothedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(
            durationMillis = 700,
            easing = FastOutSlowInEasing
        ),
        label = "splashProgress"
    )

    val startTimeNanos = remember { System.nanoTime() }
    val time by produceState(initialValue = 0f) {
        while (true) {
            withFrameNanos { frameTimeNanos ->
                value = (frameTimeNanos - startTimeNanos) / 1_000_000_000f
            }
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        shader.setFloatUniform("iResolution", size.width, size.height)
        shader.setFloatUniform("iTime", time)
        shader.setFloatUniform("progress", smoothedProgress)
        drawRect(ShaderBrush(shader))
    }
}
