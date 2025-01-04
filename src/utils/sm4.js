import { sm4 } from 'sm-crypto'

class SM4Util {
  // 密钥必须是16字节的十六进制字符串
  static KEY = '31323334353637383930414243444546' // 16字节密钥的十六进制表示

  // 加密
  static encrypt(data) {
    try {
      if (!data) return data
      const dataStr = typeof data === 'object' ? JSON.stringify(data) : String(data)
      // 使用ECB模式加密
      return sm4.encrypt(dataStr, this.KEY)
    } catch (error) {
      console.error('SM4 encrypt error:', error)
      return data
    }
  }

  // 解密
  static decrypt(data) {
    try {
      if (!data) return data
      const decrypted = sm4.decrypt(data, this.KEY)
      try {
        return JSON.parse(decrypted)
      } catch {
        return decrypted
      }
    } catch (error) {
      console.error('SM4 decrypt error:', error)
      return data
    }
  }
}

export default SM4Util 