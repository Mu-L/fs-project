<template>
  <section>
    <a-card>
      <template slot="title">
        <a-button @click="toggleRowKeys" :icon="expandedRowKeys.length === 0 ? 'menu-unfold' : 'menu-fold'"></a-button>
        <a-divider type="vertical" v-permit="'member:resource:delete'" />
        <a-button icon="minus-circle" type="danger" @click="batchRemove" v-permit="'member:resource:delete'" :disabled="selection.selectedRows.length === 0">删除</a-button>
        <a-divider type="vertical" v-permit="'member:resource:add'" />
        <a-button icon="plus-circle" type="primary" @click="add(0)" v-permit="'member:resource:add'">新增</a-button>
      </template>
      <template slot="extra">
        <a-button icon="reload" @click="reload">刷新</a-button>
        <a-divider type="vertical" />
        <a-button @click.native="$router.go(-1)">返回</a-button>
      </template>
      <a-descriptions title="应用信息">
        <a-descriptions-item label="ID">{{ info.id }}</a-descriptions-item>
        <a-descriptions-item label="标识">{{ info.serial }}</a-descriptions-item>
        <a-descriptions-item label="名称">{{ info.name }}</a-descriptions-item>
      </a-descriptions>
    </a-card>
    <a-card :bordered="false">
      <div class="table-page-search-wrapper">
        <a-table
          :columns="columns"
          :rowKey="record => record.id"
          :dataSource="rows"
          :pagination="false"
          :loading="loading"
          :rowSelection="selection"
          :bordered="true"
          :expandedRowKeys="expandedRowKeys"
          @expandedRowsChange="(expandedRows) => this.expandedRowKeys = expandedRows"
          :scroll="{ x: true }"
        >
          <span slot="name" slot-scope="text, record">{{ record.name }}/{{ record.parentId }}/{{ record.id }}</span>
          <span slot="permit" slot-scope="text, record">{{ record.module }}:{{ record.controller }}:{{ record.action }}</span>
          <span slot="action" slot-scope="text, record">
            <a-dropdown>
              <a-menu slot="overlay">
                <a-menu-item v-permit="'member:resource:'"><a-button :block="true" type="link" size="small" @click="show(text, record)">查看</a-button></a-menu-item>
                <a-menu-item v-permit="'member:resource:modify'"><a-button :block="true" type="link" size="small" @click="edit(text, record)">编辑</a-button></a-menu-item>
                <a-menu-item v-permit="'member:resource:add'"><a-button :block="true" type="link" size="small" @click="sublevel(text, record)">子级</a-button></a-menu-item>
              </a-menu>
              <a-button type="link" icon="tool"></a-button>
            </a-dropdown>
          </span>
        </a-table>
      </div>
    </a-card>
    <!--展示界面-->
    <a-modal :title="'信息查看 - ' + form.id" v-model="infoVisible" :footer="null">
      <a-form-model :model="form" :loading="infoLoading" :label-col="{ span: 5 }" :wrapper-col="{ span: 18 }">
        <a-form-model-item label="父级">{{ form.parentId }}</a-form-model-item>
        <a-form-model-item label="名称">{{ form.name }}</a-form-model-item>
        <a-form-model-item label="全称">{{ form.fullName }}</a-form-model-item>
        <a-form-model-item label="模块">{{ form.module }}</a-form-model-item>
        <a-form-model-item label="控制器">{{ form.controller }}</a-form-model-item>
        <a-form-model-item label="动作">{{ form.action }}</a-form-model-item>
        <a-form-model-item label="排序">{{ form.sort }}</a-form-model-item>
        <a-form-model-item label="状态">{{ form.statusText }}</a-form-model-item>
        <a-form-model-item label="描述">{{ form.description }}</a-form-model-item>
        <a-form-model-item label="创建者">{{ form.createdUserInfo?.name }}</a-form-model-item>
        <a-form-model-item label="创建时间">{{ form.createdTime|date }}</a-form-model-item>
        <a-form-model-item label="修改者">{{ form.updatedUserInfo?.name }}</a-form-model-item>
        <a-form-model-item label="修改时间">{{ form.updatedTime|date }}</a-form-model-item>
      </a-form-model>
    </a-modal>
    <!--编辑界面-->
    <a-modal :title="'信息' + (form.id ? ('修改 - ' + form.id) : '添加')" v-model="formVisible" :confirmLoading="formLoading" :maskClosable="false" @ok="submit">
      <a-form-model ref="form" :model="form" :rules="rules" :label-col="{ span: 5 }" :wrapper-col="{ span: 18 }">
        <a-form-model-item label="ID" prop="id">
          <a-input v-model="form.id" auto-complete="off"></a-input>
        </a-form-model-item>
        <a-form-model-item label="应用" prop="applicationId">
          <a-input v-model="form.applicationId" auto-complete="off"></a-input>
        </a-form-model-item>
        <a-form-model-item label="父级" prop="parentId">
          <a-input v-model="form.parentId" auto-complete="off"></a-input>
        </a-form-model-item>
        <a-form-model-item label="名称" prop="name">
          <a-input v-model="form.name" auto-complete="off"></a-input>
        </a-form-model-item>
        <a-form-model-item label="模块" prop="module">
          <a-input v-model="form.module" auto-complete="on"></a-input>
        </a-form-model-item>
        <a-form-model-item label="控制器" prop="controller">
          <a-input v-model="form.controller" auto-complete="on"></a-input>
        </a-form-model-item>
        <a-form-model-item label="动作" prop="action">
          <a-input v-model="form.action" auto-complete="on"></a-input>
        </a-form-model-item>
        <a-form-model-item label="排序">
          <a-input-number v-model="form.sort" :min="0" :max="200"></a-input-number>
        </a-form-model-item>
        <a-form-model-item label="状态" prop="status">
          <a-select v-model="form.status" placeholder="请选择">
            <a-select-option v-for="(value, key) in config.status" :key="key" :value="key">{{ value }}</a-select-option>
          </a-select>
        </a-form-model-item>
        <a-form-model-item label="描述">
          <a-textarea v-model="form.description" />
        </a-form-model-item>
      </a-form-model>
    </a-modal>
  </section>
