import api from '@/core/Api'

export default {
  login (data = {}, tips = {}, config = {}) {
    return api.post('/proxy/postResponse', {
      app: 'Member',
      uri: '/user/login',
      data: Object.assign({}, data, { module: 'admin' })
    }, tips, config)
  },
  logout (data = {}, tips = {}, config = {}) {
    return api.post('/proxy/postResponse', {
      app: 'Member',
      uri: '/user/logout',
      data: Object.assign({}, data, { module: 'admin' })
    }, tips, config)
  }
}
