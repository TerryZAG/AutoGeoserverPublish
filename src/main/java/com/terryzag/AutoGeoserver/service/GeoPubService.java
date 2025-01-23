package com.terryzag.AutoGeoserver.service;

import com.terryzag.AutoGeoserver.entity.TiffPublish;
import com.terryzag.AutoGeoserver.entity.TiffPublishVo;
import org.geotools.api.data.DataSourceException;
import org.springframework.boot.configurationprocessor.json.JSONException;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;

@Service
public interface GeoPubService {

    /**
     * 不上传文件发布tiff
     * */
    TiffPublishVo publishTiffWithAbsPath (TiffPublish tiffPublish);
}
