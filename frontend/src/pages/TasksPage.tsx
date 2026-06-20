import { useState, useEffect, useRef } from 'react'
import { Table, Tag, Button, Space, Modal, message, Card, Row, Col, Statistic, Progress, Typography } from 'antd'
import { SyncOutlined, DeleteOutlined, EyeOutlined, ReloadOutlined } from '@ant-design/icons'
import request from '@/api/request'

const { Text } = Typography

interface Task {
  id: number
  taskType: string
  taskName: string
  status: string
  progress: number
  currentStep: string
  errorMessage?: string
  createdAt: string
  completedAt?: string
}

const statusMap: Record<string, { color: string; label: string }> = {
  pending:    { color: 'default',  label: '等待中' },
  running:    { color: 'processing', label: '进行中' },
  completed:  { color: 'success',  label: '已完成' },
  failed:     { color: 'error',    label: '失败' },
  cancelled:  { color: 'default',  label: '已取消' },
}

const taskTypeMap: Record<string, string> = {
  course_generate: '课程生成',
  ppt_generate:    'PPT生成',
  video_generate:  '视频生成',
  quiz_generate:   '试题生成',
  tts_generate:    '语音合成',
}

const TasksPage = () => {
  const [data, setData] = useState<Task[]>([])
  const [loading, setLoading] = useState(false)
  const [detailModal, setDetailModal] = useState(false)
  const [selectedTask, setSelectedTask] = useState<Task | null>(null)
  const pollingRef = useRef<ReturnType<typeof setInterval> | null>(null)

  const loadData = async () => {
    try {
      const res = await request.get('/task/list') as any
      const tasks = res?.data || []
      setData(tasks)

      // Keep polling for running tasks
      const hasRunning = tasks.some((t: Task) => t.status === 'running' || t.status === 'pending')
      if (hasRunning && !pollingRef.current) {
        startPolling()
      } else if (!hasRunning && pollingRef.current) {
        stopPolling()
      }
    } catch {
      // silent fail — task list is non-critical
    }
  }

  const startPolling = () => {
    if (pollingRef.current) return
    pollingRef.current = setInterval(() => {
      loadData()
    }, 3000)
  }

  const stopPolling = () => {
    if (pollingRef.current) {
      clearInterval(pollingRef.current)
      pollingRef.current = null
    }
  }

  useEffect(() => {
    loadData()
    return () => stopPolling()
  }, [])

  const columns = [
    {
      title: 'ID',
      dataIndex: 'id',
      key: 'id',
      width: 60,
    },
    {
      title: '任务名称',
      dataIndex: 'taskName',
      key: 'taskName',
      width: 220,
      ellipsis: true,
    },
    {
      title: '类型',
      dataIndex: 'taskType',
      key: 'taskType',
      width: 100,
      render: (v: string) => taskTypeMap[v] || v,
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      width: 90,
      render: (status: string) => {
        const s = statusMap[status] || { color: 'default', label: status }
        return <Tag color={s.color}>{s.label}</Tag>
      },
    },
    {
      title: '进度',
      dataIndex: 'progress',
      key: 'progress',
      width: 140,
      render: (progress: number, record: Task) =>
        record.status === 'running' || record.status === 'pending' ? (
          <Progress percent={progress} size="small" status="active" />
        ) : (
          <Progress percent={progress} size="small" status={record.status === 'failed' ? 'exception' : 'success'} />
        ),
    },
    {
      title: '当前步骤',
      dataIndex: 'currentStep',
      key: 'currentStep',
      width: 140,
      ellipsis: true,
    },
    {
      title: '创建时间',
      dataIndex: 'createdAt',
      key: 'createdAt',
      width: 170,
    },
    {
      title: '操作',
      key: 'action',
      width: 120,
      render: (_: any, record: Task) => (
        <Space>
          <Button type="link" icon={<EyeOutlined />} onClick={() => {
            setSelectedTask(record)
            setDetailModal(true)
          }} />
          {(record.status === 'pending' || record.status === 'running') && (
            <Button type="link" danger icon={<DeleteOutlined />} onClick={() => handleCancel(record.id)} />
          )}
        </Space>
      ),
    },
  ]

  const handleCancel = async (id: number) => {
    try {
      await request.delete(`/task/${id}`)
      message.success('任务已取消')
      loadData()
    } catch {
      message.error('取消失败')
    }
  }

  const stats = {
    total: data.length,
    running: data.filter(t => t.status === 'running' || t.status === 'pending').length,
    completed: data.filter(t => t.status === 'completed').length,
    failed: data.filter(t => t.status === 'failed').length,
  }

  return (
    <div>
      <div className="flex justify-between mb-4">
        <h2 className="text-xl font-bold">任务中心</h2>
        <Button icon={<ReloadOutlined />} onClick={loadData}>刷新</Button>
      </div>

      <Row gutter={16} className="mb-4">
        <Col span={6}>
          <Card><Statistic title="任务总数" value={stats.total} /></Card>
        </Col>
        <Col span={6}>
          <Card><Statistic title="进行中" value={stats.running} /></Card>
        </Col>
        <Col span={6}>
          <Card><Statistic title="已完成" value={stats.completed} /></Card>
        </Col>
        <Col span={6}>
          <Card><Statistic title="失败" value={stats.failed} /></Card>
        </Col>
      </Row>

      <Table
        columns={columns}
        dataSource={data}
        loading={loading}
        rowKey="id"
        pagination={{
          pageSize: 10,
          showSizeChanger: false,
          showTotal: (t) => `共 ${t} 条`,
        }}
      />

      <Modal
        title="任务详情"
        open={detailModal}
        onCancel={() => setDetailModal(false)}
        footer={[
          <Button key="close" onClick={() => setDetailModal(false)}>关闭</Button>,
        ]}
        width={520}
      >
        {selectedTask && (
          <div className="space-y-3">
            <Row gutter={16}>
              <Col span={12}>
                <Text type="secondary">任务名称</Text>
                <div className="font-medium">{selectedTask.taskName}</div>
              </Col>
              <Col span={12}>
                <Text type="secondary">任务类型</Text>
                <div className="font-medium">{taskTypeMap[selectedTask.taskType] || selectedTask.taskType}</div>
              </Col>
            </Row>
            <Row gutter={16}>
              <Col span={12}>
                <Text type="secondary">状态</Text>
                <div>
                  <Tag color={statusMap[selectedTask.status]?.color || 'default'}>
                    {statusMap[selectedTask.status]?.label || selectedTask.status}
                  </Tag>
                </div>
              </Col>
              <Col span={12}>
                <Text type="secondary">进度</Text>
                <div className="mt-1">
                  <Progress percent={selectedTask.progress} size="small" />
                </div>
              </Col>
            </Row>
            <div>
              <Text type="secondary">当前步骤</Text>
              <div className="font-medium">{selectedTask.currentStep || '-'}</div>
            </div>
            {selectedTask.errorMessage && (
              <div>
                <Text type="secondary">错误信息</Text>
                <div className="text-red-500">{selectedTask.errorMessage}</div>
              </div>
            )}
            <Row gutter={16}>
              <Col span={12}>
                <Text type="secondary">创建时间</Text>
                <div>{selectedTask.createdAt}</div>
              </Col>
              {selectedTask.completedAt && (
                <Col span={12}>
                  <Text type="secondary">完成时间</Text>
                  <div>{selectedTask.completedAt}</div>
                </Col>
              )}
            </Row>
          </div>
        )}
      </Modal>
    </div>
  )
}

export default TasksPage