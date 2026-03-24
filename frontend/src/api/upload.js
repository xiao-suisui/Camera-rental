import request from '@/utils/request'

export const uploadApi = {
  // 上传单个图片
  uploadImage: (file) => {
    const formData = new FormData()
    formData.append('file', file)
    return request.post('/device/upload/image', formData, {
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    })
  },

  // 批量上传图片
  uploadImages: (files) => {
    const formData = new FormData()
    files.forEach(file => {
      formData.append('files', file)
    })
    return request.post('/device/upload/images', formData, {
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    })
  }
}

