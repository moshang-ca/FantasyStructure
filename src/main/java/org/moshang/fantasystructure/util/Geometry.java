package org.moshang.fantasystructure.util;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import java.util.*;

import static net.minecraft.util.Mth.PI;
import static net.minecraft.util.Mth.TWO_PI;
import static org.moshang.fantasystructure.util.MathUtil.PHI;

public class Geometry {
    public static List<Vertex> ICOSAHEDRON = icosahedron();
    public static List<Vertex> CUBE = cube();

    public static List<Vertex> subdivide(List<Vertex> vertices, int subdivisions) {
        List<Vertex> result = new ArrayList<>(vertices);

        for(int i = 0; i < subdivisions; ++i) {
            result = subdivideOnce(result);
        }

        return result;
    }

    /**
     * Return a icosphere mesh data
     * @param radius the radius of sphere
     * @param subdivisions the number of subdivisions
     */
    public static List<Vertex> icosphere(float radius, int subdivisions) {
        List<Vertex> sphere = smoothNormals(subdivide(ICOSAHEDRON, subdivisions));

        List<Vertex> result = new ArrayList<>();
        for(Vertex v : sphere) {
            float len = Mth.sqrt(v.x * v.x + v.y * v.y + v.z * v.z);
            float nx = v.x / len;
            float ny = v.y / len;
            float nz = v.z / len;

            float x = nx * radius;
            float y = ny * radius;
            float z = nz * radius;

            float u = (float) (Mth.atan2(z, x) / TWO_PI + .5);
            float v_ = (float) (Math.asin(MathUtil.clamp(y / radius, -.9999f, .9999f))) / PI + .5f;

            result.add(new Vertex(x, y, z, v.nx, v.ny, v.nz, u, v_));
        }
        return result;
    }

    /**
     * Return a unit cube mesh data,
     * use {@link #CUBE} instead.
     */
    public static List<Vertex> cube() {
        final List<Vec3> vertices = List.of(
                new Vec3(-1, -1,  1).normalize(),
                new Vec3( 1, -1,  1).normalize(),
                new Vec3( 1,  1,  1).normalize(),
                new Vec3(-1,  1,  1).normalize(),
                new Vec3(-1, -1, -1).normalize(),
                new Vec3( 1, -1, -1).normalize(),
                new Vec3( 1,  1, -1).normalize(),
                new Vec3(-1,  1, -1).normalize()
        );
        final int[][] CUBE_FACE = {
                {0, 1, 2}, {2, 3, 0},
                {1, 5, 6}, {6, 2, 1},
                {5, 4, 7}, {7, 6, 5},
                {4, 0, 3}, {3, 7, 4},
                {3, 2, 6}, {6, 7, 3},
                {4, 5, 1}, {1, 0, 4}
        };

        return calFace(vertices, CUBE_FACE);
    }

    /**
     * Return a regular icosahedron mesh data,
     * use {@link #ICOSAHEDRON} instead.
     */
    public static List<Vertex> icosahedron() {
        final List<Vec3> vertices = List.of(
                new Vec3(-1, PHI, 0).normalize(),
                new Vec3(1, PHI, 0).normalize(),
                new Vec3(-1, -PHI, 0).normalize(),
                new Vec3(1, -PHI, 0).normalize(),
                new Vec3(0, -1, PHI).normalize(),
                new Vec3(0, 1, PHI).normalize(),
                new Vec3(0, -1, -PHI).normalize(),
                new Vec3(0, 1, -PHI).normalize(),
                new Vec3(PHI, 0, -1).normalize(),
                new Vec3(PHI, 0, 1).normalize(),
                new Vec3(-PHI, 0, -1).normalize(),
                new Vec3(-PHI, 0, 1).normalize()
        );
        final int[][] ICOSAHEDRON_FACES = {
                {0, 11, 5}, {0, 5, 1}, {0, 1, 7}, {0, 7, 10}, {0, 10, 11},
                {1, 5, 9}, {5, 11, 4}, {11, 10, 2}, {10, 7, 6}, {7, 1, 8},
                {3, 9, 4}, {3, 4, 2}, {3, 2, 6}, {3, 6, 8}, {3, 8, 9},
                {4, 9, 5}, {2, 4, 11}, {6, 2, 10}, {8, 6, 7}, {9, 8, 1}
        };

        return calFace(vertices, ICOSAHEDRON_FACES);
    }

    static List<Vertex> calFace(List<Vec3> vertices, int[][] primitives) {
        List<Vertex> mesh = new ArrayList<>();
        for(var face : primitives) {
            Vec3 v1 = vertices.get(face[0]);
            Vec3 v2 = vertices.get(face[1]);
            Vec3 v3 = vertices.get(face[2]);

            Vec3 edge1 = v2.subtract(v1);
            Vec3 edge2 = v3.subtract(v1);
            Vec3 normal = edge1.cross(edge2);

            if (normal.dot(v1) < 0) {
                Vec3 tmp = v2;
                v2 = v3;
                v3 = tmp;
            }

            mesh.add(new Vertex(v1));
            mesh.add(new Vertex(v2));
            mesh.add(new Vertex(v3));
        }

        return mesh;
    }

