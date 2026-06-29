import { Result, Button } from 'antd'
import { Link } from 'react-router-dom'

const NotFound = () => {
  return (
    <Result
      status={404}
      title="页面不存在"
      subTitle="抱歉，您访问的页面不存在或已被删除"
      extra={
        <Link to="/">
          <Button type="primary">返回首页</Button>
        </Link>
      }
    />
  )
}

export default NotFound
