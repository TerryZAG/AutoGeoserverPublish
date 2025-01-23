package com.terryzag.AutoGeoserver.service.impl;

import com.terryzag.AutoGeoserver.configurations.Geoserver;
import com.terryzag.AutoGeoserver.entity.TiffPublish;
import com.terryzag.AutoGeoserver.entity.TiffPublishVo;
import com.terryzag.AutoGeoserver.service.GeoPubService;
import it.geosolutions.geoserver.rest.GeoServerRESTManager;
import it.geosolutions.geoserver.rest.GeoServerRESTPublisher;
import it.geosolutions.geoserver.rest.GeoServerRESTReader;
import it.geosolutions.geoserver.rest.HTTPUtils;
import it.geosolutions.geoserver.rest.decoder.RESTCoverageStore;
import it.geosolutions.geoserver.rest.encoder.datastore.GSGeoTIFFDatastoreEncoder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.httpclient.methods.RequestEntity;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.geotools.api.data.DataSourceException;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.gce.geotiff.GeoTiffReader;
import org.geotools.referencing.CRS;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.configurationprocessor.json.JSONArray;
import org.springframework.boot.configurationprocessor.json.JSONException;
import org.springframework.boot.configurationprocessor.json.JSONObject;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Slf4j
@Component
public class GeoPubServiceImpl implements GeoPubService {

    @Autowired
    private Geoserver geoserver;

    @Override
    public TiffPublishVo publishTiffWithAbsPath(TiffPublish tiffPublish){
        // geoserver配置
        String username = geoserver.getUsername();
        String password = geoserver.getPassword();
        URI geoserverURI = geoserver.getUrl();
        // 检查文件是否存在
        String absolutePath = tiffPublish.getAbsPath();
        Path path = Paths.get(absolutePath);
        String fileName = path.getFileName().toString();
        boolean fileExists = Files.exists(path);
        if (!fileExists) {
            log.info("file not exist, path : " + absolutePath);
            return null;
        }

        // 工作空间名称
        String wsName=tiffPublish.getWsName();
        // 数据源名称
        String storeName=tiffPublish.getStoreName();

        try{
            // 默认坐标系
            String srs = "EPSG:4326";
            // 读取TIFF文件
            GeoTiffReader reader = new GeoTiffReader(new File(absolutePath));
//        Format format = reader.getFormat(); // 读取第一幅图像
            // 获取坐标参考系统
            CoordinateReferenceSystem crs = reader.getCoordinateReferenceSystem();
            srs = CRS.toSRS(crs);
            log.info("srs : " + srs);

            // 构建GeoServerRESTManager请求
            GeoServerRESTManager geoServerRESTManager = new GeoServerRESTManager(geoserverURI.toURL(), username, password);
            GeoServerRESTPublisher geoServerRESTPublisher = geoServerRESTManager.getPublisher();
            GeoServerRESTReader geoServerRESTReader=geoServerRESTManager.getReader();

            // 判断workspace是否存在，不存在则创建
            List<String> workspaces = geoServerRESTReader.getWorkspaceNames();
            if (!workspaces.contains(wsName)) {
                boolean isCreateWs =geoServerRESTPublisher.createWorkspace(wsName);
                log.info("Create ws : {}" , isCreateWs);
            } else {
                log.info("Workspace already exist, workspace :{}" , wsName);
            }

            // 判断store是否存在，不存在则创建
//        RESTDataStore restDataStore = geoServerRESTReader.getDatastore(wsName, storeName);
            RESTCoverageStore restDataStore = geoServerRESTReader.getCoverageStore(wsName, storeName);
            if(restDataStore == null) {
                // 图层文件地址
                URI layerAbsPathUrI = new URI("file:"+absolutePath);
                GSGeoTIFFDatastoreEncoder gtde = new GSGeoTIFFDatastoreEncoder(storeName);
                gtde.setWorkspaceName(wsName);
                gtde.setUrl(layerAbsPathUrI.toURL());
                boolean createStore = geoServerRESTManager.getStoreManager().create(wsName, gtde);
                log.info("Create store : " + createStore);
            }else{
                String restDataStoreURL = restDataStore.getURL();
                if(!("file:"+absolutePath).equalsIgnoreCase(restDataStoreURL)){
                    // 文件位置不对则修改数据源
                    URI layerPathUrI = new URI("file:"+absolutePath);
                    GSGeoTIFFDatastoreEncoder gtde = new GSGeoTIFFDatastoreEncoder(storeName);
                    gtde.setWorkspaceName(wsName);
                    gtde.setUrl(layerPathUrI.toURL());
                    boolean createStore = geoServerRESTManager.getStoreManager().update(wsName, gtde);
                    log.info("Update store : " + createStore);
                }
            }

            // 发布图层
            String putCoverages = "rest/workspaces/"+wsName+"/coveragestores/"+storeName+"/coverages";
            // 构建图层数据
            JSONObject root = buildGeoJson(storeName, wsName, fileName, geoserverURI, srs);
            // 发送发布请求
            RequestEntity requestEntity = new StringRequestEntity(root.toString(),"application/json","utf-8");
            String res = HTTPUtils.post(geoserverURI + putCoverages, requestEntity, username, password);
//        RESTDataStore restStore = RESTDataStore.build(response);
            log.info("publish tiff : " + res);
        }catch (Exception e){
            log.error("publish tiff error : " + e.getMessage());
            return null;
        }

        // 构建返回数据
        TiffPublishVo resObj = new TiffPublishVo();
        resObj.setUrl(geoserverURI);
        resObj.setWsName(wsName);
        resObj.setLayerName(wsName+":"+storeName);
        return resObj;
    }

