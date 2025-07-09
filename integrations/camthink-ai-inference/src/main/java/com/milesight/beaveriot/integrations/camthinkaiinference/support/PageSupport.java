package com.milesight.beaveriot.integrations.camthinkaiinference.support;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.Collections;
import java.util.List;

/**
 * author: Luxb
 * create: 2025/6/20 15:04
 **/
public class PageSupport {
    @SuppressWarnings("unused")
    public static <T> Page<T> fromAllList(List<T> allList, int pageNumber, int pageSize) {
        int total = allList.size();
        int fromIndex = (pageNumber - 1) * pageSize;
        int toIndex = Math.min(fromIndex + pageSize, total);

        if(total == 0 || fromIndex > total) {
            return new PageImpl<>(List.of(), PageRequest.of(pageNumber - 1, pageSize), total);
        }
        List<T> pageContent = allList.subList(fromIndex, toIndex);
        return new PageImpl<>(pageContent, PageRequest.of(pageNumber - 1, pageSize), total);
    }

    public static <T> List<T> toPageList(List<T> allList, int pageNumber, int pageSize) {
        int total = allList.size();
        int fromIndex = (pageNumber - 1) * pageSize;
        int toIndex = Math.min(fromIndex + pageSize, total);

        if(total == 0 || fromIndex > total) {
            return Collections.emptyList();
        }
        return allList.subList(fromIndex, toIndex);
    }

    public static <T> Page<T> toPage(List<T> pageList, int pageNumber, int pageSize, int total) {
        return new PageImpl<>(pageList, PageRequest.of(pageNumber - 1, pageSize), total);
    }
}
