#version 330 core
in vec3 vNormal;
in vec3 vWorldPos;
in vec2 vUV;

uniform sampler2D uTex;
uniform vec3 uLightDir;
uniform vec3 uTint;

out vec4 FragColor;

void main() {
    vec3 n = normalize(vNormal);
    float diff = max(dot(n, -normalize(uLightDir)), 0.0);
    float light = 0.25 + 0.75 * diff;
    vec3 base = texture(uTex, vUV).rgb * uTint;
    FragColor = vec4(base * light, 1.0);
}
