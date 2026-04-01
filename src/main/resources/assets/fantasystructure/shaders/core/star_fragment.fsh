#version 150

uniform sampler2D Sampler0;
uniform float time;
uniform vec3 coreColor;
uniform vec3 edgeColor;
//uniform float brightness;
//uniform vec2 resolution;

in vec2 texCoord;
in vec3 modelPosition;
in vec3 viewDirection;
in vec3 vertexNormal;

out vec4 fragColor;

vec4 triplanarSample(sampler2D tex, vec3 normal, vec3 position, float scale, float time) {
    vec3 scaledPos = position * scale;

    vec2 offsetX = vec2(time * 1.15, time * 0.1);
    vec2 offsetY = vec2(time * 0.12, time * 0.18);
    vec2 offsetZ = vec2(time * 0.2, time * 0.22);

    vec2 uvX = scaledPos.yz + offsetX;
    vec2 uvY = scaledPos.xz + offsetY;
    vec2 uvZ = scaledPos.xy + offsetY;

    vec4 xColor = texture(tex, uvX);
    vec4 yColor = texture(tex, uvY);
    vec4 zColor = texture(tex, uvZ);

    vec3 blend = abs(normal);
    blend = pow(blend, vec3(4.0));
    blend /= (blend.x + blend.y + blend.z);

    return xColor * blend.x + yColor * blend.y + zColor * blend.z;
}

void main() {
    float NdotV = max(0., dot(vertexNormal, viewDirection));
    float fresnel = pow(1 - NdotV, 6.);

    float scale = .5;
    vec4 starTex = triplanarSample(Sampler0, vertexNormal, modelPosition, scale, time);

    vec3 baseColor = coreColor * starTex.r;
    vec3 edgeColor = edgeColor * fresnel;

    vec3 finalColor = baseColor + edgeColor;
    fragColor = vec4(finalColor, 1.);
}