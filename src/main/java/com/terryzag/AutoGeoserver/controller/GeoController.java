package com.terryzag.AutoGeoserver.controller;

import com.terryzag.AutoGeoserver.entity.TiffPublish;
import com.terryzag.AutoGeoserver.entity.TiffPublishVo;
import com.terryzag.AutoGeoserver.service.GeoPubService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.configurationprocessor.json.JSONException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.ObjectUtils;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URISyntaxException;

@RestController
@RequestMapping("/geo")
public class GeoController {

    private static Logger log = LogManager.getLogger(GeoController.class.getName());

    @Autowired
    private GeoPubService geoPubService;

    /**
     * 发布tiff时不上传文件
     * */
    @PostMapping("publishTiffWithAbsPath")
    public ResponseEntity<Object> publishTiffWithAbsPath(@RequestBody TiffPublish tiffPublish) {
        TiffPublishVo resObj = geoPubService.publishTiffWithAbsPath(tiffPublish);
        if(ObjectUtils.isEmpty(resObj)){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("file not exist");
        }
        return ResponseEntity.status(HttpStatus.OK).body(resObj);
    }

//    /**
//     * 发布时上传文件
//     * */
//    @PostMapping("uploadTiffAndPublish")
//    public boolean uploadTiffAndPublish(@RequestBody TiffPublish tiffPublish) throws IOException, URISyntaxException {
//
//        String wsName=tiffPublish.getWsName();
//        String layerPath=tiffPublish.getAbsPath();
//        String storeName=tiffPublish.getStoreName();  // 数据源名称
//
//        String gsImageUser = geoserver.getUsername();
//        String gsImagePwd = geoserver.getPassword();
//        URI gsImageUrl = geoserver.getUrl();
//        String username = gsImageUser;
//        String pw = gsImagePwd;
//
//        // gsImageUrl
//        URI gsImageUrI = new URI(gsImageUrl);
//
//        GeoServerRESTManager geoServerRESTManager = new GeoServerRESTManager(gsImageUrI.toURL(), gsImageUser, gsImagePwd);
//        GeoServerRESTPublisher geoServerRESTPublisher = geoServerRESTManager.getPublisher();
//        GeoServerRESTReader geoServerRESTReader=geoServerRESTManager.getReader();
//
//        // 判断workspace是否存在，不存在则创建
//        List<String> workspaces = geoServerRESTReader.getWorkspaceNames();
//        if (!workspaces.contains(wsName)) {
//            boolean isCreateWs =geoServerRESTPublisher.createWorkspace(wsName);
//            log.info("Create ws : {}" , isCreateWs);
//        } else {
//            log.info("Workspace already exist, workspace :{}" , wsName);
//        }
//
//        RESTCoverageStore restStore = geoServerRESTReader.getCoverageStore(wsName, storeName);
//        if(restStore == null) {
//            GSGeoTIFFDatastoreEncoder gtde = new GSGeoTIFFDatastoreEncoder(storeName);
//            gtde.setWorkspaceName(wsName);
//            boolean createStore = geoServerRESTManager.getStoreManager().create(wsName, gtde);
//            log.info("Create store  : " + createStore);
//        }
//
//        boolean isPublished =geoServerRESTPublisher.publishGeoTIFF(wsName, storeName, new File(layerPath));
//        log.info("publish : " + isPublished);
//        return true;
//    }
}
