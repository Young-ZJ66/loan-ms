<script setup>
</script>

<template>
  <div class="app-root">
    <router-view />
  </div>
</template>

<style>
/* 根节点无需复杂布局，全权交给 Layout 处理 */
.app-root {
  width: 100vw;
  height: 100vh;
  overflow: hidden;
}
</style>
