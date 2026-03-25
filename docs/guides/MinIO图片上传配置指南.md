# MinIO 图片上传配置指南

## 📦 MinIO 部署

### 方式1：Docker部署（推荐）

```bash
# 拉取MinIO镜像
docker pull minio/minio

# 启动MinIO容器
docker run -d \
  -p 9000:9000 \
  -p 9001:9001 \
  --name minio \
  -e "MINIO_ROOT_USER=admin" \
  -e "MINIO_ROOT_PASSWORD=admin123456" \
  -v /data/minio:/data \
  minio/minio server /data --console-address ":9001"
```

**访问地址**:
- API端口: http://localhost:9000
- 控制台: http://localhost:9001
- 用户名: admin
- 密码: admin123456

### 方式2：Windows本地部署

1. **下载MinIO**
   ```bash
   # 下载地址
   https://dl.min.io/server/minio/release/windows-amd64/minio.exe
   ```

2. **启动MinIO**
   ```bash
   # 设置环境变量
   set MINIO_ROOT_USER=admin
   set MINIO_ROOT_PASSWORD=admin123456
   
   # 启动服务
   minio.exe server D:\minio-data --console-address ":9001"
   ```

3. **访问控制台**
   - 打开浏览器访问: http://localhost:9001
   - 使用用户名密码登录

---

## ⚙️ 后端配置

### 1. 添加Maven依赖

在 `camera-rental-common/pom.xml` 中添加：

```xml
<!-- MinIO -->
<dependency>
    <groupId>io.minio</groupId>
    <artifactId>minio</artifactId>
    <version>8.5.7</version>
</dependency>
```

### 2. 配置文件

在各个服务的 `application.yml` 中添加MinIO配置：

**推荐：在Nacos配置中心添加公共配置**

创建配置文件：`camera-rental-common.yml`

```yaml
# MinIO配置
minio:
  endpoint: http://localhost:9000
  access-key: admin
  secret-key: admin123456
  bucket-name: camera-rental
```

**或者在各服务的application.yml中添加**:

```yaml
# MinIO配置
minio:
  endpoint: http://localhost:9000
  access-key: admin
  secret-key: admin123456
  bucket-name: camera-rental
```

### 3. 创建存储桶（Bucket）

MinIO启动后，需要创建存储桶：

**方式1：通过控制台创建**
1. 访问 http://localhost:9001
2. 登录后点击 "Buckets"
3. 点击 "Create Bucket"
4. 输入名称：`camera-rental`
5. 点击 "Create"
6. 设置访问策略为 "Public"（或通过代码自动创建）

**方式2：代码自动创建**
已在 `MinioService.uploadFile()` 方法中实现，首次上传时会自动创建桶。

---

## 🔧 配置说明

### 配置项说明

| 配置项 | 说明 | 示例值 |
|-------|------|--------|
| minio.endpoint | MinIO服务地址 | http://localhost:9000 |
| minio.access-key | 访问密钥 | admin |
| minio.secret-key | 密钥 | admin123456 |
| minio.bucket-name | 存储桶名称 | camera-rental |

### 生产环境建议

```yaml
# 生产环境配置
minio:
  endpoint: https://minio.yourdomain.com
  access-key: ${MINIO_ACCESS_KEY}  # 从环境变量读取
  secret-key: ${MINIO_SECRET_KEY}  # 从环境变量读取
  bucket-name: camera-rental-prod
```

---

## 📝 已实现的功能

### 后端实现

1. **MinioConfig.java** ✅
   - MinIO客户端配置
   - 自动注入MinioClient Bean

2. **MinioService.java** ✅
   - `uploadFile()` - 上传文件
   - `getFileUrl()` - 获取文件URL（7天有效期）
   - `deleteFile()` - 删除文件
   - 自动创建桶
   - 文件类型验证

3. **UploadController.java** ✅
   - `POST /upload/image` - 上传单张图片
   - `POST /upload/images` - 批量上传图片
   - 文件大小限制（2MB）
   - 文件类型验证
   - 数量限制（最多5张）

### 前端实现

1. **upload.js** ✅
   - `uploadImage()` - 上传单张
   - `uploadImages()` - 批量上传

2. **ImageUpload.vue** ✅
   - 通用图片上传组件
   - 拖拽上传
   - 图片预览
   - 删除图片
   - 上传进度显示
   - 文件大小和类型验证

---

## 🚀 使用方法

### 前端使用示例

```vue
<template>
  <div>
    <h3>上传图片</h3>
    <ImageUpload 
      v-model="imageList" 
      :max-count="5" 
      :max-size="2"
    />
  </div>
</template>

<script setup>
import { ref } from 'vue'
import ImageUpload from '@/components/ImageUpload.vue'

const imageList = ref([])
</script>
```

