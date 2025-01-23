package com.terryzag.AutoGeoserver.entity;

import lombok.Data;

@Data
public class TiffPublish {

    private String wsName;   // 工作空间名称
    private String storeName;  // 数据存储名称
    private String absPath;  // 文件绝对路径

}
