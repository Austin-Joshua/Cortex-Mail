package com.nexora.dto.response;

import java.util.ArrayList;
import java.util.List;

public class CortexScoreResponse {
    private int score;
    private String band;
    private List<Factor> factors = new ArrayList<>();

    public CortexScoreResponse() {}

    public static CortexScoreResponseBuilder builder() {
        return new CortexScoreResponseBuilder();
    }

    public int getScore() { return score; }
    public void setScore(int score) { this.score = score; }

    public String getBand() { return band; }
    public void setBand(String band) { this.band = band; }

    public List<Factor> getFactors() { return factors; }
    public void setFactors(List<Factor> factors) { this.factors = factors; }

    public static class Factor {
        private String key;
        private String label;
        private int points;
        private String detail;

        public Factor() {}

        public static FactorBuilder builder() { return new FactorBuilder(); }

        public String getKey() { return key; }
        public void setKey(String key) { this.key = key; }
        public String getLabel() { return label; }
        public void setLabel(String label) { this.label = label; }
        public int getPoints() { return points; }
        public void setPoints(int points) { this.points = points; }
        public String getDetail() { return detail; }
        public void setDetail(String detail) { this.detail = detail; }

        public static class FactorBuilder {
            private String key;
            private String label;
            private int points;
            private String detail;

            public FactorBuilder key(String key) { this.key = key; return this; }
            public FactorBuilder label(String label) { this.label = label; return this; }
            public FactorBuilder points(int points) { this.points = points; return this; }
            public FactorBuilder detail(String detail) { this.detail = detail; return this; }

            public Factor build() {
                Factor f = new Factor();
                f.key = key;
                f.label = label;
                f.points = points;
                f.detail = detail;
                return f;
            }
        }
    }

    public static class CortexScoreResponseBuilder {
        private int score;
        private String band;
        private List<Factor> factors = new ArrayList<>();

        public CortexScoreResponseBuilder score(int score) { this.score = score; return this; }
        public CortexScoreResponseBuilder band(String band) { this.band = band; return this; }
        public CortexScoreResponseBuilder factors(List<Factor> factors) { this.factors = factors; return this; }

        public CortexScoreResponse build() {
            CortexScoreResponse r = new CortexScoreResponse();
            r.score = score;
            r.band = band;
            r.factors = factors != null ? factors : new ArrayList<>();
            return r;
        }
    }
}