    /**
     * 构建图层数据
     * @param storeName 数据源名称
     * @param wsName 工作空间名称
     * @param fileName 源文件名
     * @param geoserverURI geoserver地址
     * @param srs 坐标系
     * @return 图层数据JsonObject
     * */
    private JSONObject buildGeoJson(String storeName, String wsName, String fileName, URI geoserverURI, String srs) throws JSONException {
        // 构建图层数据
        JSONObject root = new JSONObject();
        JSONObject content = new JSONObject();
        content.put("name", storeName);  // 数据源名称
        content.put("nativeName", wsName+":"+storeName);  // 源文件名，图层名称
        int dotIndex = fileName.lastIndexOf('.');
        String fileNameWithoutExtension = fileName.substring(0, dotIndex);
        content.put("nativeCoverageName", fileNameWithoutExtension);  // 源文件名
        content.put("enabled",true);
        // 命名空间
        JSONObject namespace = new JSONObject();
        namespace.put("name", wsName);
        namespace.put("href", geoserverURI+"rest/namespaces/"+wsName+".json");
        content.put("namespace", namespace);
        content.put("title", wsName+":"+storeName);  // 标题
        content.put("defaultInterpolationMethod", "nearest neighbor");  //默认插值法
        // 关键字
        JSONObject keywords = new JSONObject();
        JSONArray keywordsArray = new JSONArray();
        keywordsArray.put("WCS");
        keywordsArray.put("GeoTIFF");
        keywords.put("string", keywordsArray);
        content.put("keywords", keywords);
        content.put("srs", srs);  // 坐标系
//        // 经纬度范围
//        JSONObject latLonBoundingBox = new JSONObject();
//        latLonBoundingBox.put("crs", "EPSG:4326");
//        latLonBoundingBox.put("maxx", "EPSG:4326");
//        latLonBoundingBox.put("maxy", "EPSG:4326");
//        latLonBoundingBox.put("minx", "EPSG:4326");
//        latLonBoundingBox.put("miny", "EPSG:4326");
//        content.put("latLonBoundingBox", latLonBoundingBox);
        // 数据源
        JSONObject store = new JSONObject();
        store.put("@class", "coverageStore");
        store.put("href", geoserverURI + "rest/workspaces/" + wsName + "/coveragestores/" + storeName + ".json");
        store.put("name", wsName+":"+storeName);
        content.put("store", store);
        // responseSRS
        JSONObject responseSRS = new JSONObject();
        JSONArray responseSRSArr = new JSONArray();
        responseSRSArr.put(srs);
        responseSRS.put("string", responseSRSArr);
        content.put("responseSRS", responseSRS);
        // root
        root.put("coverage", content);
        return root;
    }
}