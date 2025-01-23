package com.terryzag.AutoGeoserver.entity;

import lombok.Data;

import java.net.URI;

@Data
public class TiffPublishVo {

    private URI url;   // Geoserver地址
    private String wsName;  // 工作空间名称
    private String layerName;  // 图层名称

}
