import api from '@/core/Api'

export default {
  get (url: string, data = {}, tips = {}, config = {}) {
    return api.post('/proxy/get', {
      app: 'OA',
      uri: url,
      data: data
    }, tips, config)
  },
  post (url: string, data = {}, tips = {}, config = {}) {
    return api.post('/proxy/post', {
      app: 'OA',
      uri: url,
      data: data
    }, tips, config)
  }
}