</template>

<script>
import RouteUtil from '@/utils/route'
import resourceService from '@/service/member/resource'
import applicationService from '@/service/member/application'
import ApiUtil from '@/utils/api'

export default {
  data () {
    return {
      columns: [
        { title: '名称', dataIndex: 'name', scopedSlots: { customRender: 'name' } },
        { title: '全称', dataIndex: 'fullName' },
        { title: '资源', scopedSlots: { customRender: 'permit' } },
        { title: '排序', dataIndex: 'sort' },
        { title: '状态', dataIndex: 'statusText' },
        { title: '操作', scopedSlots: { customRender: 'action' } }
      ],
      toggle: false,
      expandedRowKeys: [],
      selection: RouteUtil.selection(),
      info: {},
      rows: [],
      loading: false,
      config: {
        ready: false,
        status: []
      },
      infoVisible: false,
      infoLoading: false,
      formVisible: false,
      formLoading: false,
      form: {},
      rules: {
        name: [{ required: true, message: '请输入名称', trigger: 'blur' }],
        status: [{ required: true, message: '请选择状态', trigger: 'change' }]
      }
    }
  },
  methods: {
    toggleRowKeys () {
      this.toggle = !this.toggle
      if (this.toggle) {
        this.expandedRowKeys = []
      } else {
        this.expandedRowKeys = RouteUtil.expandedRowKeys(this.rows)
      }
    },
    batchRemove () {
      this.$confirm(this.selection.confirm(() => {
        this.loading = true
        resourceService.delete(this.selection.selectedRowKeys, { success: true }).then((result) => {
          if (result.code === 0) {
            this.search()
          } else {
            this.loading = false
          }
        })
      }))
    },
    search () {
      this.selection.clear()
      this.loading = true
      const id = this.$route.query.id
      resourceService.tree({ applicationId: id }).then((result) => {
        if (result.code === 0) {
          this.rows = result.data
          if (this.expandedRowKeys.length === 0) {
            this.expandedRowKeys = RouteUtil.expandedRowKeys(result.data, 1)
          }
        }
        this.loading = false
      })
    },
    submit () {
      this.$refs.form.validate(valid => {
        if (!valid || this.formLoading) return false
        this.formLoading = true
        resourceService.save(this.form).then(result => {
          if (result.code === 0) {
            this.formVisible = false
            this.search()
          }
          this.formLoading = false
        })
      })
    },
    sublevel (text, record) {
      this.form = { parentId: record.id, applicationId: record.applicationId, module: record.module, controller: record.controller, action: record.action }
      this.formVisible = true
    },
    add (parentId = 0) {
      this.form = { parentId, applicationId: this.info.id, module: this.info.serial }
      this.formVisible = true
    },
    edit (text, record) {
      this.form = Object.assign({}, record, {
        status: record.status + ''
      })
      this.formVisible = true
    },
    show (text, record) {
      this.form = Object.assign({}, record, {
        description: record.description ? record.description : '暂无'
      })
      this.infoVisible = true
    },
    reload () {
      const id = this.$route.query.id
      applicationService.info(id).then(result => {
        if (ApiUtil.succeed(result)) {
          this.info = result.data
        }
      })
      this.search()
    }
  },
  mounted () {
    this.reload()
    resourceService.config().then((result) => {
      this.config.ready = true
      if (result.code === 0) {
        Object.assign(this.config, result.data)
      }
    })
  }
}
</script>
