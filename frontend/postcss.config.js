export default {
  plugins: {
    'postcss-px-to-viewport': {
      viewportWidth: 375, // 设计稿宽度
      viewportHeight: 812, // 设计稿高度
      unitPrecision: 5, // 转换精度
      viewportUnit: 'vw', // 转换后的单位
      fontViewportUnit: 'vw', // 字体转换后的单位
      selectorBlackList: ['ignore', 'tab-bar'], // 不转换的选择器
      minPixelValue: 1, // 最小转换值
      mediaQuery: false, // 是否转换媒体查询中的px
      exclude: [/node_modules/, /\/admin\//], // 排除的文件（PC 端不走 vw 转换）
    },
  },
}
