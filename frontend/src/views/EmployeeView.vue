<script setup lang="ts">
import { onMounted, ref } from 'vue';
import { ElAlert, ElButton, ElMessage, ElPagination, ElTable, ElTableColumn, ElTag } from 'element-plus';
import PageHeader from '../components/PageHeader.vue';
import EmployeeFormDialog from '../components/EmployeeFormDialog.vue';
import {
  createEmployee,
  listEmployees,
  updateEmployee,
  type Employee,
  type EmployeeInput,
  type EmployeeStatus,
  type Gender,
} from '../api/employee';

const rows = ref<Employee[]>([]);
const total = ref(0);
const page = ref(1);
const size = ref(10);
const loading = ref(false);
const error = ref<string | null>(null);

const dialogVisible = ref(false);
const editing = ref<Employee | null>(null);
const submitting = ref(false);
const submitError = ref<string | null>(null);

const GENDER_LABELS: Record<Gender, string> = {
  MALE: '男',
  FEMALE: '女',
  OTHER: '其他',
};

function genderLabel(gender: Gender | null): string {
  return gender ? GENDER_LABELS[gender] : '—';
}

function statusLabel(status: EmployeeStatus): string {
  return status === 'ACTIVE' ? '在职' : '离职';
}

function statusTagType(status: EmployeeStatus): 'success' | 'info' {
  return status === 'ACTIVE' ? 'success' : 'info';
}

async function load(): Promise<void> {
  loading.value = true;
  error.value = null;
  try {
    const result = await listEmployees({ page: page.value, size: size.value });
    rows.value = result.items;
    total.value = result.total;
    page.value = result.page;
    size.value = result.size;
  } catch (cause) {
    rows.value = [];
    total.value = 0;
    error.value = cause instanceof Error ? cause.message : '加载员工列表失败';
  } finally {
    loading.value = false;
  }
}

function handleCurrentChange(nextPage: number): void {
  if (nextPage === page.value) return;
  page.value = nextPage;
  void load();
}

function handleSizeChange(nextSize: number): void {
  if (nextSize === size.value) return;
  size.value = nextSize;
  page.value = 1;
  void load();
}

function openCreate(): void {
  editing.value = null;
  submitError.value = null;
  dialogVisible.value = true;
}

function openEdit(row: Employee): void {
  editing.value = row;
  submitError.value = null;
  dialogVisible.value = true;
}

async function handleSubmit(payload: EmployeeInput): Promise<void> {
  submitting.value = true;
  submitError.value = null;
  const isEdit = editing.value !== null;
  const editingId = editing.value?.id;
  try {
    if (isEdit && editingId !== undefined) {
      await updateEmployee(editingId, payload);
    } else {
      await createEmployee(payload);
    }
    dialogVisible.value = false;
    ElMessage.success(isEdit ? '员工信息已更新' : '员工新增成功');
    editing.value = null;
    await load();
  } catch (cause) {
    const message = cause instanceof Error ? cause.message : '保存失败，请稍后重试';
    submitError.value = message;
    ElMessage.error(message);
  } finally {
    submitting.value = false;
  }
}

onMounted(() => {
  void load();
});
</script>

<template>
  <main class="page page--wide">
    <PageHeader title="员工管理" subtitle="维护员工基础信息，支持新增与编辑。">
      <template #actions>
        <ElButton type="primary" data-testid="employee-create" @click="openCreate">新增员工</ElButton>
      </template>
    </PageHeader>

    <ElAlert v-if="error" class="list-error" :title="error" type="error" show-icon :closable="false">
      <ElButton size="small" @click="load">重试</ElButton>
    </ElAlert>

    <section class="card table-card">
      <ElTable v-loading="loading" :data="rows" row-key="id" stripe empty-text="暂无员工数据">
        <ElTableColumn prop="employeeNo" label="工号" min-width="110" />
        <ElTableColumn prop="name" label="姓名" min-width="100" />
        <ElTableColumn label="性别" min-width="80">
          <template #default="{ row }">{{ genderLabel(row.gender) }}</template>
        </ElTableColumn>
        <ElTableColumn label="部门" min-width="120">
          <template #default="{ row }">{{ row.department || '—' }}</template>
        </ElTableColumn>
        <ElTableColumn label="职位" min-width="120">
          <template #default="{ row }">{{ row.position || '—' }}</template>
        </ElTableColumn>
        <ElTableColumn label="手机号" min-width="130">
          <template #default="{ row }">{{ row.phone || '—' }}</template>
        </ElTableColumn>
        <ElTableColumn label="邮箱" min-width="190">
          <template #default="{ row }">{{ row.email || '—' }}</template>
        </ElTableColumn>
        <ElTableColumn label="入职日期" min-width="120">
          <template #default="{ row }">{{ row.hireDate || '—' }}</template>
        </ElTableColumn>
        <ElTableColumn label="状态" min-width="90">
          <template #default="{ row }">
            <ElTag :type="statusTagType(row.status)" effect="light">{{ statusLabel(row.status) }}</ElTag>
          </template>
        </ElTableColumn>
        <ElTableColumn label="操作" width="90" fixed="right">
          <template #default="{ row }">
            <ElButton link type="primary" data-testid="employee-edit" @click="openEdit(row as Employee)">编辑</ElButton>
          </template>
        </ElTableColumn>
      </ElTable>

      <div class="table-footer">
        <ElPagination
          background
          layout="total, sizes, prev, pager, next, jumper"
          :total="total"
          :current-page="page"
          :page-size="size"
          :page-sizes="[10, 20, 50]"
          @current-change="handleCurrentChange"
          @size-change="handleSizeChange"
        />
      </div>
    </section>

    <EmployeeFormDialog
      v-model:visible="dialogVisible"
      :employee="editing"
      :submitting="submitting"
      :server-error="submitError"
      @submit="handleSubmit"
    />
  </main>
</template>
