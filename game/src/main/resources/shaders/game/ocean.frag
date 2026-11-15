#version 330 core

in vec3 vWorldPosition;
in vec3 vNormal;
in vec2 vUV;
in float vWave;

uniform vec3 uLightDir;
uniform vec3 uViewPos;
uniform float uTime;

out vec4 fragColor;

float hash(vec2 p) {
    return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453);
}

float noise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);

    f = f * f * (3.0 - 2.0 * f);

    return mix(
            mix(hash(i), hash(i + vec2(1.0, 0.0)), f.x),
            mix(hash(i + vec2(0.0, 1.0)), hash(i + vec2(1.0, 1.0)), f.x),
            f.y
    );
}

float fbm(vec2 p) {
    float value = 0.0;
    float amplitude = 0.5;

    for (int i = 0; i < 5; i++) {
        value += noise(p) * amplitude;
        p *= 2.0;
        amplitude *= 0.5;
    }

    return value;
}

void main() {
    vec3 N = normalize(vNormal);
    vec3 V = normalize(uViewPos - vWorldPosition);
    vec3 L = normalize(-uLightDir);

    float diffuse = max(dot(N, L), 0.0);
    float fresnel = pow(1.0 - max(dot(N, V), 0.0), 3.0);

    vec2 p = vWorldPosition.xz;

    float broad = fbm(p * 0.055 + vec2(uTime * 0.012, -uTime * 0.008));
    float medium = fbm(p * 0.16 + vec2(-uTime * 0.018, uTime * 0.013));
    float fine = fbm(p * 0.38 + vec2(uTime * 0.035, -uTime * 0.025));

    vec2 flow = p * 0.09;
    flow.x += sin(p.y * 0.08 + uTime * 0.12) * 0.7;
    flow.y += cos(p.x * 0.07 - uTime * 0.1) * 0.7;

    float brush = fbm(flow * vec2(2.8, 0.65));
    float brushMask = smoothstep(0.52, 0.78, brush);

vec3 deep = vec3(0.043, 0.039, 0.047);
vec3 ocean = vec3(0.125, 0.102, 0.137);
vec3 blue = vec3(0.196, 0.125, 0.20);
vec3 turquoise = vec3(0.322, 0.133, 0.251);
vec3 light = vec3(0.478, 0.122, 0.176);
vec3 foamColour = vec3(0.561, 0.231, 0.145);

    vec3 colour = mix(deep, ocean, smoothstep(0.12, 0.72, broad));
    colour = mix(colour, blue, smoothstep(0.32, 0.78, medium) * 0.7);
    colour = mix(colour, turquoise, smoothstep(0.58, 0.88, broad) * 0.4);

    colour += (fine - 0.5) * 0.055;

    float lighting = 0.65 + diffuse * 0.35;
    colour *= lighting;

    float specular = pow(max(dot(reflect(-L, N), V), 0.0), 24.0);
    specular *= 0.25 + medium * 0.35;

    float broadHighlight = smoothstep(0.62, 0.82, brush);
    float crest = smoothstep(1.1, 2.0, vWave);

    float foam = brushMask * crest;
    float highlights = broadHighlight * (0.15 + crest * 0.55);

    colour = mix(colour, light, highlights * 0.4);
    colour = mix(colour, foamColour, foam * 0.55);

    colour += vec3(0.70, 0.55, 0.58) * specular;

    colour = mix(
            colour,
            vec3(0.22, 0.12, 0.20),
            fresnel * 0.35
    );

    fragColor = vec4(colour, 1.0);
}