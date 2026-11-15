#version 330 core

layout(location = 0) in vec3 aPosition;
layout(location = 1) in vec3 aNormal;
layout(location = 2) in vec2 aUV;

uniform mat4 uModel;
uniform mat4 uView;
uniform mat4 uProj;
uniform float uTime;

out vec3 vWorldPosition;
out vec3 vNormal;
out vec2 vUV;
out float vWave;

float wave(vec2 p, vec2 direction, float frequency, float amplitude, float speed) {
    return sin(dot(p, direction) * frequency + uTime * speed) * amplitude;
}

float oceanHeight(vec2 p) {
    float h = 0.0;

    h += wave(p, normalize(vec2(1.0, 0.35)), 0.09, 1.8, 0.65);
    h += wave(p, normalize(vec2(-0.4, 1.0)), 0.15, 1.1, 0.85);
    h += wave(p, normalize(vec2(0.7, -1.0)), 0.25, 0.65, 1.2);
    h += wave(p, normalize(vec2(-1.0, -0.2)), 0.38, 0.3, 1.7);

    h += wave(p, normalize(vec2(1.0, 1.0)), 0.55, 0.16, 2.1);
    h += wave(p, normalize(vec2(-0.8, 0.6)), 0.75, 0.09, 2.5);

    return h;
}

void main() {
    vec3 p = aPosition;

    float height = oceanHeight(p.xz);
    p.y += height;

    float epsilon = 0.08;

    float hL = oceanHeight(p.xz - vec2(epsilon, 0.0));
    float hR = oceanHeight(p.xz + vec2(epsilon, 0.0));
    float hD = oceanHeight(p.xz - vec2(0.0, epsilon));
    float hU = oceanHeight(p.xz + vec2(0.0, epsilon));

    vec3 tangentX = vec3(2.0 * epsilon, hR - hL, 0.0);
    vec3 tangentZ = vec3(0.0, hU - hD, 2.0 * epsilon);

    vec3 normal = normalize(cross(tangentZ, tangentX));

    vec4 worldPosition = uModel * vec4(p, 1.0);

    vWorldPosition = worldPosition.xyz;
    vNormal = normalize(mat3(transpose(inverse(uModel))) * normal);
    vUV = aUV;
    vWave = height;

    gl_Position = uProj * uView * worldPosition;
}