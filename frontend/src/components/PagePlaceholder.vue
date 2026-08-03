<template>
  <el-card>
    <el-empty :description="`「${title}」页面将在后续阶段实现`" />
  </el-card>
</template>

<script setup lang="ts">
defineProps<{ title: string }>()
</script>
