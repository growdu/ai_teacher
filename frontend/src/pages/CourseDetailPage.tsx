import { useState, useEffect } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { Card, Button, Tag, Spin, Descriptions, Collapse, message, Space } from 'antd'
import { ArrowLeftOutlined, FileTextOutlined } from '@ant-design/icons'
import request from '@/api/request'

interface Course {
  id: number
  title: string
  status: string
  knowledgePointId: number
  createdAt: string
  outline?: any
  script?: string
}

interface Chapter {
  title: string
  duration: number
  keyPoints: string[]
  teachingNotes: string
}

interface Outline {
  title: string
  description: string
  chapters: Chapter[]
  totalDuration: number
}

const CourseDetailPage = () => {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const [course, setCourse] = useState<Course | null>(null)
  const [loading, setLoading] = useState(true)
  const [outlineData, setOutlineData] = useState<Outline | null>(null)

  useEffect(() => {
    if (!id) return
    loadCourse(+id)
  }, [id])

  const loadCourse = async (courseId: number) => {
    setLoading(true)
    try {
      const res = await request.get(`/course/${courseId}`) as any
      if (res.code === 200) {
        setCourse(res.data)
        if (res.data?.outline) {
          try {
            const parsed = typeof res.data.outline === 'string'
              ? JSON.parse(res.data.outline)
              : res.data.outline
            setOutlineData(parsed)
          } catch {
            setOutlineData(null)
          }
        }
      } else {
        message.error(res.message || '加载失败')
        navigate('/course')
      }
    } catch (error) {
      message.error('加载课程详情失败')
      navigate('/course')
    } finally {
      setLoading(false)
    }
  }

  if (loading) {
    return <Spin tip="加载中..." style={{ marginTop: 100, textAlign: 'center' }} />
  }

  if (!course) return null

  const statusMap: Record<string, { color: string; label: string }> = {
    draft: { color: 'default', label: '草稿' },
    generating: { color: 'processing', label: '生成中' },
    generated: { color: 'success', label: '已生成' },
    failed: { color: 'error', label: '失败' },
  }

  return (
    <div style={{ padding: 24 }}>
      <div style={{ marginBottom: 16 }}>
        <Button icon={<ArrowLeftOutlined />} onClick={() => navigate('/course')}>
          返回课程列表
        </Button>
      </div>

      <Card
        title={<span style={{ fontSize: 20 }}>{course.title}</span>}
        extra={
          <Space>
            <Tag color={statusMap[course.status]?.color || 'default'}>
              {statusMap[course.status]?.label || course.status}
            </Tag>
            <Button
              type="primary"
              icon={<FileTextOutlined />}
              onClick={() => navigate(`/quiz?courseId=${course.id}`)}
            >
              生成测验
            </Button>
          </Space>
        }
      >
        <Descriptions column={2} style={{ marginBottom: 24 }}>
          <Descriptions.Item label="课程ID">{course.id}</Descriptions.Item>
          <Descriptions.Item label="创建时间">{course.createdAt}</Descriptions.Item>
          <Descriptions.Item label="知识点ID">{course.knowledgePointId}</Descriptions.Item>
          <Descriptions.Item label="状态">{statusMap[course.status]?.label || course.status}</Descriptions.Item>
        </Descriptions>

        {outlineData && (
          <div style={{ marginBottom: 24 }}>
            <h3 style={{ marginBottom: 12 }}>课程大纲</h3>
            {outlineData.description && (
              <p style={{ color: '#666', marginBottom: 16 }}>{outlineData.description}</p>
            )}
            {outlineData.totalDuration && (
              <p style={{ color: '#888', marginBottom: 16 }}>总时长：{outlineData.totalDuration} 分钟</p>
            )}
            <Collapse
              items={
                outlineData.chapters?.map((ch: Chapter, idx: number) => ({
                  key: String(idx),
                  label: `第${idx + 1}章：${ch.title}${ch.duration ? ` (${ch.duration}分钟)` : ''}`,
                  children: (
                    <div>
                      {ch.keyPoints && ch.keyPoints.length > 0 && (
                        <div style={{ marginBottom: 8 }}>
                          <strong>重点：</strong>
                          <ul style={{ margin: '8px 0' }}>
                            {ch.keyPoints.map((kp, i) => (
                              <li key={i}>{kp}</li>
                            ))}
                          </ul>
                        </div>
                      )}
                      {ch.teachingNotes && (
                        <div>
                          <strong>教学备注：</strong>
                          <p style={{ color: '#555', margin: '8px 0' }}>{ch.teachingNotes}</p>
                        </div>
                      )}
                    </div>
                  ),
                })) || []
              }
            />
          </div>
        )}

        {course.script && (
          <div>
            <h3 style={{ marginBottom: 12 }}>讲稿脚本</h3>
            <Card
              style={{
                background: '#f5f5f5',
                whiteSpace: 'pre-wrap',
                fontSize: 14,
                lineHeight: 1.8,
                maxHeight: 500,
                overflow: 'auto',
              }}
            >
              {course.script}
            </Card>
          </div>
        )}
      </Card>
    </div>
  )
}

export default CourseDetailPage