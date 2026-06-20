import { useState, useEffect } from 'react'
import { Card, Button, Tag, Space, Progress, message, Modal, Radio, Alert } from 'antd'
import { CheckOutlined, WechatOutlined, AlipayOutlined, CrownOutlined, ThunderboltOutlined, BankOutlined } from '@ant-design/icons'
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
  const [paymentModalVisible, setPaymentModalVisible] = useState(false)
  const [selectedPlan, setSelectedPlan] = useState<Plan | null>(null)
  const [paymentChannel, setPaymentChannel] = useState<'alipay' | 'wechat'>('alipay')
  const [creatingPayment, setCreatingPayment] = useState(false)
  const [stubMode, setStubMode] = useState(false)

  useEffect(() => {
    loadPlans()
    loadCurrentSubscription()
  }, [])

  const loadPlans = async () => {
    try {
      const res = await request.get<any, Plan[]>('/plan/list')
      setPlans(res || [])
    } catch (error) {
      message.error('加载套餐失败')
    }
  }

  const loadCurrentSubscription = async () => {
    try {
      const res = await request.get<any, { subscription: Subscription, currentUsage: number }>('/subscription/current')
      if (res) {
        setCurrentSubscription(res.subscription)
        setCurrentUsage(res.currentUsage || 0)
      }
    } catch {
      setCurrentSubscription(null)
    }
  }

  const handleSubscribe = (plan: Plan) => {
    if (plan.price === 0) {
      // 免费套餐直接订阅
      handleFreeSubscribe(plan)
      return
    }
    setSelectedPlan(plan)
    setPaymentModalVisible(true)
  }

  const handleFreeSubscribe = async (plan: Plan) => {
    setLoading(true)
    try {
      await request.post('/subscription/create', { planId: plan.id })
      message.success('订阅成功！')
      loadCurrentSubscription()
    } catch (error: any) {
      message.error(error.response?.data?.message || '订阅失败')
    } finally {
      setLoading(false)
    }
  }

  const handlePayment = async () => {
    if (!selectedPlan) return
    setCreatingPayment(true)
    try {
      const res = await request.post<any, { channel: string; tradeNo: string; paymentUrl?: string; codeUrl?: string; type?: string; mode?: string }>(
        '/payment/create',
        { planId: selectedPlan.id, channel: paymentChannel }
      )

      if (res.type === 'stub' || res.mode === 'demo') {
        // Stub 模式：演示支付
        setStubMode(true)
        Modal.info({
          title: '演示模式',
          content: (
            <div className="py-4">
              <p className="mb-2">支付功能当前处于演示模式（PAYMENT_ENABLED=false）</p>
              <p className="text-sm text-gray-500">支付渠道：{res.channel}</p>
              <p className="text-sm text-gray-500">订单号：{res.tradeNo}</p>
              <p className="text-sm text-gray-500">金额：¥{selectedPlan.price}</p>
              <Alert className="mt-3" message="真实环境中，这里会跳转微信支付/支付宝收银台" type="info" />
            </div>
          ),
          onOk: () => {
            setPaymentModalVisible(false)
            setStubMode(false)
          }
        })
      } else if (res.paymentUrl) {
        // 支付宝网页支付
        window.location.href = res.paymentUrl
      } else if (res.codeUrl) {
        // 微信支付二维码
        Modal.info({
          title: `请使用${paymentChannel === 'wechat' ? '微信' : '支付宝'}扫码支付`,
          content: (
            <div className="text-center py-4">
              <div className="bg-gray-100 p-4 rounded mb-4">
                <p className="text-sm text-gray-500">金额：¥{selectedPlan.price}</p>
                <p className="text-sm text-gray-500">订单号：{res.tradeNo}</p>
              </div>
              <Alert message={`请扫码支付（${paymentChannel === 'wechat' ? '微信' : '支付宝'}）`} type="info" />
            </div>
          ),
          okText: '支付完成',
          onOk: () => {
            message.success('支付提交成功，请等待系统确认...')
            setPaymentModalVisible(false)
            // 轮询订阅状态
            pollSubscription()
          }
        })
      }
    } catch (error: any) {
      message.error(error.response?.data?.message || '发起支付失败')
    } finally {
      setCreatingPayment(false)
    }
  }

  const pollSubscription = () => {
    let attempts = 0
    const interval = setInterval(async () => {
      attempts++
      try {
        const res = await request.get<any, { subscription: Subscription, currentUsage: number }>('/subscription/current')
        if (res?.subscription?.status === 'ACTIVE') {
          clearInterval(interval)
          setCurrentSubscription(res.subscription)
          message.success('订阅已激活！')
        }
      } catch {}
      if (attempts >= 10) {
        clearInterval(interval)
        message.warning('订阅状态确认中，请稍后刷新页面')
      }
    }, 3000)
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
      case 'Free': return <ThunderboltOutlined />
      case 'Pro': return <CrownOutlined />
      case 'Enterprise': return <BankOutlined />
      default: return <ThunderboltOutlined />
    }
  }

  const getPlanColor = (name: string) => {
    switch (name) {
      case 'Free': return 'default'
      case 'Pro': return 'blue'
      case 'Enterprise': return 'purple'
      default: return 'default'
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
                <Tag color={currentSubscription.status === 'ACTIVE' ? 'success' : 'default'}>
                  {currentSubscription.status === 'ACTIVE' ? '有效' :
                   currentSubscription.status === 'EXPIRED' ? '已过期' : '已取消'}
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
              <Button danger onClick={handleCancel} disabled={currentSubscription.status !== 'ACTIVE'}>
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
                <div className="text-3xl mb-2">{getPlanIcon(plan.name)}</div>
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
                  {(typeof plan.features === 'string' ? JSON.parse(plan.features) : plan.features).map((feature: string, idx: number) => (
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
                onClick={() => handleSubscribe(plan)}
              >
                {isCurrentPlan ? '当前套餐' : plan.price === 0 ? '免费开通' : isActive ? '立即开通' : '暂不可用'}
              </Button>
            </Card>
          )
        })}
      </div>

      {/* 支付方式选择弹窗 */}
      <Modal
        title={`开通 ${selectedPlan?.name} - ¥${selectedPlan?.price}`}
        open={paymentModalVisible}
        onCancel={() => setPaymentModalVisible(false)}
        footer={null}
        destroyOnClose
      >
        <div className="py-4">
          <p className="mb-4 text-gray-500">选择支付方式：</p>

          <Radio.Group
            value={paymentChannel}
            onChange={e => setPaymentChannel(e.target.value)}
            className="flex flex-col gap-3 mb-6"
          >
            <Radio value="alipay" className="p-3 border rounded">
              <Space>
                <AlipayOutlined style={{ fontSize: 20, color: '#1677ff' }} />
                <span>支付宝</span>
              </Space>
            </Radio>
            <Radio value="wechat" className="p-3 border rounded">
              <Space>
                <WechatOutlined style={{ fontSize: 20, color: '#07c160' }} />
                <span>微信支付</span>
              </Space>
            </Radio>
          </Radio.Group>

          <div className="flex justify-end gap-3">
            <Button onClick={() => setPaymentModalVisible(false)}>取消</Button>
            <Button type="primary" loading={creatingPayment} onClick={handlePayment}>
              确认支付 ¥{selectedPlan?.price}
            </Button>
          </div>
        </div>
      </Modal>

      {/* 底部说明 */}
      <Card className="mt-6" bordered={false} style={{ background: '#fafafa' }}>
        <h4 className="font-medium mb-2">温馨提示</h4>
        <ul className="text-sm text-gray-600 space-y-1">
          <li>• 所有套餐均支持随时取消，取消后服务持续到当月到期日</li>
          <li>• API配额按月统计，每月月初重置</li>
          <li>• 如需升级套餐，将立即生效并按比例计算差价</li>
          <li>• 支付成功后请等待1-2分钟系统确认</li>
        </ul>
      </Card>
    </div>
  )
}

export default PricingPage
