import type { PostVO } from '@/api/types'

/**
 * Mock 数据
 *
 * 鉴权接口走真实后端，这里只保留非鉴权 mock。
 */

/* ==================== 社区动态 ==================== */

/** 模拟动态列表 */
export const mockPosts: PostVO[] = [
  {
    id: 1,
    userId: 2001,
    username: '橘猫小胖',
    avatar: '🐱',
    content: '今天天气真好，在阳台晒太阳 🌞',
    images: [
      'https://placekitten.com/300/300',
      'https://placekitten.com/301/301',
      'https://placekitten.com/302/302',
    ],
    tags: ['日常', '晒太阳'],
    likeCount: 128,
    commentCount: 12,
    favoriteCount: 8,
    isLiked: false,
    isFavorited: false,
    createdAt: new Date(Date.now() - 2 * 3600 * 1000).toISOString(),
  },
  {
    id: 2,
    userId: 2002,
    username: '布偶豆豆',
    avatar: '🐈',
    content: '新买的猫窝到了，超喜欢！软软的，睡得都不想动 🐾',
    images: ['https://placekitten.com/400/400'],
    tags: ['好物分享'],
    likeCount: 256,
    commentCount: 34,
    favoriteCount: 21,
    isLiked: true,
    isFavorited: false,
    createdAt: new Date(Date.now() - 4 * 3600 * 1000).toISOString(),
  },
  {
    id: 3,
    userId: 2003,
    username: '英短球球',
    avatar: '😺',
    content: '铲屎官今天忘记喂罐头了，抗议！',
    images: [],
    tags: ['吐槽'],
    likeCount: 89,
    commentCount: 5,
    favoriteCount: 2,
    isLiked: false,
    isFavorited: false,
    createdAt: new Date(Date.now() - 6 * 3600 * 1000).toISOString(),
  },
  {
    id: 4,
    userId: 2004,
    username: '狸花阿黄',
    avatar: '😻',
    content: '今天学会开抽屉了，感觉解锁了新技能',
    images: [],
    tags: ['搞笑', '日常'],
    likeCount: 512,
    commentCount: 67,
    favoriteCount: 45,
    isLiked: false,
    isFavorited: true,
    createdAt: new Date(Date.now() - 24 * 3600 * 1000).toISOString(),
  },
]