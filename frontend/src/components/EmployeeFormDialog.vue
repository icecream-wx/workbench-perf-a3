<script setup lang="ts">
import { nextTick, reactive, ref, watch } from 'vue';
import {
  ElAlert,
  ElButton,
  ElCol,
  ElDatePicker,
  ElDialog,
  ElForm,
  ElFormItem,
  ElInput,
  ElOption,
  ElRow,
  ElSelect,
  type FormInstance,
  type FormRules,
} from 'element-plus';
import type { Employee, EmployeeInput, EmployeeStatus, Gender } from '../api/employee';

const props = defineProps<{
  visible: boolean;
  employee: Employee | null;
  submitting: boolean;
  serverError: string | null;
}>();

const emit = defineEmits<{
  'update:visible': [value: boolean];
  submit: [payload: EmployeeInput];
}>();

interface EmployeeForm {
  employeeNo: string;
  name: string;
  gender: Gender | '';
  department: string;
  position: string;
  phone: string;
  email: string;
  hireDate: string;
  status: EmployeeStatus;
}

const GENDER_OPTIONS: { value: Gender; label: string }[] = [
  { value: 'MALE', label: '男' },
  { value: 'FEMALE', label: '女' },
  { value: 'OTHER', label: '其他' },
];

const STATUS_OPTIONS: { value: EmployeeStatus; label: string }[] = [
  { value: 'ACTIVE', label: '在职' },
  { value: 'INACTIVE', label: '离职' },
];

const formRef = ref<FormInstance>();

function emptyForm(): EmployeeForm {
  return {
    employeeNo: '',
    name: '',
    gender: '',
    department: '',
    position: '',
    phone: '',
    email: '',
    hireDate: '',
    status: 'ACTIVE',
  };
}

const form = reactive<EmployeeForm>(emptyForm());

const rules: FormRules<EmployeeForm> = {
  employeeNo: [
    { required: true, whitespace: true, message: '请输入工号', trigger: 'blur' },
    { max: 32, message: '工号长度不能超过 32', trigger: 'blur' },
  ],
  name: [
    { required: true, whitespace: true, message: '请输入姓名', trigger: 'blur' },
    { max: 64, message: '姓名长度不能超过 64', trigger: 'blur' },
  ],
  department: [{ max: 64, message: '部门长度不能超过 64', trigger: 'blur' }],
  position: [{ max: 64, message: '职位长度不能超过 64', trigger: 'blur' }],
  phone: [{ max: 32, message: '手机号长度不能超过 32', trigger: 'blur' }],
  email: [
    { type: 'email', message: '请输入合法的邮箱地址', trigger: 'blur' },
    { max: 128, message: '邮箱长度不能超过 128', trigger: 'blur' },
  ],
  hireDate: [{ pattern: /^\d{4}-\d{2}-\d{2}$/, message: '请输入合法的入职日期', trigger: 'change' }],
};

function resetForm(): void {
  const source = props.employee;
  form.employeeNo = source?.employeeNo ?? '';
  form.name = source?.name ?? '';
  form.gender = source?.gender ?? '';
  form.department = source?.department ?? '';
  form.position = source?.position ?? '';
  form.phone = source?.phone ?? '';
  form.email = source?.email ?? '';
  form.hireDate = source?.hireDate ?? '';
  form.status = source?.status ?? 'ACTIVE';
}

watch(
  () => props.visible,
  async (visible) => {
    if (!visible) return;
    resetForm();
    await nextTick();
    formRef.value?.clearValidate();
  },
);

function close(): void {
  if (props.submitting) return;
  emit('update:visible', false);
}

function onVisibleChange(value: boolean): void {
  if (!value && props.submitting) return;
  emit('update:visible', value);
}

function optional(value: string): string | null {
  const trimmed = value.trim();
  return trimmed === '' ? null : trimmed;
}

async function submit(): Promise<void> {
  const formEl = formRef.value;
  if (!formEl) return;
  const valid = await formEl.validate().catch(() => false);
  if (!valid) return;
  emit('submit', {
    employeeNo: form.employeeNo.trim(),
    name: form.name.trim(),
    gender: form.gender === '' ? null : form.gender,
    department: optional(form.department),
    position: optional(form.position),
    phone: optional(form.phone),
    email: optional(form.email),
    hireDate: optional(form.hireDate),
    status: form.status,
  });
}
</script>

<template>
  <ElDialog
    :model-value="visible"
    :title="employee ? '编辑员工' : '新增员工'"
    width="640px"
    :close-on-click-modal="false"
    @update:model-value="onVisibleChange"
  >
    <ElAlert
      v-if="serverError"
      class="form-error"
      data-testid="employee-form-error"
      :title="serverError"
      type="error"
      show-icon
      :closable="false"
    />

    <ElForm ref="formRef" :model="form" :rules="rules" label-width="88px">
      <ElRow :gutter="16">
        <ElCol :span="12">
          <ElFormItem label="工号" prop="employeeNo">
            <ElInput v-model="form.employeeNo" data-testid="employee-no" maxlength="32" placeholder="请输入工号" />
          </ElFormItem>
        </ElCol>
        <ElCol :span="12">
          <ElFormItem label="姓名" prop="name">
            <ElInput v-model="form.name" data-testid="employee-name" maxlength="64" placeholder="请输入姓名" />
          </ElFormItem>
        </ElCol>
        <ElCol :span="12">
          <ElFormItem label="性别" prop="gender">
            <ElSelect v-model="form.gender" placeholder="请选择性别" clearable>
              <ElOption v-for="item in GENDER_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
            </ElSelect>
          </ElFormItem>
        </ElCol>
        <ElCol :span="12">
          <ElFormItem label="状态" prop="status">
            <ElSelect v-model="form.status">
              <ElOption v-for="item in STATUS_OPTIONS" :key="item.value" :label="item.label" :value="item.value" />
            </ElSelect>
          </ElFormItem>
        </ElCol>
        <ElCol :span="12">
          <ElFormItem label="部门" prop="department">
            <ElInput v-model="form.department" maxlength="64" placeholder="请输入部门" />
          </ElFormItem>
        </ElCol>
        <ElCol :span="12">
          <ElFormItem label="职位" prop="position">
            <ElInput v-model="form.position" maxlength="64" placeholder="请输入职位" />
          </ElFormItem>
        </ElCol>
        <ElCol :span="12">
          <ElFormItem label="手机号" prop="phone">
            <ElInput v-model="form.phone" maxlength="32" placeholder="请输入手机号" />
          </ElFormItem>
        </ElCol>
        <ElCol :span="12">
          <ElFormItem label="邮箱" prop="email">
            <ElInput v-model="form.email" maxlength="128" placeholder="请输入邮箱" />
          </ElFormItem>
        </ElCol>
        <ElCol :span="12">
          <ElFormItem label="入职日期" prop="hireDate">
            <ElDatePicker
              v-model="form.hireDate"
              type="date"
              value-format="YYYY-MM-DD"
              placeholder="请选择入职日期"
              style="width: 100%"
            />
          </ElFormItem>
        </ElCol>
      </ElRow>
    </ElForm>

    <template #footer>
      <ElButton :disabled="submitting" data-testid="employee-cancel" @click="close">取消</ElButton>
      <ElButton type="primary" :loading="submitting" data-testid="employee-submit" @click="submit">保存</ElButton>
    </template>
  </ElDialog>
</template>