### 在设备发布页面集成

在 `DevicePublish.vue` 中使用：

```vue
<el-form-item label="设备图片" prop="imagePaths">
  <ImageUpload 
    v-model="form.imagePaths" 
    :max-count="5" 
    :max-size="2"
  />
</el-form-item>
```

### 在个人资料页面上传头像

```vue
<el-form-item label="头像">
  <ImageUpload 
    v-model="avatarList" 
    :max-count="1" 
    :max-size="2"
  />
</el-form-item>
```

---

## 🧪 测试步骤

### 1. 启动MinIO

```bash
docker start minio
# 或
minio.exe server D:\minio-data --console-address ":9001"
```

### 2. 验证MinIO服务

访问 http://localhost:9001，确认可以登录。

### 3. 配置后端

在 `application.yml` 或 Nacos 配置中心添加MinIO配置。

### 4. 启动服务

启动 `camera-rental-user-service` 服务。

### 5. 测试上传

使用Postman或前端页面测试上传：

```bash
# Postman测试
POST http://localhost:8080/upload/image
Content-Type: multipart/form-data
Body: form-data
  file: [选择图片文件]
```

### 6. 验证结果

- 检查MinIO控制台，确认文件已上传
- 访问返回的URL，确认图片可访问

---

## 🔒 安全建议

### 开发环境
- 使用简单的用户名密码（admin/admin123456）
- 桶设置为public（方便测试）

### 生产环境
- ✅ 使用强密码
- ✅ 启用HTTPS
- ✅ 配置访问策略（只允许特定IP）
- ✅ 定期备份数据
- ✅ 使用环境变量存储密钥
- ✅ 设置文件访问权限

---

## 📦 文件结构

```
camera-rental/
├── service/
│   ├── camera-rental-common/
│   │   └── src/main/java/com/example/camerarental/common/
│   │       ├── config/
│   │       │   └── MinioConfig.java          ✅ MinIO配置
│   │       └── service/
│   │           └── MinioService.java         ✅ MinIO服务
│   └── camera-rental-user-service/
│       └── src/main/java/com/example/camerarental/user/
│           └── controller/
│               └── UploadController.java     ✅ 上传接口
└── frontend/
    └── src/
        ├── api/
        │   └── upload.js                     ✅ 上传API
        └── components/
            └── ImageUpload.vue               ✅ 上传组件
```

---

## 🎯 集成位置

### 需要图片上传的地方

1. **设备发布** - 设备图片（最多5张）
2. **设备编辑** - 修改设备图片
3. **个人资料** - 上传头像（1张）
4. **订单评价** - 评价图片（最多3张）

---

## ❗ 常见问题

### 1. 连接MinIO失败
**问题**: `Connection refused`  
**解决**: 
- 检查MinIO是否启动
- 检查端口是否被占用
- 检查防火墙设置

### 2. 文件上传成功但访问失败
**问题**: 返回URL无法访问  
**解决**:
- 检查桶的访问策略
- 确认URL有效期未过期
- 检查MinIO服务是否正常

### 3. 跨域问题
**问题**: 前端请求MinIO时出现CORS错误  
**解决**:
- MinIO默认允许跨域
- 如需配置，在启动时添加环境变量

### 4. 文件大小限制
**问题**: 上传大文件失败  
**解决**:
- 前端限制：修改 `maxSize` prop
- 后端限制：修改 `UploadController` 中的验证逻辑
- Spring Boot配置：
  ```yaml
  spring:
    servlet:
      multipart:
        max-file-size: 10MB
        max-request-size: 50MB
  ```

---

## 📊 功能完成度

| 功能 | 状态 |
|-----|------|
| MinIO配置 | ✅ 完成 |
| 上传服务 | ✅ 完成 |
| 上传接口 | ✅ 完成 |
| 前端组件 | ✅ 完成 |
| 文件验证 | ✅ 完成 |
| 进度显示 | ✅ 完成 |
| 图片预览 | ✅ 完成 |
| 删除功能 | ✅ 完成 |

---

## 🎉 总结

图片上传功能已完整实现！

**你需要做的**:
1. ✅ 部署MinIO服务（Docker或本地）
2. ✅ 在配置文件中添加MinIO配置
3. ✅ 在pom.xml中添加MinIO依赖
4. ✅ 启动服务测试上传功能

**已为你实现的**:
- ✅ 完整的后端上传服务
- ✅ 完整的前端上传组件
- ✅ 文件验证和错误处理
- ✅ 美观的上传界面
- ✅ 图片预览和删除功能

---

**最后更新**: 2026-01-31  
**状态**: ✅ 已完成，待配置MinIO

