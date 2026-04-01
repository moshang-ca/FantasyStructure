#version 150
#extension GL_ARB_separate_shader_objects : enable

layout(location = 0) in vec3 Position;
layout(location = 1) in vec3 Normal;
layout(location = 2) in vec2 UV0;

out vec2 texCoord;
out vec3 modelPosition;
out vec3 viewDirection;
out vec3 vertexNormal;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;

void main() {
    vec4 viewPos = ModelViewMat * vec4(Position, 1.0);
    gl_Position = ProjMat * viewPos;

    texCoord = UV0;
    vertexNormal = normalize(mat3(ModelViewMat) * Normal);
    modelPosition = Position;
    viewDirection = normalize(-viewPos.xyz);
}