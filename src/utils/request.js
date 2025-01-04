import axios from 'axios'
import SM4Util from './sm4'

// 创建axios实例
const service = axios.create({
  baseURL: '/api', // 基础URL
  timeout: 15000, // 请求超时时间
  headers: {
    'Content-Type': 'application/json'
  }
})

// 请求拦截器
service.interceptors.request.use(
  config => {
    // 加密请求数据
    if (config.data) {
      // 统一将数据转换为JSON字符串再加密
      const jsonStr = typeof config.data === 'string' ? config.data : JSON.stringify(config.data)
      config.data = {data: SM4Util.encrypt(jsonStr)}
    }

    // 如果是GET请求，处理params
    if (config.method === 'get' && config.params) {
      let url = config.url
      url += '?'
      const keys = Object.keys(config.params)
      for (const key of keys) {
        if (config.params[key] !== undefined && config.params[key] !== null) {
          url += `${key}=${encodeURIComponent(config.params[key])}&`
        }
      }
      url = url.substring(0, url.length - 1)
      config.params = {}
      config.url = url
    }
    return config
  },
  error => {
    console.error('Request error:', error)
    return Promise.reject(error)
  }
)

// 响应拦截器
service.interceptors.response.use(
  response => {
    const res = response.data
    // 解密响应数据
    if (res.data) {
      res.data = SM4Util.decrypt(res.data)
    }
    if (res.code !== 200) {
      console.error('Response error:', res)
      return Promise.reject(new Error(res.message || 'Error'))
    }
    return res
  },
  error => {
    console.error('Response error:', error)
    return Promise.reject(error)
  }
)

// 封装请求方法
const request = (options) => {
  if (typeof options === 'string') {
    options = {
      url: options
    }
  }
  
  // 设置默认method为get
  options.method = options.method || 'get'
  
  // 处理GET请求的参数
  if (options.method.toLowerCase() === 'get') {
    options.params = options.data || options.params
  }

  // 添加 signal 到请求配置
  if (options.signal) {
    options.signal = options.signal
  }

  return service(options)
}

export default request 