# member

用户中心。

## 组织架构（organize）

### 架构组成
- 部门（department）
- 岗位（job）
- 分组（group）

### 架构说明
- 一个用户可从属于多个部门，在单个部门下可承担多个岗位。
- 部门和岗位有上下级关系，用于控制数据的访问范围和流程指派。
- 一个用户可从属于多个分组，仅用于用户筛选，不参与权限校验。

## 会话

- 会话有效期由 `server.servlet.session.timeout` 控制（默认 30d），Cookie 有效期由 `server.servlet.session.cookie.max-age` 控制。
- 登录接口 `/user/login` 在未传 `serial` 时用于校验当前会话：会话不存在或已过期时返回空 `info`；账号被禁用、删除或锁定时自动注销。
- 会话按最后访问时间滑动续期：服务端访问会话时自动刷新 Redis 中的过期时间；浏览器 Cookie 方式在调用 `/user/login` 校验通过后重新下发 Cookie，刷新 Max-Age，其他接口不做额外处理。
- 登录成功或会话校验通过时，`info` 返回 `token`、`maxInactiveInterval`、`expireTime`、`remaining`，便于客户端提前处理续期或重新登录。

## 参考
