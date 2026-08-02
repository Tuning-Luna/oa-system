package com.tuning.oasystem.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文件信息实体（file_info）
 */
@Data
@TableName("file_info")
public class FileInfo {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 原始文件名 */
    private String originalName;

    /** 存储文件名（相对路径） */
    private String storeName;

    /** 存储相对路径（磁盘操作用） */
    private String path;

    /** 访问地址 */
    private String url;

    /** 文件大小（字节） */
    private Long size;

    /** MIME 类型 */
    private String contentType;

    /** 上传人ID */
    private Long uploaderId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 逻辑删除：0否 1是 */
    @TableLogic
    private Integer deleted;
}
