import { createRouter, createWebHistory } from 'vue-router';
import HomeView from '../views/HomeView.vue';

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      name: 'home',
      component: HomeView,
    },
    {
      path: '/employees',
      name: 'employees',
      component: () => import('../views/EmployeeView.vue'),
    },
  ],
});

export default router;
