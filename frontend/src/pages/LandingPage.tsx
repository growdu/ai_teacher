import { useState } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import { Card, Button, Tag, Space, message, Carousel } from 'antd'
import { 
  RobotOutlined, BookOutlined, DollarOutlined, TeamOutlined,
  CheckCircleOutlined, PlayCircleOutlined, ArrowRightOutlined,
  BulbOutlined, ExperimentOutlined, CrownOutlined
} from '@ant-design/icons'

const LandingPage = () => {
  const navigate = useNavigate()

  const features = [
    {
      icon: <RobotOutlined style={{ fontSize: 40, color: '#1890ff' }} />,
      title: 'AI 智能导师',
      description: '基于先进大语言模型，为学生提供7×24小时个性化答疑辅导，精准定位知识盲区',
      demo: {
        title: '学生提问示例',
        content: 'Q: 什么是机器学习中的梯度下降？\n\nA: 梯度下降是一种优化算法，用于寻找函数的局部最小值...',
        user: '李同学 · 3分钟前'
      }
    },
    {
      icon: <BookOutlined style={{ fontSize: 40, color: '#52c41a' }} />,
      title: '课程管理系统',
      description: '创建、管理、销售在线课程，支持多级课程目录、章节管理和进度追踪',
      demo: {
        title: '课程数据预览',
        content: '《Python机器学习实战》\n• 42个课时 · 18小时视频\n• 学习人数：1,286\n• 评分：4.9/5.0 ⭐',
        user: '本周新增课程'
      }
    },
    {
      icon: <BulbOutlined style={{ fontSize: 40, color: '#faad14' }} />,
      title: '知识库管理',
      description: '构建企业级知识库，支持文档上传、智能标签、知识图谱构建与检索',
      demo: {
        title: '知识库统计',
        content: '文档总数：3,842\n知识点：12,567\n关联关系：8,934\n本周新增：156条',
        user: '实时数据'
      }
    },
    {
      icon: <ExperimentOutlined style={{ fontSize: 40, color: '#722ed1' }} />,
      title: '智能测验系统',
      description: 'AI自动生成测验题目，支持多种题型、自动评分与错题本管理',
      demo: {
        title: '测验报告示例',
        content: '正确率：78%\n用时：25分钟\n薄弱知识点：\n• 线性回归 · 决策树\n建议复习：第3章 第7节',
        user: '张同学 · 今日测验'
      }
    }
  ]

  const testimonials = [
    {
      name: '王老师',
      role: '某高校计算机教师',
      content: '使用AI Teacher Studio后，学生课后答疑工作量减少了70%，教学质量反而提升了',
      avatar: '👨‍🏫'
    },
    {
      name: '李校长',
      role: '在线教育机构创始人',
      content: '这套系统帮我们实现了课程数字化转型，3个月内营收增长了200%',
      avatar: '👨‍💼'
    },
    {
      name: '张同学',
      role: '计算机专业大三学生',
      content: 'AI导师随时随地解答我的问题，比找家教便宜多了，效果还更好',
      avatar: '👨‍🎓'
    }
  ]

  const plans = [
    {
      name: '免费版',
      price: 0,
      period: '永久',
      description: '适合个人体验',
      features: ['100次/月 AI对话', '3个课程', '1GB存储', '基础支持'],
      color: '#8c8c8c',
      highlighted: false
    },
    {
      name: '专业版',
      price: 99,
      period: '每月',
      description: '适合教师和小型机构',
      features: ['2000次/月 AI对话', '无限课程', '50GB存储', '优先支持', '高级分析', '自定义域名'],
      color: '#1890ff',
      highlighted: true
    },
    {
      name: '企业版',
      price: 399,
      period: '每月',
      description: '适合教育机构和团队',
      features: ['无限AI对话', '无限课程', '500GB存储', '专属客服', 'API接口', '私有部署', 'SLA保障'],
      color: '#722ed1',
      highlighted: false
    }
  ]

  const stats = [
    { value: '50,000+', label: '注册用户' },
    { value: '1,200+', label: '在线课程' },
    { value: '98%', label: '满意度' },
    { value: '7×24', label: 'AI服务' }
  ]

  return (
    <div className="min-h-screen bg-gradient-to-br from-slate-900 via-blue-900 to-slate-900">
      {/* 顶部导航 */}
      <header className="fixed top-0 left-0 right-0 z-50 bg-white/80 backdrop-blur-md border-b border-gray-100">
        <div className="max-w-7xl mx-auto px-6 py-4 flex justify-between items-center">
          <div className="flex items-center gap-2">
            <RobotOutlined style={{ fontSize: 28, color: '#1890ff' }} />
            <span className="text-xl font-bold text-gray-800">AI Teacher Studio</span>
          </div>
          <Space size="large">
            <Button type="text" className="text-gray-600" onClick={() => document.getElementById('features')?.scrollIntoView({ behavior: 'smooth' })}>功能介绍</Button>
            <Button type="text" className="text-gray-600" onClick={() => document.getElementById('pricing')?.scrollIntoView({ behavior: 'smooth' })}>价格方案</Button>
            <Button type="text" className="text-gray-600" onClick={() => document.getElementById('cases')?.scrollIntoView({ behavior: 'smooth' })}>案例展示</Button>
            <Link to="/login"><Button>登录</Button></Link>
            <Link to="/register"><Button type="primary">免费试用</Button></Link>
          </Space>
        </div>
      </header>

      {/* Hero Section */}
      <section className="pt-32 pb-20 px-6">
        <div className="max-w-7xl mx-auto text-center">
          <Tag color="blue" className="mb-6 text-sm px-4 py-1">🎉 新用户首月5折优惠</Tag>
          <h1 className="text-5xl font-bold text-white mb-6 leading-tight">
            用AI重新定义<br />
            <span className="text-blue-400">在线教育</span>的未来
          </h1>
          <p className="text-xl text-gray-300 mb-10 max-w-2xl mx-auto">
            为教育者打造的智能教学平台，集成AI导师、课程管理、知识库、智能测验于一体
          </p>
          <Space size="large">
            <Link to="/register">
              <Button type="primary" size="large" icon={<ArrowRightOutlined />} className="h-12 px-8">
                立即开始免费试用
              </Button>
            </Link>
            <Link to="/login">
              <Button size="large" className="h-12 px-8 bg-white/10 text-white border-white/20 hover:bg-white/20">
                已有账号？立即登录
              </Button>
            </Link>
          </Space>
          
          {/* 数据统计 */}
          <div className="mt-20 grid grid-cols-4 gap-8 max-w-3xl mx-auto">
            {stats.map((stat, idx) => (
              <div key={idx} className="text-center">
                <div className="text-3xl font-bold text-white">{stat.value}</div>
                <div className="text-gray-400 text-sm mt-1">{stat.label}</div>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* 功能展示 */}
      <section id="features" className="py-20 px-6 bg-white">
        <div className="max-w-7xl mx-auto">
          <div className="text-center mb-16">
            <h2 className="text-3xl font-bold text-gray-800 mb-4">强大的功能，满足您的教学需求</h2>
            <p className="text-gray-500 text-lg">一体化解决方案，让在线教育更简单、更高效</p>
          </div>

          <div className="grid grid-cols-2 gap-8">
            {features.map((feature, idx) => (
              <Card 
                key={idx} 
                className="hover:shadow-xl transition-all duration-300 border-0 shadow-lg"
                styles={{ body: { padding: 0 } }}
              >
                <div className="flex">
                  <div className="w-1/2 p-8">
                    <div className="mb-4">{feature.icon}</div>
                    <h3 className="text-xl font-bold text-gray-800 mb-2">{feature.title}</h3>
                    <p className="text-gray-500 mb-4">{feature.description}</p>
                    <Button type="link" className="text-blue-500 p-0" icon={<ArrowRightOutlined />}>
                      了解更多
                    </Button>
                  </div>
                  <div className="w-1/2 bg-gradient-to-br from-gray-50 to-blue-50 p-6 flex flex-col justify-center">
                    <div className="bg-white rounded-lg p-4 shadow-sm">
                      <div className="text-xs text-gray-400 mb-2">{feature.demo.title}</div>
                      <pre className="text-sm text-gray-700 whitespace-pre-wrap font-sans">
                        {feature.demo.content}
                      </pre>
                      <div className="text-xs text-gray-400 mt-3 flex items-center gap-2">
                        <span>{feature.demo.user}</span>
                        <CheckCircleOutlined style={{ color: '#52c41a', fontSize: 12 }} />
                      </div>
                    </div>
                  </div>
                </div>
              </Card>
            ))}
          </div>
        </div>
      </section>

      {/* 产品演示 */}
      <section className="py-20 px-6 bg-gradient-to-br from-blue-50 to-indigo-50">
        <div className="max-w-7xl mx-auto">
          <div className="text-center mb-12">
            <h2 className="text-3xl font-bold text-gray-800 mb-4">看看用户怎么说</h2>
            <p className="text-gray-500 text-lg">来自真实用户的反馈</p>
          </div>

          <div className="grid grid-cols-3 gap-8">
            {testimonials.map((item, idx) => (
              <Card key={idx} className="shadow-lg">
                <div className="text-4xl mb-4">{item.avatar}</div>
                <p className="text-gray-600 mb-4 italic">"{item.content}"</p>
                <div className="border-t pt-4">
                  <div className="font-medium text-gray-800">{item.name}</div>
                  <div className="text-sm text-gray-400">{item.role}</div>
                </div>
              </Card>
            ))}
          </div>
        </div>
      </section>

      {/* 价格方案 */}
      <section id="pricing" className="py-20 px-6 bg-white">
        <div className="max-w-7xl mx-auto">
          <div className="text-center mb-16">
            <h2 className="text-3xl font-bold text-gray-800 mb-4">简单透明的定价</h2>
            <p className="text-gray-500 text-lg">选择最适合您的方案，所有套餐均支持随时取消</p>
          </div>

          <div className="grid grid-cols-3 gap-8 max-w-5xl mx-auto">
            {plans.map((plan, idx) => (
              <Card 
                key={idx}
                className={`relative ${plan.highlighted ? 'border-2 shadow-xl scale-105' : 'shadow-lg'}`}
                style={{ borderColor: plan.highlighted ? plan.color : undefined }}
              >
                {plan.highlighted && (
                  <Tag color={plan.color} className="absolute -top-3 left-1/2 -translate-x-1/2 px-4">
                    最受欢迎
                  </Tag>
                )}
                <div className="text-center mb-6">
                  <div className="text-lg font-medium text-gray-800 mb-2">{plan.name}</div>
                  <div className="text-gray-500 text-sm">{plan.description}</div>
                </div>
                <div className="text-center mb-6">
                  <span className="text-4xl font-bold" style={{ color: plan.color }}>¥{plan.price}</span>
                  <span className="text-gray-400">/{plan.period}</span>
                </div>
                <div className="space-y-3 mb-6">
                  {plan.features.map((feature, fidx) => (
                    <div key={fidx} className="flex items-center text-sm text-gray-600">
                      <CheckCircleOutlined style={{ color: plan.color, marginRight: 8 }} />
                      {feature}
                    </div>
                  ))}
                </div>
                <Link to="/register">
                  <Button 
                    type={plan.highlighted ? 'primary' : 'default'} 
                    block 
                    size="large"
                    style={plan.highlighted ? { backgroundColor: plan.color, borderColor: plan.color } : undefined}
                  >
                    {plan.price === 0 ? '免费开始' : '立即开通'}
                  </Button>
                </Link>
              </Card>
            ))}
          </div>

          <div className="text-center mt-8 text-gray-400 text-sm">
            * 所有套餐均支持7天无理由退款 · 年付更优惠
          </div>
        </div>
      </section>

      {/* CTA */}
      <section className="py-20 px-6 bg-gradient-to-r from-blue-600 to-indigo-600">
        <div className="max-w-3xl mx-auto text-center">
          <CrownOutlined style={{ fontSize: 48, color: '#fff', marginBottom: 16 }} />
          <h2 className="text-3xl font-bold text-white mb-4">准备好开始了吗？</h2>
          <p className="text-blue-100 mb-8 text-lg">加入50,000+教育者，让AI赋能您的教学</p>
          <Link to="/register">
            <Button type="primary" size="large" className="h-14 px-12 text-lg bg-white text-blue-600 hover:bg-blue-50 border-0">
              立即免费试用，无需信用卡
            </Button>
          </Link>
        </div>
      </section>

      {/* Footer */}
      <footer className="bg-gray-900 text-gray-400 py-12 px-6">
        <div className="max-w-7xl mx-auto">
          <div className="flex justify-between items-center">
            <div className="flex items-center gap-2">
              <RobotOutlined style={{ fontSize: 24, color: '#1890ff' }} />
              <span className="text-lg font-medium text-white">AI Teacher Studio</span>
            </div>
            <div className="text-sm">
              © 2024 AI Teacher Studio. 保留所有权利.
            </div>
          </div>
        </div>
      </footer>
    </div>
  )
}

export default LandingPage
