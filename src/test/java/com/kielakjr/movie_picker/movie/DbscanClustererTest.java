package com.kielakjr.movie_picker.movie;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.Offset.offset;

class DbscanClustererTest {

    private final DbscanClusterer clusterer = new DbscanClusterer();

    @Nested
    class FindClusterCentroids {

        @Test
        void emptyInput_returnsEmptyList() {
            List<float[]> result = clusterer.findClusterCentroids(List.of());

            assertThat(result).isEmpty();
        }

        @Test
        void singleVector_noClusterFormed_returnsEmptyList() {
            float[] v = {1f, 0f, 0f};

            List<float[]> centroids = clusterer.findClusterCentroids(List.of(v));

            assertThat(centroids).isEmpty();
        }

        @Test
        void twoCloseVectors_withMinPointsOne_formSingleCluster() {
            float[] v1 = {1f, 0f, 0f};
            float[] v2 = {2f, 0f, 0f};

            List<float[]> centroids = clusterer.findClusterCentroids(List.of(v1, v2));

            assertThat(centroids).hasSize(1);
        }

        @Test
        void threeCollinearVectors_formSingleCluster_centroidIsComponentMean() {
            float[] v1 = {1f, 0f, 0f};
            float[] v2 = {2f, 0f, 0f};
            float[] v3 = {3f, 0f, 0f};

            List<float[]> centroids = clusterer.findClusterCentroids(List.of(v1, v2, v3));

            assertThat(centroids).hasSize(1);
            assertThat(centroids.get(0)[0]).isCloseTo(2.0f, offset(0.001f));
            assertThat(centroids.get(0)[1]).isCloseTo(0f, offset(0.001f));
        }

        @Test
        void twoClustersInOrthogonalDirections_returnsTwoCentroids() {
            float[] a1 = {1f, 0f, 0f};
            float[] a2 = {2f, 0f, 0f};
            float[] a3 = {3f, 0f, 0f};
            float[] b1 = {0f, 1f, 0f};
            float[] b2 = {0f, 2f, 0f};
            float[] b3 = {0f, 3f, 0f};

            List<float[]> centroids = clusterer.findClusterCentroids(List.of(a1, a2, a3, b1, b2, b3));

            assertThat(centroids).hasSize(2);

            boolean clusterAFound = centroids.stream().anyMatch(c ->
                    Math.abs(c[0] - 2.0f) < 0.001f && Math.abs(c[1]) < 0.001f);
            boolean clusterBFound = centroids.stream().anyMatch(c ->
                    Math.abs(c[0]) < 0.001f && Math.abs(c[1] - 2.0f) < 0.001f);

            assertThat(clusterAFound).as("cluster A centroid [2,0,0]").isTrue();
            assertThat(clusterBFound).as("cluster B centroid [0,2,0]").isTrue();
        }

        @Test
        void vectorsJustWithinEpsilon_treatedAsNeighbors() {
            float[] v1 = {1f, 1f, 0f};
            float[] v2 = {1f, 0f, 0f};
            float[] v3 = {2f, 0f, 0f};

            List<float[]> centroids = clusterer.findClusterCentroids(List.of(v1, v2, v3));

            assertThat(centroids).hasSize(1);
        }

        @Test
        void zeroVector_neverWithinEpsilon_treatedAsNoise() {
            float[] zero = {0f, 0f, 0f};
            float[] v1   = {1f, 0f, 0f};
            float[] v2   = {2f, 0f, 0f};
            float[] v3   = {3f, 0f, 0f};

            List<float[]> centroids = clusterer.findClusterCentroids(List.of(zero, v1, v2, v3));

            assertThat(centroids).hasSize(1);
            assertThat(centroids.get(0)[0]).isCloseTo(2.0f, offset(0.001f));
        }

        @Test
        void vectorDimensionIsPreservedInCentroid() {
            float[] v1 = {1f, 0f};
            float[] v2 = {2f, 0f};
            float[] v3 = {3f, 0f};

            List<float[]> centroids = clusterer.findClusterCentroids(List.of(v1, v2, v3));

            assertThat(centroids.get(0)).hasSize(2);
        }
    }
}
