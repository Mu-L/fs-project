# Admin based on ant-design-pro-vue

## 安装和运行
- 运行环境
```
nvm install 20.18.0
nvm use 20.18.0
npm install -g yarn
# node v20.18.0
# npm 10.8.2
# yarn 1.22.22
```
- 安装依赖
```
yarn install
# error @achrinza/node-ipc@9.2.2: The engine "node" is incompatible with this module. Expected version "8 || 10 || 12 || 14 || 16 || 17". Got "20.18.0"
# error Found incompatible module.
yarn config set ignore-engines true
```

- 开发模式运行
```
yarn run serve
```

- 编译项目
```
yarn run build
```

- Lints and fixes files
```
yarn run lint
```

- 跨域请求
```
# 将API接口地址设置为本地路径
VUE_APP_API_BASE_URL=./
# 配置代理托管远程接口调用
proxy: {
  '/proxy': {
    target: 'http://ip:port',
    ws: false,
    changeOrigin: true
  }
}
```

## 注意事项

### 表单在1.5.0-rc.3版本正式发布FormModel相关文档，项目统一采用v-model双向绑定方式进行开发。

### 表格的分页暂不支持深度监听，只能重置影响分页数据，[issue](https://github.com/vueComponent/ant-design-vue/issues/70)。

### 排序可通过config接口获取排序项，通过下拉菜单方式进行选择。

### 在ant-design-vue-1.7.3中，若FormItem的isFormItemChildren为true时，wrapperCol不生效。
```
provide: function provide() {
  return {
    isFormItemChildren: true
  };
},
```
因此，当嵌套表单或在a-form-model-item中提供Modal弹窗填写表单，则wrapperCol无法正常工作。此时，可在自己的组件中，提供provide函数，强制将isFormItemChildren覆盖为false即可。

## 最佳实践

### 当有多个属性值被子组件修改时，可采用[.sync](https://cn.vuejs.org/v2/guide/components-custom-events.html#sync-%E4%BF%AE%E9%A5%B0%E7%AC%A6)修饰符。

### 深度嵌套的子组件，访问父组件的部分内容，可采用[Provide / Inject](https://v3.cn.vuejs.org/guide/component-provide-inject.html)方法。

### antdv关闭时销毁Modal里的子元素，设置destroyOnClose=true即可。

### 组件循环引用

- 异步导入（推荐）
```
components: { ComponentName: () => import('path for component') }
```
- 生命周期方法
```
beforeCreate () {
  this.$options.components.ComponentName = require('path for component').default
}
```
### 覆盖基础组件样式
- 避免使用全局样式，防止污染组件外的空间。
- 优先使用组件本身提供的类选择器参数或样式回调函数。
```
如：Table.rowClassName
```
- 使用/deep/或者>>>深度作用选择器，[Scoped CSS](https://vue-loader.vuejs.org/zh/guide/scoped-css.html)。
```
// >>>：只作用于css
// /deep/：可作用于css/less/scss
.fs-demo {
  & /deep/ .ant-card-body {
    padding: 0px;
    height: calc(100% - 64px);
  }
}
```
### 局部页面引入外部CDN资源
```
beforeCreate () {
  Object.assign(this.$options.components, {
    'ui-css': this.$assets.components.css,
    'ui-js': this.$assets.components.js
  })
},
```

## 浏览器兼容

Modern browsers and IE10.

| [<img src="https://raw.githubusercontent.com/alrra/browser-logos/master/src/edge/edge_48x48.png" alt="IE / Edge" width="24px" height="24px" />](http://godban.github.io/browsers-support-badges/)</br>IE / Edge | [<img src="https://raw.githubusercontent.com/alrra/browser-logos/master/src/firefox/firefox_48x48.png" alt="Firefox" width="24px" height="24px" />](http://godban.github.io/browsers-support-badges/)</br>Firefox | [<img src="https://raw.githubusercontent.com/alrra/browser-logos/master/src/chrome/chrome_48x48.png" alt="Chrome" width="24px" height="24px" />](http://godban.github.io/browsers-support-badges/)</br>Chrome | [<img src="https://raw.githubusercontent.com/alrra/browser-logos/master/src/safari/safari_48x48.png" alt="Safari" width="24px" height="24px" />](http://godban.github.io/browsers-support-badges/)</br>Safari | [<img src="https://raw.githubusercontent.com/alrra/browser-logos/master/src/opera/opera_48x48.png" alt="Opera" width="24px" height="24px" />](http://godban.github.io/browsers-support-badges/)</br>Opera |
| --- | --- | --- | --- | --- |
| IE10, Edge | last 2 versions | last 2 versions | last 2 versions | last 2 versions |

## 参考

- [iconfont](https://www.iconfont.cn/)
- [vue-draggable](http://www.itxst.com/vue-draggable/tutorial.html)
- [jsPlumb Toolkit](https://docs.jsplumbtoolkit.com/toolkit/2.x/)
