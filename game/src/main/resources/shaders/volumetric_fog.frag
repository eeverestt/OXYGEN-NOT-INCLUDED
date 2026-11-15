#version 330 core

in vec2 vUV;
out vec4 FragColor;

uniform sampler2D uColorTex;
uniform sampler2D uDepthTex;
uniform mat4 uInvViewProj;
uniform vec3 uCameraPos;
uniform vec3 uLightDir;

uniform vec3  uFogColor;
uniform float uFogDensity;
uniform float uFogHeightFalloff;
uniform float uFogBaseHeight;
uniform float uNoiseScale;
uniform float uMaxDistance;
uniform float uTime;

float hash(vec3 p) {
    p = fract(p * 0.3183099 + vec3(0.1, 0.2, 0.3));
    p *= 17.0;
    return fract(p.x * p.y * p.z * (p.x + p.y + p.z));
}
float noise(vec3 x) {
    vec3 i = floor(x), f = fract(x);
    f = f * f * (3.0 - 2.0 * f);
    return mix(
            mix(mix(hash(i+vec3(0,0,0)), hash(i+vec3(1,0,0)), f.x),
                    mix(hash(i+vec3(0,1,0)), hash(i+vec3(1,1,0)), f.x), f.y),
            mix(mix(hash(i+vec3(0,0,1)), hash(i+vec3(1,0,1)), f.x),
                    mix(hash(i+vec3(0,1,1)), hash(i+vec3(1,1,1)), f.x), f.y),
            f.z);
}
float fbm(vec3 p) {
    float v = 0.0, a = 0.5;
    for (int i = 0; i < 4; ++i) { v += a * noise(p); p *= 2.0; a *= 0.5; }
    return v;
}

vec3 worldFromDepth(vec2 uv, float depth) {
    vec4 ndc = vec4(uv * 2.0 - 1.0, depth * 2.0 - 1.0, 1.0);
    vec4 w = uInvViewProj * ndc;
    return w.xyz / w.w;
}

void main() {
    vec3 sceneColor = texture(uColorTex, vUV).rgb;
    float depth = texture(uDepthTex, vUV).r;

    vec3 worldPos = worldFromDepth(vUV, depth);
    vec3 rayDir = normalize(worldPos - uCameraPos);
    float sceneDist  = length(worldPos - uCameraPos);

    if (depth >= 0.9999) sceneDist = uMaxDistance;
    float marchDist = min(sceneDist, uMaxDistance);

    float sunAmount = clamp(dot(rayDir, -normalize(uLightDir)) * 0.5 + 0.5, 0.0, 1.0);
    vec3 fogTint = uFogColor * (0.6 + 0.8 * sunAmount);

    const int STEPS = 48;
    float stepSize = marchDist / float(STEPS);

    float jitter = fract(sin(dot(vUV, vec2(12.9898, 78.233)) + uTime * 0.5) * 43758.5453);

    float transmittance = 1.0;
    vec3  scattered = vec3(0.0);

    for (int i = 0; i < STEPS; ++i) {
        float t = (float(i) + jitter) * stepSize;
        vec3 p = uCameraPos + rayDir * t;

        float h = exp(-max(p.y - uFogBaseHeight, 0.0) * uFogHeightFalloff);

        vec3 nPos = p * uNoiseScale + vec3(uTime * 0.05, 0.0, uTime * 0.03);
        float n = fbm(nPos);
        n = smoothstep(0.35, 0.75, n);

        float density = uFogDensity * h * n;
        float absorbed = 1.0 - exp(-density * stepSize);

        scattered += transmittance * absorbed * fogTint;
        transmittance *= (1.0 - absorbed);

        if (transmittance < 0.01) break;
    }

    vec3 finalColor = sceneColor * transmittance + scattered;
    FragColor = vec4(finalColor, 1.0);
}