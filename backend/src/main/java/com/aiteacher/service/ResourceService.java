package com.aiteacher.service;

import com.aiteacher.entity.Resource;
import com.aiteacher.mapper.ResourceMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class ResourceService {

    @Autowired
    private ResourceMapper resourceMapper;

    @Autowired
    private FileStorageService fileStorageService;

    public Resource upload(MultipartFile file, Long workspaceId, Long tenantId) {
        try {
            // Determine resource type
            String resourceType = determineResourceType(file.getContentType());
            
            // Upload to file storage
            String objectName = resourceType + "/" + UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
            fileStorageService.uploadFile(file, resourceType);
            String fileUrl = fileStorageService.getFileUrl(objectName);
            
            // Save resource record
            Resource resource = new Resource();
            resource.setTenantId(tenantId);
            resource.setWorkspaceId(workspaceId);
            resource.setResourceType(resourceType);
            resource.setName(file.getOriginalFilename());
            resource.setUrl(fileUrl);
            resource.setFileSize(file.getSize());
            resource.setMimeType(file.getContentType());
            resource.setCreatedAt(LocalDateTime.now());
            resource.setDeleted(false);
            
            resourceMapper.insert(resource);
            return resource;
            
        } catch (Exception e) {
            log.error("Resource upload failed: {}", e.getMessage(), e);
            throw new RuntimeException("资源上传失败: " + e.getMessage(), e);
        }
    }

    public Page<Resource> page(Page<Resource> page, LambdaQueryWrapper<Resource> wrapper) {
        // Bypass PaginationInnerInterceptor - use manual LIMIT/OFFSET via full load + slice
        List<Resource> records = resourceMapper.selectList(wrapper);
        int total = records.size();
        int fromIndex = (int) ((page.getCurrent() - 1) * page.getSize());
        int toIndex = (int) Math.min(fromIndex + page.getSize(), total);
        page.setTotal(total);
        page.setRecords(fromIndex < total ? records.subList(fromIndex, toIndex) : List.of());
        return page;
    }

    public Resource getById(Long id) {
        return resourceMapper.selectById(id);
    }

    public List<Resource> list(LambdaQueryWrapper<Resource> wrapper) {
        return resourceMapper.selectList(wrapper);
    }

    public boolean delete(Long id) {
        Resource resource = new Resource();
        resource.setId(id);
        resource.setDeleted(true);
        return resourceMapper.updateById(resource) > 0;
    }

    private String determineResourceType(String mimeType) {
        if (mimeType == null) return "document";
        if (mimeType.startsWith("image/")) return "image";
        if (mimeType.startsWith("video/")) return "video";
        if (mimeType.startsWith("audio/")) return "audio";
        return "document";
    }
}