    static List<Vertex> subdivideOnce(List<Vertex> vertices) {
        Map<Edge, Vertex> edgeCache = new HashMap<>();
        List<Vertex> newVertices = new ArrayList<>();

        for(int i = 0; i < vertices.size(); i += 3) {
            Vertex v1 = vertices.get(i);
            Vertex v2 = vertices.get(i + 1);
            Vertex v3 = vertices.get(i + 2);

            Vertex m12 = getMidPoint(v1, v2, edgeCache);
            Vertex m23 = getMidPoint(v2, v3, edgeCache);
            Vertex m31 = getMidPoint(v3, v1, edgeCache);

            newVertices.addAll(Arrays.asList(v1, m12, m31));
            newVertices.addAll(Arrays.asList(v2, m23, m12));
            newVertices.addAll(Arrays.asList(v3, m31, m23));
            newVertices.addAll(Arrays.asList(m12, m23, m31));
        }
        return newVertices;
    }

    public static List<Vertex> smoothNormals(List<Vertex> vertices) {
        Map<PositionKey, List<Integer>> positionGroup = new HashMap<>();
        for(int i = 0; i < vertices.size(); ++i) {
            Vertex v = vertices.get(i);
            PositionKey key = new PositionKey(v.x, v.y, v.z);
            positionGroup.computeIfAbsent(key, k -> new ArrayList<>()).add(i);
        }

        float[] normals = new float[vertices.size() * 3];
        for(var group : positionGroup.values()) {
            float nx = 0, ny = 0, nz = 0;
            for(int idx : group) {
                Vertex v = vertices.get(idx);
                nx += v.nx;
                ny += v.ny;
                nz += v.nz;
            }
            float len = Mth.sqrt(nx * nx + ny * ny + nz * nz);
            if(len > 0) {
                nx /= len;
                ny /= len;
                nz /= len;
            }
            for(int idx : group) {
                normals[idx * 3] = nx;
                normals[idx * 3 + 1] = ny;
                normals[idx * 3 + 2] = nz;
            }
        }

        List<Vertex> result = new ArrayList<>();
        for (int i = 0; i < vertices.size(); i++) {
            Vertex v = vertices.get(i);
            result.add(new Vertex(
                    v.x, v.y, v.z,
                    normals[i * 3], normals[i * 3 + 1], normals[i * 3 + 2],
                    v.u, v.v
            ));
        }
        return result;
    }

    private static Vertex getMidPoint(Vertex v1, Vertex v2, Map<Edge, Vertex> edgeCache) {
        Edge edge = new Edge(v1, v2);
        if(edgeCache.containsKey(edge)) {
            return edgeCache.get(edge);
        }

        float x = (v1.x + v2.x) * 0.5f;
        float y = (v1.y + v2.y) * 0.5f;
        float z = (v1.z + v2.z) * 0.5f;

        float len = Mth.sqrt(x * x + y * y + z * z);
        if(len > 1e-6f) {
            x /= len;
            y /= len;
            z /= len;
        }

        Vertex mid = new Vertex(x, y, z, x, y, z, 0, 0);
        edgeCache.put(edge, mid);
        return mid;
    }

    public record Vertex(float x, float y, float z,
                         float nx, float ny, float nz,
                         float u, float v, int color) {
        public Vertex(float x, float y, float z, float nx, float ny, float nz, float u, float v) {
            this(x, y, z, nx, ny, nz, u, v, -1);
        }

        public Vertex(Vec3 vert) {
            this((float) vert.x, (float) vert.y, (float) vert.z,
                    (float) vert.x, (float) vert.y, (float) vert.z,
                    0, 0);
        }

        public float[] rgb() {
            return MathUtil.colorToFloat3D(color);
        }

        public float[] rgba() {
            return MathUtil.colorToFloat4D(color);
        }

        public boolean hasColor() {
            return color >= 0;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof Vertex vertex)) return false;
            return Mth.abs(x - vertex.x) < 1e-6f &&
                    Mth.abs(y - vertex.y) < 1e-6f &&
                    Mth.abs(z - vertex.z) < 1e-6f;
        }

        @Override
        public int hashCode() {
            int result = Float.floatToIntBits(x);
            result = 31 * result + Float.floatToIntBits(y);
            result = 31 * result + Float.floatToIntBits(z);
            return result;
        }
    }

    public static class Edge {
        final Vertex v1, v2;
        final int hash;

        public Edge(Vertex v1, Vertex v2) {
            if(v1.hashCode() <= v2.hashCode()) {
                this.v1 = v1;
                this.v2 = v2;
            } else {
                this.v1 = v2;
                this.v2 = v1;
            }
            this.hash = computeHash();
        }

        private int computeHash() {
            int result = Float.floatToIntBits(v1.x);
            result = 31 * result + Float.floatToIntBits(v1.y);
            result = 31 * result + Float.floatToIntBits(v1.z);
            result = 31 * result + Float.floatToIntBits(v2.x);
            result = 31 * result + Float.floatToIntBits(v2.y);
            result = 31 * result + Float.floatToIntBits(v2.z);
            return result;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof Edge edge)) return false;
            return v1.equals(edge.v1) && v2.equals(edge.v2);
        }

        @Override
        public int hashCode() {
            return hash;
        }
    }

    record PositionKey(float x, float y, float z) {
        PositionKey(float x, float y, float z) {
            this.x = Math.round(x * 10000) / 10000f;
            this.y = Math.round(y * 10000) / 10000f;
            this.z = Math.round(z * 10000) / 10000f;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof PositionKey k)) return false;
            return x == k.x && y == k.y && z == k.z;
        }

        @Override
        public int hashCode() {
            return Float.hashCode(x) ^ Float.hashCode(y) ^ Float.hashCode(z);
        }
    }

}
