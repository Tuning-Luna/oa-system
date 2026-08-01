package com.tuning.oasystem.common;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 分页返回对象
 *
 * @param <T> 分页数据类型
 */
@Data
public class PageResult<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 总记录数 */
    private Long total;

    /** 当前页数据 */
    private List<T> records;

    /** 当前页码 */
    private Long pageNum;

    /** 每页条数 */
    private Long pageSize;

    public PageResult() {
    }

    public PageResult(Long total, List<T> records, Long pageNum, Long pageSize) {
        this.total = total;
        this.records = records;
        this.pageNum = pageNum;
        this.pageSize = pageSize;
    }

    public static <T> PageResult<T> of(Long total, List<T> records, Long pageNum, Long pageSize) {
        return new PageResult<>(total, records, pageNum, pageSize);
    }
}
