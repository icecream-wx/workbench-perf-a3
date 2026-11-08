<script setup lang="ts">
import { RouterLink, RouterView, useRoute } from 'vue-router';

const route = useRoute();
</script>

<template>
  <div class="app-shell">
    <header class="app-nav">
      <RouterLink class="app-brand" to="/">perf-a3</RouterLink>
      <nav class="app-nav-links">
        <RouterLink to="/" :class="{ active: route.name === 'home' }">概览</RouterLink>
        <RouterLink to="/employees" :class="{ active: route.name === 'employees' }">员工管理</RouterLink>
      </nav>
    </header>
    <RouterView />
  </div>
</template>
