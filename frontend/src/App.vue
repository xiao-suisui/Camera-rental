<template>
  <div id="app">
    <router-view />
  </div>
</template>

<script setup>
</script>

<style>
@import url('https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&family=Noto+Sans+SC:wght@400;500;700&display=swap');

* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

body {
  font-family: 'Inter', 'Noto Sans SC', -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  color: #1a1a2e;
}

#app {
  min-height: 100vh;
  background: #f8f9fc;
}

/* 统一滚动条 */
::-webkit-scrollbar {
  width: 6px;
}
::-webkit-scrollbar-track {
  background: transparent;
}
::-webkit-scrollbar-thumb {
  background: #c1c1c1;
  border-radius: 3px;
}
::-webkit-scrollbar-thumb:hover {
  background: #a0a0a0;
}

/* 统一卡片圆角 */
.el-card {
  border-radius: 12px !important;
  border: none !important;
  box-shadow: 0 1px 3px rgba(0,0,0,0.04), 0 1px 2px rgba(0,0,0,0.06) !important;
}
.el-card:hover {
  box-shadow: 0 4px 12px rgba(0,0,0,0.08) !important;
}

/* 统一按钮圆角 */
.el-button {
  border-radius: 8px !important;
  font-weight: 500 !important;
}

/* 统一输入框 */
.el-input__wrapper {
  border-radius: 8px !important;
}

/* 统一 Tag */
.el-tag {
  border-radius: 6px !important;
}
</style>

