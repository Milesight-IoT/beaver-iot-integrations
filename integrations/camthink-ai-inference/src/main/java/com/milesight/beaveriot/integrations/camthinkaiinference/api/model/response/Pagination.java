package com.milesight.beaveriot.integrations.camthinkaiinference.api.model.response;

import lombok.Data;

/**
 * author: Luxb
 * create: 2025/6/5 14:21
 **/
@Data
public class Pagination {
    private int currentPage;
    private int pageSize;
    private int totalItems;
    private int totalPages;
}