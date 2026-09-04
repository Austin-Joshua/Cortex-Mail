package com.nexora.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

public class CortexScoreResponse {
    /** Null while {@link #ready} is false — never expose a fake numeric score. */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Integer score;
    private String band;
    private List<Factor> factors = new ArrayList<>();
    /** False until Gmail is synced and inbox mail is fully classified. */
    private boolean ready;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String statusMessage;
    /** One concrete next step for the signed-in mailbox — never generic filler. */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String nextAction;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Long inboxUnread;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Long overdueCount;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Long storedCount;

    public CortexScoreResponse() {}

    public static CortexScoreResponseBuilder builder() {
        return new CortexScoreResponseBuilder();
    }

    /** Score not ready — no numeric value is emitted in JSON. */
    public static CortexScoreResponse pending(String band, String statusMessage) {
        return builder()
                .ready(false)
                .band(band)
                .statusMessage(statusMessage)
                .factors(List.of())
                .build();
    }

    public Integer getScore() { return score; }
    public void setScore(Integer score) { this.score = score; }

    public String getBand() { return band; }
    public void setBand(String band) { this.band = band; }

    public List<Factor> getFactors() { return factors; }
    public void setFactors(List<Factor> factors) { this.factors = factors; }

    @JsonProperty("ready")
    public boolean isReady() { return ready; }
    public void setReady(boolean ready) { this.ready = ready; }

    public String getStatusMessage() { return statusMessage; }
    public void setStatusMessage(String statusMessage) { this.statusMessage = statusMessage; }

    public String getNextAction() { return nextAction; }
    public void setNextAction(String nextAction) { this.nextAction = nextAction; }

    public Long getInboxUnread() { return inboxUnread; }
    public void setInboxUnread(Long inboxUnread) { this.inboxUnread = inboxUnread; }

    public Long getOverdueCount() { return overdueCount; }
    public void setOverdueCount(Long overdueCount) { this.overdueCount = overdueCount; }

    public Long getStoredCount() { return storedCount; }
    public void setStoredCount(Long storedCount) { this.storedCount = storedCount; }

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
                Factor factor = new Factor();
                factor.key = key;
                factor.label = label;
                factor.points = points;
                factor.detail = detail;
                return factor;
            }
        }
    }

    public static class CortexScoreResponseBuilder {
        private Integer score;
        private String band;
        private List<Factor> factors = new ArrayList<>();
        private boolean ready = false;
        private String statusMessage;
        private String nextAction;

        public CortexScoreResponseBuilder score(int score) { this.score = score; return this; }
        public CortexScoreResponseBuilder band(String band) { this.band = band; return this; }
        public CortexScoreResponseBuilder factors(List<Factor> factors) { this.factors = factors; return this; }
        public CortexScoreResponseBuilder ready(boolean ready) { this.ready = ready; return this; }
        public CortexScoreResponseBuilder statusMessage(String statusMessage) { this.statusMessage = statusMessage; return this; }
        public CortexScoreResponseBuilder nextAction(String nextAction) { this.nextAction = nextAction; return this; }

        public CortexScoreResponse build() {
            CortexScoreResponse response = new CortexScoreResponse();
            response.score = ready ? score : null;
            response.band = band;
            response.factors = factors != null ? factors : new ArrayList<>();
            response.ready = ready;
            response.statusMessage = statusMessage;
            response.nextAction = ready ? nextAction : null;
            return response;
        }
    }
}
