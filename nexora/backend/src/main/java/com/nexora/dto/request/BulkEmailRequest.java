package com.nexora.dto.request;

import java.util.ArrayList;
import java.util.List;

public class BulkEmailRequest {
    private List<Long> ids = new ArrayList<>();
    private String action;

    public List<Long> getIds() {
        return ids;
    }

    public void setIds(List<Long> ids) {
        this.ids = ids != null ? ids : new ArrayList<>();
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }
}
