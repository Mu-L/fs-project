import base from './Api'

export default {
  info(id: any, tips = {}) {
    return base.post('/dataQueryLog/info', { id }, tips)
  },
  list(params: any = {}, tips = {}) {
    return base.post('/dataQueryLog/list', params, tips)
  },
  delete(ids: any, tips = {}) {
    return base.post('/dataQueryLog/delete', { ids }, tips)
  },
  config(tips = {}) {
    return base.post('/dataQueryLog/config', {}, tips)
  },
}
