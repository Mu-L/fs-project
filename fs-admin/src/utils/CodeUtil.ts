import { Base64 } from 'js-base64'

const CodeUtil = {
  encodeBase64 (str: any) {
    return Base64.encode(str)
  },
  decodeBase64 (str: any) {
    return Base64.decode(str)
  }
}

export default CodeUtil
