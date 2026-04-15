package com.kielakjr.movie_picker.movie;

import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class DbscanClusterer {

    private static final float EPSILON = 0.3f;
    private static final int MIN_POINTS = 2;

    public List<float[]> findClusterCentroids(List<float[]> vectors) {
        if (vectors.isEmpty()) return List.of();

        int n = vectors.size();
        int[] labels = new int[n];
        Arrays.fill(labels, -1);

        int clusterId = 0;

        for (int i = 0; i < n; i++) {
            if (labels[i] != -1) continue;

            List<Integer> neighbors = getNeighbors(vectors, i, EPSILON);

            if (neighbors.size() < MIN_POINTS) {
                labels[i] = -2;
                continue;
            }

            labels[i] = clusterId;
            Queue<Integer> queue = new LinkedList<>(neighbors);

            while (!queue.isEmpty()) {
                int j = queue.poll();

                if (labels[j] == -2) {
                    labels[j] = clusterId;
                }
                if (labels[j] != -1) continue;

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

        if (centroids.isEmpty()) {
            centroids.add(computeGlobalMean(vectors));
        }

        return centroids;
    }

    private float[] computeGlobalMean(List<float[]> vectors) {
        int dim = vectors.get(0).length;
        float[] mean = new float[dim];
        for (float[] v : vectors) {
            for (int d = 0; d < dim; d++) {
                mean[d] += v[d];
            }
        }
        for (int d = 0; d < dim; d++) {
            mean[d] /= vectors.size();
        }
        return mean;
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
