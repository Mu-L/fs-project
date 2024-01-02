const FIELD_CODE = 'code'
const FIELD_MSG = 'message'
const FIELD_DATA = 'data'

const ApiUtil = {
  FIELD_CODE,
  FIELD_MSG,
  FIELD_DATA,
  result (code = 0, message = null, data = null) {
    if (message === null) {
      switch (code) {
        case 0:
            message = '操作成功'
            break
        case 403:
        case 9403:
            message = '禁止访问'
            break
        case 404:
        case 9404:
            message = '信息不存在'
            break
        case 500:
        case 9500:
            message = '操作失败'
            break
      }
    }
    return { [FIELD_CODE]: code, [FIELD_MSG]: message, [FIELD_DATA]: data }
  },
  failed (result) {
    if (result === null) return false
    return result instanceof Object ? result[FIELD_CODE] !== 0 : true
  },
  succeed (result) {
    return !this.failed(result)
  },
  code (result) {
    return result[FIELD_CODE]
  },
  message (result) {
    return result[FIELD_MSG]
  }
}

export default ApiUtil
