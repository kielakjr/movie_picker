package com.kielakjr.movie_picker.movie;

import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class DbscanClusterer {

    private static final float EPSILON = 0.65f;
    private static final int MIN_POINTS = 0;
    private static final int UNVISITED = -1;
    private static final int NOISE = -2;

    public List<float[]> findClusterCentroids(List<float[]> vectors) {
        if (vectors.isEmpty()) return List.of();

        int n = vectors.size();
        int[] labels = new int[n];
        Arrays.fill(labels, UNVISITED);

        int clusterId = 0;

        for (int i = 0; i < n; i++) {
            if (labels[i] != UNVISITED) continue;

            List<Integer> neighbors = getNeighbors(vectors, i, EPSILON);

            if (neighbors.size() < MIN_POINTS) {
                labels[i] = NOISE;
                continue;
            }

            labels[i] = clusterId;
            Queue<Integer> queue = new LinkedList<>(neighbors);

            while (!queue.isEmpty()) {
                int j = queue.poll();

                if (labels[j] == NOISE) {
                    labels[j] = clusterId;
                }
                if (labels[j] != UNVISITED) continue;

                labels[j] = clusterId;
                List<Integer> jNeighbors = getNeighbors(vectors, j, EPSILON);

                if (jNeighbors.size() >= MIN_POINTS) {
                    queue.addAll(jNeighbors);
                }
            }

            clusterId++;
        }

        return computeCentroids(vectors, labels, clusterId);
    }

    private List<Integer> getNeighbors(List<float[]> vectors, int idx, float epsilon) {
        List<Integer> neighbors = new ArrayList<>();
        for (int i = 0; i < vectors.size(); i++) {
            if (i == idx) continue;
            if (cosineDistance(vectors.get(idx), vectors.get(i)) <= epsilon) {
                neighbors.add(i);
            }
        }
        return neighbors;
    }

    private List<float[]> computeCentroids(List<float[]> vectors, int[] labels, int clusterCount) {
        int dim = vectors.get(0).length;
        List<float[]> centroids = new ArrayList<>();

        for (int c = 0; c < clusterCount; c++) {
            float[] centroid = new float[dim];
            int count = 0;

            for (int i = 0; i < vectors.size(); i++) {
                if (labels[i] == c) {
                    for (int d = 0; d < dim; d++) {
                        centroid[d] += vectors.get(i)[d];
                    }
                    count++;
                }
            }

            if (count > 0) {
                for (int d = 0; d < dim; d++) {
                    centroid[d] /= count;
                }
                centroids.add(centroid);
            }
        }

        return centroids;
    }

    private float cosineDistance(float[] a, float[] b) {
        float dot = 0, normA = 0, normB = 0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        if (normA == 0 || normB == 0) return 1f;
        return 1f - (dot / (float)(Math.sqrt(normA) * Math.sqrt(normB)));
    }
}
