import { useState, useEffect } from 'react'
import { Card, Button, Tag, Space, Progress, message, Modal } from 'antd'
import { CheckOutlined, CloseOutlined, CrownOutlined, ThunderboltOutlined, BankOutlined } from '@ant-design/icons'
import request from '@/api/request'

interface Plan {
  id: number
  name: string
  description: string
  price: number
  billingPeriod: string
  features: string[]
  apiQuota: number
  storageQuota: number
  status: string
}

interface Subscription {
  id: number
  tenantId: number
  planId: number
  status: string
  startedAt: string
  expiresAt: string
  autoRenew: boolean
  plan?: Plan
}

const PricingPage = () => {
  const [plans, setPlans] = useState<Plan[]>([])
  const [currentSubscription, setCurrentSubscription] = useState<Subscription | null>(null)
  const [loading, setLoading] = useState(false)
  const [currentUsage, setCurrentUsage] = useState(0)

  useEffect(() => {
    loadPlans()
    loadCurrentSubscription()
  }, [])

  const loadPlans = async () => {
    try {
      const res = await request.get('/plans')
      setPlans(res || [])
    } catch (error) {
      message.error('加载套餐失败')
    }
  }

  const loadCurrentSubscription = async () => {
    try {
      const res = await request.get('/subscription/current')
      if (res) {
        setCurrentSubscription(res.subscription)
        setCurrentUsage(res.currentUsage || 0)
      }
    } catch (error) {
      // 可能没有订阅
      setCurrentSubscription(null)
    }
  }

  const handleSubscribe = async (planId: number) => {
    setLoading(true)
    try {
      await request.post('/subscription/create', { planId })
      message.success('订阅成功')
      loadCurrentSubscription()
    } catch (error: any) {
      if (error.response?.data?.code === 402) {
        Modal.warning({
          title: '配额不足',
          content: error.response.data.message || 'API配额已用完，请升级套餐或等待下月重置',
        })
      } else {
        message.error('订阅失败')
      }
    } finally {
      setLoading(false)
    }
  }

  const handleCancel = async () => {
    try {
      await request.put('/subscription/cancel')
      message.success('订阅已取消')
      loadCurrentSubscription()
    } catch (error) {
      message.error('取消订阅失败')
    }
  }

  const getQuotaDisplay = (quota: number) => {
    return quota === -1 ? '无限' : quota
  }

  const getPlanIcon = (name: string) => {
    switch (name) {
      case 'Free':
        return <ThunderboltOutlined />
      case 'Pro':
        return <CrownOutlined />
      case 'Enterprise':
        return <BankOutlined />
      default:
        return <ThunderboltOutlined />
    }
  }

  const getPlanColor = (name: string) => {
    switch (name) {
      case 'Free':
        return 'default'
      case 'Pro':
        return 'blue'
      case 'Enterprise':
        return 'purple'
      default:
        return 'default'
    }
  }

  return (
    <div className="p-6">
      <div className="mb-6">
        <h1 className="text-2xl font-bold mb-2">订阅套餐</h1>
        <p className="text-gray-500">选择适合您的订阅计划，解锁更多功能</p>
      </div>

      {/* 当前订阅状态 */}
      {currentSubscription && (
        <Card className="mb-6" bordered={false} style={{ background: '#f6f8fa' }}>
          <div className="flex justify-between items-center">
            <div>
              <Space align="center">
                <span className="font-medium">当前订阅：</span>
                <Tag color={getPlanColor(currentSubscription.plan?.name || '')}>
                  {currentSubscription.plan?.name || 'Unknown'}
                </Tag>
                <Tag color={currentSubscription.status === 'active' ? 'success' : 'default'}>
                  {currentSubscription.status === 'active' ? '有效' : 
                   currentSubscription.status === 'cancelled' ? '已取消' : '已过期'}
                </Tag>
              </Space>
              <div className="text-gray-500 mt-2 text-sm">
                到期时间：{new Date(currentSubscription.expiresAt).toLocaleDateString('zh-CN')}
                {currentSubscription.autoRenew && <Tag className="ml-2" color="green">自动续订</Tag>}
              </div>
              {currentSubscription.plan && currentSubscription.plan.apiQuota !== -1 && (
                <div className="mt-3" style={{ width: 300 }}>
                  <div className="flex justify-between text-sm mb-1">
                    <span>本月API使用量</span>
                    <span>{currentUsage} / {currentSubscription.plan.apiQuota}</span>
                  </div>
                  <Progress 
                    percent={Math.min(100, (currentUsage / currentSubscription.plan.apiQuota) * 100)} 
                    size="small"
                    status={currentUsage >= currentSubscription.plan.apiQuota ? 'exception' : 'normal'}
                  />
                </div>
              )}
            </div>
            <div>
              <Button danger onClick={handleCancel} disabled={currentSubscription.status !== 'active'}>
                取消订阅
              </Button>
            </div>
          </div>
        </Card>
      )}

      {/* 套餐卡片 */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        {plans.map((plan) => {
          const isCurrentPlan = currentSubscription?.planId === plan.id
          const isActive = plan.status === 'active'

          return (
            <Card
              key={plan.id}
              className={isCurrentPlan ? 'border-blue-500' : ''}
              style={{ 
                borderWidth: isCurrentPlan ? 2 : 1,
                position: 'relative',
              }}
              styles={{ body: { padding: '24px' } }}
            >
              {isCurrentPlan && (
                <Tag color="blue" style={{ position: 'absolute', top: 12, right: 12 }}>
                  当前套餐
                </Tag>
              )}
              
              <div className="text-center mb-4">
                <div className={`text-3xl mb-2 ${getPlanColor(plan.name)}`}>
                  {getPlanIcon(plan.name)}
                </div>
                <h3 className="text-xl font-bold">{plan.name}</h3>
                <p className="text-gray-500 text-sm mt-1">{plan.description}</p>
              </div>

              <div className="text-center mb-6">
                <span className="text-4xl font-bold">¥{plan.price}</span>
                <span className="text-gray-500">/{plan.billingPeriod === 'monthly' ? '月' : '年'}</span>
              </div>

              <div className="space-y-3 mb-6">
                <div className="flex justify-between text-sm">
                  <span className="text-gray-500">API配额</span>
                  <span className="font-medium">{getQuotaDisplay(plan.apiQuota)} 次/月</span>
                </div>
                <div className="flex justify-between text-sm">
                  <span className="text-gray-500">存储空间</span>
                  <span className="font-medium">{plan.storageQuota} GB</span>
                </div>
              </div>

              <div className="border-t pt-4 mb-4">
                <p className="text-sm font-medium mb-2">功能包含：</p>
                <ul className="space-y-2">
                  {(typeof plan.features === 'string' ? JSON.parse(plan.features) : plan.features).map((feature, idx) => (
                    <li key={idx} className="flex items-center text-sm">
                      <CheckOutlined className="text-green-500 mr-2" />
                      {feature}
                    </li>
                  ))}
                </ul>
              </div>

              <Button
                type={isCurrentPlan ? 'default' : 'primary'}
                block
                disabled={!isActive || isCurrentPlan}
                loading={loading}
                onClick={() => handleSubscribe(plan.id)}
              >
                {isCurrentPlan ? '当前套餐' : isActive ? '立即开通' : '暂不可用'}
              </Button>
            </Card>
          )
        })}
      </div>

      {/* 底部说明 */}
      <Card className="mt-6" bordered={false} style={{ background: '#fafafa' }}>
        <h4 className="font-medium mb-2">温馨提示</h4>
        <ul className="text-sm text-gray-600 space-y-1">
          <li>• 所有套餐均支持随时取消，取消后服务持续到当月到期日</li>
          <li>• API配额按月统计，每月月初重置</li>
          <li>• 如需升级套餐，将立即生效并按比例计算差价</li>
          <li>• 如遇问题请联系客服支持</li>
        </ul>
      </Card>
    </div>
  )
}

export default PricingPage
